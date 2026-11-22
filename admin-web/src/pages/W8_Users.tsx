// FILE: admin-web/src/pages/W8_Users.tsx — Ngôn ngữ & tài khoản (Figma W8)
import { useEffect, useMemo, useState, type CSSProperties, type ReactNode } from "react";
import { Ellipsis, Languages as LanguagesIcon, Search, ShieldCheck, SquarePen, User } from "lucide-react";
import { apiFetch, getSavedUser } from "../api";
import type { Language } from "../types";
import { Banner, BRAND_RED, Pager } from "../ui";

interface UserRecord {
  user_id: number;
  full_name: string;
  email: string;
  role: string;
  preferred_lang: string;
  is_active: boolean;
}

interface UserForm {
  full_name: string;
  email: string;
  password: string;
  role: string;
  preferred_lang: string;
  is_active: boolean;
}

const PAGE_SIZE = 6;
const ROLE_LABELS: Record<string, string> = { ADMIN: "Admin", CONTENT_MANAGER: "Quản lý nội dung", VISITOR: "Khách tham quan" };
const ROLE_BADGE: Record<string, { color: string; bg: string }> = {
  ADMIN: { color: "#9b1c1c", bg: "#fdecea" },
  CONTENT_MANAGER: { color: "#b45309", bg: "#fff4e0" },
  VISITOR: { color: "#475569", bg: "#f1f5f9" },
};
const EMPTY_FORM: UserForm = { full_name: "", email: "", password: "", role: "CONTENT_MANAGER", preferred_lang: "vi", is_active: true };

export default function W8_Users() {
  const me = getSavedUser();
  const isAdmin = me?.role === "ADMIN";

  const [languages, setLanguages] = useState<Language[]>([]);
  const [users, setUsers] = useState<UserRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<{ tone: "error" | "ok"; text: string } | null>(null);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [menuFor, setMenuFor] = useState<number | null>(null);
  const [modal, setModal] = useState<{ mode: "create" } | { mode: "edit"; user: UserRecord } | null>(null);
  const [form, setForm] = useState<UserForm>(EMPTY_FORM);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    const requests: Array<Promise<unknown>> = [
      apiFetch<Language[]>("/api/languages").then((l) => active && setLanguages(l)),
    ];
    if (isAdmin) requests.push(apiFetch<UserRecord[]>("/api/users").then((u) => active && setUsers(u)));
    Promise.all(requests)
      .catch((e: Error) => active && setNotice({ tone: "error", text: e.message }))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [isAdmin, reloadKey]);

  const reload = () => setReloadKey((n) => n + 1);

  const filtered = useMemo(() => {
    const q = search.trim().toLocaleLowerCase("vi");
    return users.filter(
      (u) => (roleFilter === "ALL" || u.role === roleFilter) && `${u.full_name} ${u.email}`.toLocaleLowerCase("vi").includes(q),
    );
  }, [users, search, roleFilter]);
  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pageUsers = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const enabledCount = languages.filter((l) => l.is_enabled).length;
  const enabledLangs = languages.filter((l) => l.is_enabled);

  // ---- hành động ----
  const run = async (action: () => Promise<unknown>, okText: string) => {
    setBusy(true);
    setNotice(null);
    try {
      await action();
      setNotice({ tone: "ok", text: okText });
      reload();
      return true;
    } catch (e) {
      setNotice({ tone: "error", text: e instanceof Error ? e.message : "Thao tác thất bại." });
      return false;
    } finally {
      setBusy(false);
    }
  };

  const toggleLanguage = (lang: Language) =>
    run(
      () => apiFetch(`/api/languages/${lang.lang_code}`, { method: "PUT", body: JSON.stringify({ is_enabled: !lang.is_enabled }) }),
      `Đã ${lang.is_enabled ? "tắt" : "bật"} ${lang.name_native}.`,
    );

  const toggleActive = (u: UserRecord) => {
    setMenuFor(null);
    const next = !u.is_active;
    if (!window.confirm(next ? `Mở khóa tài khoản ${u.full_name}?` : `Khóa tài khoản ${u.full_name}? Người này sẽ không đăng nhập được.`)) return;
    void run(() => apiFetch(`/api/users/${u.user_id}`, { method: "PUT", body: JSON.stringify({ is_active: next }) }), next ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
  };

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setModal({ mode: "create" });
  };
  const openEdit = (u: UserRecord) => {
    setMenuFor(null);
    setForm({ full_name: u.full_name, email: u.email, password: "", role: u.role, preferred_lang: u.preferred_lang, is_active: u.is_active });
    setModal({ mode: "edit", user: u });
  };

  const submit = async () => {
    if (!modal) return;
    const name = form.full_name.trim();
    if (!name) return setNotice({ tone: "error", text: "Họ tên không được để trống." });
    if (modal.mode === "create") {
      if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) return setNotice({ tone: "error", text: "Email không hợp lệ." });
      if (form.password.length < 6) return setNotice({ tone: "error", text: "Mật khẩu cần ít nhất 6 ký tự." });
      const ok = await run(
        () => apiFetch("/api/users", { method: "POST", body: JSON.stringify({ ...form, full_name: name, email: form.email.trim() }) }),
        "Đã tạo tài khoản.",
      );
      if (ok) setModal(null);
      return;
    }
    if (form.password && form.password.length < 6) return setNotice({ tone: "error", text: "Mật khẩu mới cần ít nhất 6 ký tự." });
    const isSelf = modal.user.user_id === me?.user_id;
    const payload: Record<string, unknown> = { full_name: name, preferred_lang: form.preferred_lang };
    if (!isSelf) {
      payload.role = form.role;
      payload.is_active = form.is_active;
    }
    if (form.password) payload.password = form.password;
    const ok = await run(() => apiFetch(`/api/users/${modal.user.user_id}`, { method: "PUT", body: JSON.stringify(payload) }), "Đã cập nhật tài khoản.");
    if (ok) setModal(null);
  };

  const editingSelf = modal?.mode === "edit" && modal.user.user_id === me?.user_id;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <h2 style={{ fontSize: 24, fontWeight: 800, color: "#0f172a" }}>Ngôn ngữ & tài khoản</h2>
          <p style={{ fontSize: 13, color: "#475569", marginTop: 2 }}>Quản lý ngôn ngữ trải nghiệm và phân quyền truy cập hệ thống.</p>
        </div>
        {isAdmin ? <button type="button" style={primaryBtnStyle} onClick={openCreate}>+ Thêm tài khoản</button> : null}
      </div>

      {notice ? <Banner tone={notice.tone} text={notice.text} onClose={() => setNotice(null)} /> : null}

      <div style={{ display: "grid", gridTemplateColumns: "0.82fr 1.6fr", gap: 18, alignItems: "start" }}>
        {/* ---------- NGÔN NGỮ HỆ THỐNG ---------- */}
        <div style={cardStyle}>
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <div>
              <h3 style={cardTitleStyle}>Ngôn ngữ hệ thống</h3>
              <p style={cardSubStyle}>{enabledCount} đang bật · {languages.length} ngôn ngữ hỗ trợ</p>
            </div>
            <LanguagesIcon size={20} color={BRAND_RED} />
          </div>
          <div style={{ marginTop: 10 }}>
            {loading ? <p style={mutedStyle}>Đang tải...</p> : null}
            {languages.map((l, i) => (
              <div key={l.lang_code} style={{ padding: "14px 0", borderTop: i ? "1px solid #e2e8f0" : "none" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <span style={{ ...langCodeStyle, color: l.is_enabled ? BRAND_RED : "#64748b", backgroundColor: l.is_enabled ? "#fdecea" : "#f1f5f9" }}>{l.lang_code.toUpperCase()}</span>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 700, color: "#0f172a", fontSize: 14 }}>{l.name_native}</div>
                    <div style={{ fontSize: 12, color: "#64748b" }}>{l.is_default ? "Ngôn ngữ mặc định" : l.name_vi}</div>
                  </div>
                  <Switch
                    checked={l.is_enabled}
                    disabled={!isAdmin || l.is_default || busy}
                    label={`${l.is_enabled ? "Tắt" : "Bật"} ${l.name_native}`}
                    onChange={() => void toggleLanguage(l)}
                  />
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", marginTop: 6, fontSize: 12, color: "#64748b" }}>
                  <span>{l.is_enabled ? "Đang bật" : "Đang tắt"}</span>
                  <span>{l.translated_count} / {l.total_artifacts} hiện vật đã dịch</span>
                </div>
              </div>
            ))}
          </div>
          <p style={{ ...mutedStyle, marginTop: 8 }}>Tắt ngôn ngữ sẽ ẩn lựa chọn trên ứng dụng khách tham quan. Nội dung đã dịch vẫn được giữ lại.</p>
        </div>

        {/* ---------- TÀI KHOẢN NGƯỜI DÙNG ---------- */}
        <div style={cardStyle}>
          <h3 style={cardTitleStyle}>Tài khoản người dùng</h3>
          {isAdmin ? (
            <>
              <p style={cardSubStyle}>{users.length} tài khoản · Phân quyền theo vai trò</p>
              <div style={{ display: "flex", gap: 12, margin: "14px 0" }}>
                <label style={searchBoxStyle}>
                  <Search size={16} color="#64748b" />
                  <input
                    value={search}
                    onChange={(e) => { setSearch(e.target.value); setPage(1); }}
                    placeholder="Tìm họ tên hoặc email..."
                    style={{ border: "none", outline: "none", flex: 1, fontSize: 13.5, background: "transparent" }}
                  />
                </label>
                <select value={roleFilter} onChange={(e) => { setRoleFilter(e.target.value); setPage(1); }} style={selectStyle} aria-label="Lọc vai trò">
                  <option value="ALL">Tất cả vai trò</option>
                  {Object.entries(ROLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select>
              </div>

              <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13.5 }}>
                <thead>
                  <tr style={{ backgroundColor: "#f1f5f9" }}>
                    {["Người dùng / Email", "Vai trò", "Trạng thái", "Thao tác"].map((h) => <th key={h} style={thStyle}>{h}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {pageUsers.length === 0 ? (
                    <tr><td colSpan={4} style={{ padding: 24, textAlign: "center", color: "#64748b" }}>{loading ? "Đang tải..." : "Không có tài khoản phù hợp."}</td></tr>
                  ) : pageUsers.map((u) => {
                    const badge = ROLE_BADGE[u.role] ?? ROLE_BADGE.VISITOR;
                    const self = u.user_id === me?.user_id;
                    return (
                      <tr key={u.user_id} style={{ borderBottom: "1px solid #e2e8f0" }}>
                        <td style={tdStyle}>
                          <div style={{ fontWeight: 600, color: "#0f172a" }}>{u.full_name}{self ? " (Bạn)" : ""}</div>
                          <div style={{ fontSize: 12, color: "#64748b" }}>{u.email}</div>
                        </td>
                        <td style={tdStyle}><span style={{ ...roleBadgeStyle, color: badge.color, backgroundColor: badge.bg }}>{ROLE_LABELS[u.role] ?? u.role}</span></td>
                        <td style={{ ...tdStyle, fontSize: 12.5, color: u.is_active ? "#15803d" : "#64748b" }}>{u.is_active ? "Đang hoạt động" : "Tạm khóa"}</td>
                        <td style={{ ...tdStyle, position: "relative", whiteSpace: "nowrap" }}>
                          <button type="button" style={linkBtnStyle} onClick={() => openEdit(u)}>Sửa</button>
                          <button type="button" style={moreBtnStyle} aria-label="Thêm thao tác" disabled={self} onClick={() => setMenuFor(menuFor === u.user_id ? null : u.user_id)}>
                            <Ellipsis size={16} />
                          </button>
                          {menuFor === u.user_id ? (
                            <>
                              <div style={{ position: "fixed", inset: 0, zIndex: 10 }} onClick={() => setMenuFor(null)} />
                              <div style={menuStyle}>
                                <button type="button" style={menuItemStyle} onClick={() => toggleActive(u)}>{u.is_active ? "Khóa tài khoản" : "Mở khóa tài khoản"}</button>
                              </div>
                            </>
                          ) : null}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 14 }}>
                <span style={{ fontSize: 13, color: "#475569" }}>
                  {filtered.length === 0 ? "Không có kết quả" : `Hiển thị ${(currentPage - 1) * PAGE_SIZE + 1}–${Math.min(currentPage * PAGE_SIZE, filtered.length)} trên ${filtered.length} kết quả`}
                </span>
                <Pager page={currentPage} pageCount={pageCount} onChange={setPage} />
              </div>
            </>
          ) : (
            <p style={{ ...mutedStyle, marginTop: 14 }}>Chỉ Quản trị viên (Admin) mới xem và quản lý được danh sách tài khoản.</p>
          )}
        </div>
      </div>

      {/* ---------- PHẠM VI PHÂN QUYỀN ---------- */}
      <div style={cardStyle}>
        <h3 style={cardTitleStyle}>Phạm vi phân quyền</h3>
        <p style={cardSubStyle}>Mỗi tài khoản có một vai trò. Chỉ Admin được thay đổi quyền truy cập.</p>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 18, marginTop: 14 }}>
          <Scope icon={<ShieldCheck size={18} color={BRAND_RED} />} bg="#fdecea" title="Admin" text="Toàn quyền quản trị hệ thống, thiết bị, nội dung và tài khoản." />
          <Scope icon={<SquarePen size={18} color="#b45309" />} bg="#fff4e0" title="Quản lý nội dung" text="Biên tập hiện vật, bản dịch, media và kiểm duyệt phản hồi." />
          <Scope icon={<User size={18} color="#475569" />} bg="#f1f5f9" title="Khách tham quan" text="Khám phá hiện vật, nghe thuyết minh và gửi đánh giá." />
        </div>
      </div>

      {modal ? (
        <div style={overlayStyle} role="dialog" aria-modal="true" onClick={() => !busy && setModal(null)}>
          <div style={modalStyle} onClick={(e) => e.stopPropagation()}>
            <h3 style={{ fontSize: 18, fontWeight: 700, color: "#0f172a" }}>{modal.mode === "create" ? "Thêm tài khoản" : "Sửa tài khoản"}</h3>
            <Field label="Họ tên *"><input value={form.full_name} onChange={(e) => setForm({ ...form, full_name: e.target.value })} style={inputStyle} /></Field>
            <Field label="Email *">
              <input value={form.email} disabled={modal.mode === "edit"} onChange={(e) => setForm({ ...form, email: e.target.value })} style={{ ...inputStyle, backgroundColor: modal.mode === "edit" ? "#f8fafc" : "#fff" }} />
            </Field>
            <Field label={modal.mode === "create" ? "Mật khẩu * (ít nhất 6 ký tự)" : "Đặt lại mật khẩu (để trống nếu không đổi)"}>
              <input type="password" autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} style={inputStyle} />
            </Field>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
              <Field label="Vai trò">
                <select value={form.role} disabled={editingSelf} onChange={(e) => setForm({ ...form, role: e.target.value })} style={inputStyle}>
                  {Object.entries(ROLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select>
              </Field>
              <Field label="Ngôn ngữ ưu tiên">
                <select value={form.preferred_lang} onChange={(e) => setForm({ ...form, preferred_lang: e.target.value })} style={inputStyle}>
                  {(enabledLangs.some((l) => l.lang_code === form.preferred_lang) ? enabledLangs : [...enabledLangs, ...languages.filter((l) => l.lang_code === form.preferred_lang)]).map((l) => (
                    <option key={l.lang_code} value={l.lang_code}>{l.name_native}</option>
                  ))}
                </select>
              </Field>
            </div>
            <label style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 14, fontSize: 13.5, color: "#0f172a" }}>
              <input type="checkbox" checked={form.is_active} disabled={editingSelf} onChange={(e) => setForm({ ...form, is_active: e.target.checked })} style={{ accentColor: BRAND_RED }} />
              Tài khoản đang hoạt động
            </label>
            {editingSelf ? <p style={{ ...mutedStyle, marginTop: 8 }}>Bạn không thể tự khóa hoặc hạ quyền tài khoản của chính mình.</p> : null}
            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 20 }}>
              <button type="button" style={secondaryBtnStyle} disabled={busy} onClick={() => setModal(null)}>Hủy</button>
              <button type="button" style={primaryBtnStyle} disabled={busy} onClick={() => void submit()}>{busy ? "Đang lưu..." : "Lưu"}</button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function Switch({ checked, disabled, label, onChange }: { checked: boolean; disabled: boolean; label: string; onChange: () => void }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={onChange}
      style={{ width: 44, height: 24, borderRadius: 999, border: "none", padding: 2, backgroundColor: checked ? BRAND_RED : "#cbd5e1", opacity: disabled ? 0.6 : 1, cursor: disabled ? "not-allowed" : "pointer", display: "flex", justifyContent: checked ? "flex-end" : "flex-start", transition: "background-color 0.15s" }}
    >
      <span style={{ width: 20, height: 20, borderRadius: "50%", backgroundColor: "#fff" }} />
    </button>
  );
}

function Scope({ icon, bg, title, text }: { icon: ReactNode; bg: string; title: string; text: string }) {
  return (
    <div style={{ display: "flex", gap: 12 }}>
      <span style={{ width: 36, height: 36, borderRadius: 8, backgroundColor: bg, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>{icon}</span>
      <div>
        <div style={{ fontWeight: 600, color: "#0f172a", fontSize: 14 }}>{title}</div>
        <div style={{ fontSize: 12.5, color: "#475569", marginTop: 2, lineHeight: 1.45 }}>{text}</div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label style={{ display: "block", marginTop: 14 }}>
      <span style={{ fontSize: 13, fontWeight: 600, color: "#1e293b" }}>{label}</span>
      <div style={{ marginTop: 6 }}>{children}</div>
    </label>
  );
}

const cardStyle: CSSProperties = { backgroundColor: "#fff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "18px 20px" };
const cardTitleStyle: CSSProperties = { fontSize: 16, fontWeight: 600, color: "#0f172a" };
const cardSubStyle: CSSProperties = { fontSize: 12.5, color: "#475569", marginTop: 2 };
const mutedStyle: CSSProperties = { fontSize: 12, color: "#64748b", lineHeight: 1.5 };
const langCodeStyle: CSSProperties = { width: 36, height: 36, borderRadius: 8, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12.5, fontWeight: 700, flexShrink: 0 };
const searchBoxStyle: CSSProperties = { flex: 1, display: "flex", alignItems: "center", gap: 8, height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8 };
const selectStyle: CSSProperties = { height: 40, padding: "0 10px", border: "none", fontSize: 13.5, color: "#475569", backgroundColor: "transparent", cursor: "pointer" };
const thStyle: CSSProperties = { textAlign: "left", padding: "12px", fontSize: 12.5, fontWeight: 600, color: "#334155" };
const tdStyle: CSSProperties = { padding: "10px 12px", verticalAlign: "middle" };
const roleBadgeStyle: CSSProperties = { fontSize: 12, fontWeight: 600, padding: "4px 10px", borderRadius: 5 };
const linkBtnStyle: CSSProperties = { background: "none", border: "none", color: BRAND_RED, fontWeight: 700, cursor: "pointer", fontSize: 13, marginRight: 8 };
const moreBtnStyle: CSSProperties = { background: "none", border: "none", color: "#475569", cursor: "pointer", verticalAlign: "middle" };
const menuStyle: CSSProperties = { position: "absolute", right: 0, top: "100%", zIndex: 11, minWidth: 170, backgroundColor: "#fff", border: "1px solid #cbd5e1", borderRadius: 8, boxShadow: "0 8px 20px rgba(15,23,42,0.12)", padding: 4 };
const menuItemStyle: CSSProperties = { display: "block", width: "100%", textAlign: "left", padding: "8px 12px", background: "none", border: "none", fontSize: 13, color: "#0f172a", cursor: "pointer", borderRadius: 6 };
const primaryBtnStyle: CSSProperties = { height: 40, padding: "0 18px", border: "none", borderRadius: 8, backgroundColor: BRAND_RED, color: "#fff", fontSize: 13.5, fontWeight: 700, cursor: "pointer" };
const secondaryBtnStyle: CSSProperties = { height: 40, padding: "0 16px", border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", fontSize: 13.5, fontWeight: 600, color: "#0f172a", cursor: "pointer" };
const overlayStyle: CSSProperties = { position: "fixed", inset: 0, zIndex: 50, backgroundColor: "rgba(15,23,42,0.45)", display: "flex", alignItems: "center", justifyContent: "center" };
const modalStyle: CSSProperties = { width: 460, maxWidth: "92vw", maxHeight: "90vh", overflowY: "auto", backgroundColor: "#fff", borderRadius: 12, padding: "22px 24px" };
const inputStyle: CSSProperties = { width: "100%", height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 13.5, color: "#0f172a", backgroundColor: "#fff" };
