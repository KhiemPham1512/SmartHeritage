// FILE: admin-web/src/pages/W8_Users.tsx
import { useCallback, useEffect, useMemo, useState } from "react";
import type { CSSProperties } from "react";
import { apiFetch } from "../api";

interface UserRecord {
  user_id: number;
  full_name: string;
  email: string;
  role: string;
  preferred_lang: string;
  is_active: boolean;
}

interface UserFormState {
  full_name: string;
  email: string;
  password: string;
  role: string;
  preferred_lang: string;
  is_active: boolean;
}

const ROLE_LABELS: Record<string, string> = {
  ADMIN: "Quản trị viên",
  CONTENT_MANAGER: "Quản lý nội dung",
  VISITOR: "Khách tham quan",
};

const LANGUAGE_LABELS: Record<string, string> = {
  vi: "Tiếng Việt",
  en: "English",
  ko: "한국어",
  ja: "日本語",
  zh: "中文",
};

const DEFAULT_FORM: UserFormState = {
  full_name: "",
  email: "",
  password: "",
  role: "CONTENT_MANAGER",
  preferred_lang: "vi",
  is_active: true,
};

const C = {
  red: "#80140E",
  redLight: "#FDF0EF",
  background: "#F1EDE4",
  white: "#FFFFFF",
  text: "#0F172A",
  muted: "#475569",
  border: "#D4CDC3",
  green: "#15803D",
  greenLight: "#EAF7EE",
  orange: "#E4A93B",
  gray: "#64748B",
};

export default function W8_Users() {
  const [users, setUsers] = useState<UserRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [createForm, setCreateForm] = useState<UserFormState>(DEFAULT_FORM);
  const [editingUser, setEditingUser] = useState<UserRecord | null>(null);
  const [editForm, setEditForm] = useState<{ role: string; preferred_lang: string; is_active: boolean }>({
    role: "CONTENT_MANAGER",
    preferred_lang: "vi",
    is_active: true,
  });
  const [submitting, setSubmitting] = useState(false);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await apiFetch<UserRecord[]>("/api/users");
      setUsers(Array.isArray(response) ? response : []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể tải danh sách tài khoản.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(loadUsers);
  }, [loadUsers]);

  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      const matchesSearch = !searchTerm.trim()
        || `${user.full_name} ${user.email}`.toLowerCase().includes(searchTerm.trim().toLowerCase());
      const matchesRole = roleFilter === "all" || user.role === roleFilter;
      const matchesStatus = statusFilter === "all" || String(user.is_active) === statusFilter;
      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [users, searchTerm, roleFilter, statusFilter]);

  const stats = useMemo(() => {
    return {
      total: users.length,
      active: users.filter((user) => user.is_active).length,
      locked: users.filter((user) => !user.is_active).length,
      admin: users.filter((user) => user.role === "ADMIN").length,
    };
  }, [users]);

  const resetCreateForm = () => {
    setCreateForm(DEFAULT_FORM);
  };

  const validateCreateForm = () => {
    const fullName = createForm.full_name.trim();
    const email = createForm.email.trim();
    const password = createForm.password;

    if (!fullName) {
      throw new Error("Họ tên không được bỏ trống.");
    }

    if (!email || !/^\S+@\S+\.\S+$/.test(email)) {
      throw new Error("Email không hợp lệ.");
    }

    if (!password || password.trim().length < 6) {
      throw new Error("Mật khẩu phải có ít nhất 6 ký tự.");
    }
  };

  const handleCreateUser = async () => {
    try {
      validateCreateForm();
      setSubmitting(true);
      setError("");

      await apiFetch("/api/users", {
        method: "POST",
        body: JSON.stringify({
          full_name: createForm.full_name.trim(),
          email: createForm.email.trim(),
          password: createForm.password,
          role: createForm.role,
          preferred_lang: createForm.preferred_lang,
          is_active: createForm.is_active,
        }),
      });

      setShowCreateModal(false);
      resetCreateForm();
      await loadUsers();
      setNotice({ type: "success", text: "Tạo tài khoản thành công." });
    } catch (err) {
      const message = err instanceof Error ? err.message : "Không thể tạo tài khoản.";
      setNotice({ type: "error", text: message });
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditOpen = (user: UserRecord) => {
    setEditingUser(user);
    setEditForm({
      role: user.role,
      preferred_lang: user.preferred_lang,
      is_active: user.is_active,
    });
    setShowEditModal(true);
  };

  const handleUpdateUser = async () => {
    if (!editingUser) return;

    const payload: Record<string, unknown> = {};
    if (editForm.role !== editingUser.role) payload.role = editForm.role;
    if (editForm.preferred_lang !== editingUser.preferred_lang) payload.preferred_lang = editForm.preferred_lang;
    if (editForm.is_active !== editingUser.is_active) payload.is_active = editForm.is_active;

    if (Object.keys(payload).length === 0) {
      setShowEditModal(false);
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      await apiFetch(`/api/users/${editingUser.user_id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      setShowEditModal(false);
      setEditingUser(null);
      await loadUsers();
      setNotice({ type: "success", text: "Cập nhật tài khoản thành công." });
    } catch (err) {
      const message = err instanceof Error ? err.message : "Không thể cập nhật tài khoản.";
      setNotice({ type: "error", text: message });
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusToggle = async (user: UserRecord) => {
    const nextState = !user.is_active;
    const confirmed = window.confirm(
      nextState ? `Bạn có muốn mở khóa tài khoản ${user.full_name}?` : `Bạn có muốn khóa tài khoản ${user.full_name}?`
    );

    if (!confirmed) return;

    setSubmitting(true);
    setError("");

    try {
      await apiFetch(`/api/users/${user.user_id}`, {
        method: "PUT",
        body: JSON.stringify({ is_active: nextState }),
      });
      await loadUsers();
      setNotice({ type: "success", text: nextState ? "Tài khoản đã được mở khóa." : "Tài khoản đã được khóa." });
    } catch (err) {
      const message = err instanceof Error ? err.message : "Không thể cập nhật trạng thái tài khoản.";
      setNotice({ type: "error", text: message });
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const cardStyle: CSSProperties = {
    background: C.white,
    border: `1.5px solid ${C.border}`,
    borderRadius: 11,
    padding: "16px 20px",
  };

  const buttonStyle: CSSProperties = {
    border: `1.5px solid ${C.border}`,
    borderRadius: 8,
    background: C.white,
    padding: "8px 12px",
    fontSize: 12,
    fontWeight: 700,
    cursor: "pointer",
    color: C.text,
  };

  const selectStyle: CSSProperties = {
    height: 38,
    border: `1.5px solid ${C.border}`,
    borderRadius: 9,
    background: C.white,
    color: C.text,
    padding: "0 12px",
    fontSize: 12,
    outline: "none",
    minWidth: 140,
  };

  return (
    <div style={{ width: "100%", minHeight: "100%", background: C.background, padding: 20, boxSizing: "border-box", fontFamily: "Arial, sans-serif", color: C.text }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, flexWrap: "wrap", marginBottom: 18 }}>
        <div>
          <div style={{ fontSize: 19, fontWeight: 800 }}>Quản lý tài khoản</div>
          <div style={{ color: C.muted, fontSize: 12, marginTop: 4 }}>Theo dõi và điều phối quyền truy cập theo vai trò của hệ thống.</div>
        </div>

        <button type="button" onClick={() => setShowCreateModal(true)} style={{ ...buttonStyle, background: C.red, borderColor: C.red, color: C.white }}>
          + Thêm tài khoản
        </button>
      </div>

      {error && (
        <div role="alert" style={{ background: C.redLight, color: C.red, border: "1px solid #F2C7C4", padding: 12, borderRadius: 8, marginBottom: 15, fontSize: 12, fontWeight: 700 }}>
          ⚠ {error}
        </div>
      )}

      {notice && (
        <div style={{ background: notice.type === "success" ? "#eaf7ee" : "#fff7ed", border: `1px solid ${notice.type === "success" ? "#b5d9be" : "#f6d3a6"}`, color: notice.type === "success" ? C.green : C.orange, padding: 12, borderRadius: 8, marginBottom: 15, fontSize: 12, fontWeight: 700 }}>
          {notice.text}
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, minmax(0, 1fr))", gap: 12, marginBottom: 18 }}>
        {[
          { label: "Tổng tài khoản", value: stats.total, color: C.text },
          { label: "Đang hoạt động", value: stats.active, color: C.green },
          { label: "Đã khóa", value: stats.locked, color: C.gray },
          { label: "Quản trị viên", value: stats.admin, color: C.red },
        ].map((stat) => (
          <div key={stat.label} style={cardStyle}>
            <div style={{ fontSize: 11, color: C.muted, marginBottom: 6 }}>{stat.label}</div>
            <div style={{ fontSize: 26, fontWeight: 800, color: stat.color }}>{stat.value}</div>
          </div>
        ))}
      </div>

      <div style={{ ...cardStyle, padding: 12, marginBottom: 12 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
          <input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="⌕ Tìm theo họ tên hoặc email..."
            style={{ flex: "1 1 260px", height: 38, border: `1.5px solid ${C.border}`, borderRadius: 9, padding: "0 12px", fontSize: 12, outline: "none", minWidth: 220 }}
          />

          <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)} style={selectStyle}>
            <option value="all">Tất cả vai trò</option>
            {Object.entries(ROLE_LABELS).map(([role, label]) => (
              <option key={role} value={role}>{label}</option>
            ))}
          </select>

          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} style={selectStyle}>
            <option value="all">Tất cả trạng thái</option>
            <option value="true">Đang hoạt động</option>
            <option value="false">Đã khóa</option>
          </select>

          <button type="button" onClick={() => void loadUsers()} style={{ ...buttonStyle, minWidth: 120 }} disabled={loading}>
            {loading ? "Đang tải..." : "Làm mới"}
          </button>
        </div>
      </div>

      <div style={{ ...cardStyle, padding: 0, overflow: "hidden" }}>
        <div style={{ padding: "14px 15px", borderBottom: `1px solid ${C.border}`, fontSize: 13, fontWeight: 800 }}>
          Danh sách tài khoản
          <span style={{ color: C.muted, fontWeight: 600, marginLeft: 7 }}>({filteredUsers.length} người dùng)</span>
        </div>

        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", minWidth: 900, borderCollapse: "collapse", fontSize: 11 }}>
            <thead>
              <tr style={{ background: "#F4F6F9" }}>
                {['ID', 'Họ tên', 'Email', 'Vai trò', 'Ngôn ngữ ưu tiên', 'Trạng thái', 'Thao tác'].map((heading) => (
                  <th key={heading} style={{ padding: "11px 9px", textAlign: "left", color: C.gray, fontWeight: 700, borderBottom: `1px solid ${C.border}` }}>
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ padding: 35, textAlign: "center", color: C.muted }}>Đang tải danh sách tài khoản...</td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ padding: 35, textAlign: "center", color: C.muted }}>Không có tài khoản phù hợp.</td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.user_id} style={{ borderBottom: `1px solid ${C.border}` }}>
                    <td style={{ padding: 11, fontWeight: 700 }}>{user.user_id}</td>
                    <td style={{ padding: 11, fontWeight: 700 }}>{user.full_name}</td>
                    <td style={{ padding: 11, color: C.muted }}>{user.email}</td>
                    <td style={{ padding: 11 }}>
                      <span style={{ display: "inline-block", padding: "5px 8px", borderRadius: 6, background: user.role === "ADMIN" ? "#FDE8E6" : user.role === "CONTENT_MANAGER" ? "#E0F2FE" : "#F1F5F9", color: user.role === "ADMIN" ? C.red : user.role === "CONTENT_MANAGER" ? "#0F766E" : C.gray, fontWeight: 700 }}>
                        {ROLE_LABELS[user.role] || user.role}
                      </span>
                    </td>
                    <td style={{ padding: 11 }}>{LANGUAGE_LABELS[user.preferred_lang] || user.preferred_lang}</td>
                    <td style={{ padding: 11 }}>
                      <span style={{ display: "inline-block", padding: "5px 8px", borderRadius: 6, background: user.is_active ? C.greenLight : "#F1F5F9", color: user.is_active ? C.green : C.gray, fontWeight: 700 }}>
                        {user.is_active ? "Đang hoạt động" : "Đã khóa"}
                      </span>
                    </td>
                    <td style={{ padding: 11 }}>
                      <div style={{ display: "flex", gap: 5, flexWrap: "wrap" }}>
                        <button type="button" onClick={() => handleEditOpen(user)} style={{ ...buttonStyle, color: C.text }}>Sửa</button>
                        <button type="button" onClick={() => void handleStatusToggle(user)} style={{ ...buttonStyle, color: user.is_active ? C.gray : C.green }} disabled={submitting}>
                          {user.is_active ? "Khóa" : "Mở khóa"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showCreateModal && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(15, 23, 42, 0.35)", display: "flex", alignItems: "center", justifyContent: "center", padding: 20 }}>
          <div style={{ width: "100%", maxWidth: 620, background: C.white, borderRadius: 16, border: `1.5px solid ${C.border}`, padding: 24, boxShadow: "0 16px 40px rgba(15, 23, 42, 0.12)" }}>
            <div style={{ fontSize: 22, fontWeight: 800, marginBottom: 18 }}>Thêm tài khoản</div>

            <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 14 }}>
              <label style={{ display: "grid", gap: 6, fontSize: 12, fontWeight: 700 }}>
                Họ tên
                <input value={createForm.full_name} onChange={(e) => setCreateForm((prev) => ({ ...prev, full_name: e.target.value }))} style={{ height: 38, border: `1.5px solid ${C.border}`, borderRadius: 8, padding: "0 12px", fontSize: 12, outline: "none" }} />
              </label>

              <label style={{ display: "grid", gap: 6, fontSize: 12, fontWeight: 700 }}>
                Email
                <input value={createForm.email} onChange={(e) => setCreateForm((prev) => ({ ...prev, email: e.target.value }))} style={{ height: 38, border: `1.5px solid ${C.border}`, borderRadius: 8, padding: "0 12px", fontSize: 12, outline: "none" }} />
              </label>

              <label style={{ display: "grid", gap: 6, fontSize: 12, fontWeight: 700 }}>
                Mật khẩu
                <input type="password" value={createForm.password} onChange={(e) => setCreateForm((prev) => ({ ...prev, password: e.target.value }))} style={{ height: 38, border: `1.5px solid ${C.border}`, borderRadius: 8, padding: "0 12px", fontSize: 12, outline: "none" }} />
              </label>

              <label style={{ display: "grid", gap: 6, fontSize: 12, fontWeight: 700 }}>
                Vai trò
                <select value={createForm.role} onChange={(e) => setCreateForm((prev) => ({ ...prev, role: e.target.value }))} style={selectStyle}>
                  {Object.entries(ROLE_LABELS).map(([role, label]) => (
                    <option key={role} value={role}>{label}</option>
                  ))}
                </select>
              </label>

              <label style={{ display: "grid", gap: 6, fontSize: 12, fontWeight: 700 }}>
                Ngôn ngữ ưu tiên
                <select value={createForm.preferred_lang} onChange={(e) => setCreateForm((prev) => ({ ...prev, preferred_lang: e.target.value }))} style={selectStyle}>
                  {Object.entries(LANGUAGE_LABELS).map(([lang, label]) => (
                    <option key={lang} value={lang}>{label}</option>
                  ))}
                </select>
              </label>

              <label style={{ display: "grid", gap: 6, fontSize: 12, fontWeight: 700 }}>
                Trạng thái
                <select value={String(createForm.is_active)} onChange={(e) => setCreateForm((prev) => ({ ...prev, is_active: e.target.value === "true" }))} style={selectStyle}>
                  <option value="true">Đang hoạt động</option>
                  <option value="false">Đã khóa</option>
                </select>
              </label>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 22 }}>
              <button type="button" onClick={() => { setShowCreateModal(false); resetCreateForm(); }} style={{ ...buttonStyle, borderColor: C.border, color: C.text }}>
                Hủy
              </button>
              <button type="button" onClick={() => void handleCreateUser()} style={{ ...buttonStyle, background: C.red, borderColor: C.red, color: C.white }} disabled={submitting}>
                {submitting ? "Đang tạo..." : "Tạo tài khoản"}
              </button>
            </div>
          </div>
        </div>
      )}

      {showEditModal && editingUser && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(15, 23, 42, 0.35)", display: "flex", alignItems: "center", justifyContent: "center", padding: 20 }}>
          <div style={{ width: "100%", maxWidth: 520, background: C.white, borderRadius: 16, border: `1.5px solid ${C.border}`, padding: 24, boxShadow: "0 16px 40px rgba(15, 23, 42, 0.12)" }}>
            <div style={{ fontSize: 22, fontWeight: 800, marginBottom: 18 }}>Chỉnh sửa tài khoản</div>

            <div style={{ display: "grid", gap: 14 }}>
              <label style={{ display: "grid", gap: 6, fontSize: 12, fontWeight: 700 }}>
                Vai trò
                <select value={editForm.role} onChange={(e) => setEditForm((prev) => ({ ...prev, role: e.target.value }))} style={selectStyle}>
                  {Object.entries(ROLE_LABELS).map(([role, label]) => (
                    <option key={role} value={role}>{label}</option>
                  ))}
                </select>
              </label>

              <label style={{ display: "grid", gap: 6, fontSize: 12, fontWeight: 700 }}>
                Ngôn ngữ ưu tiên
                <select value={editForm.preferred_lang} onChange={(e) => setEditForm((prev) => ({ ...prev, preferred_lang: e.target.value }))} style={selectStyle}>
                  {Object.entries(LANGUAGE_LABELS).map(([lang, label]) => (
                    <option key={lang} value={lang}>{label}</option>
                  ))}
                </select>
              </label>

              <label style={{ display: "grid", gap: 6, fontSize: 12, fontWeight: 700 }}>
                Trạng thái hoạt động
                <select value={String(editForm.is_active)} onChange={(e) => setEditForm((prev) => ({ ...prev, is_active: e.target.value === "true" }))} style={selectStyle}>
                  <option value="true">Đang hoạt động</option>
                  <option value="false">Đã khóa</option>
                </select>
              </label>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 22 }}>
              <button type="button" onClick={() => { setShowEditModal(false); setEditingUser(null); }} style={{ ...buttonStyle, color: C.text }}>
                Hủy
              </button>
              <button type="button" onClick={() => void handleUpdateUser()} style={{ ...buttonStyle, background: C.red, borderColor: C.red, color: C.white }} disabled={submitting}>
                {submitting ? "Đang lưu..." : "Lưu thay đổi"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
