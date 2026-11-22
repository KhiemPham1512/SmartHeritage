// FILE: admin-web/src/pages/W7_Feedbacks.tsx — Kiểm duyệt đánh giá & phản hồi (Figma W7)
import { useEffect, useState, type CSSProperties, type ReactNode } from "react";
import { ArrowDownUp, Check, EyeOff, Search, ShieldCheck } from "lucide-react";
import { apiFetch } from "../api";
import type { Artifact } from "../types";
import { Banner, BRAND_RED, MaskIcon, Pager } from "../ui";

interface Feedback {
  feedback_id: number;
  visitor_name: string;
  artifact_id: number;
  artifact_code: string;
  artifact_name: string;
  rating: number;
  comment: string | null;
  is_visible: boolean;
  hide_reason: string | null;
  created_at: string;
}

interface FeedbackPage {
  stats: { total_feedbacks: number; avg_rating: number; visible_count: number; hidden_count: number };
  total: number;
  items: Feedback[];
}

const PAGE_SIZE = 6;
const OTHER = "Khác";
const HIDE_REASONS = ["Quảng cáo / Spam", "Ngôn từ xúc phạm", "Không liên quan đến hiện vật", "Chứa thông tin cá nhân", OTHER];
const SORTS: Array<[string, string]> = [["newest", "Mới nhất"], ["oldest", "Cũ nhất"], ["rating_desc", "Sao cao nhất"], ["rating_asc", "Sao thấp nhất"]];

const initials = (name: string) =>
  name.trim().split(/\s+/).slice(-2).map((w) => w[0]).join("").toUpperCase() || "?";

// created_at dạng "2026-09-30T16:45:00" (giờ máy chủ, GMT+7) → ["30/09 · 16:45", "2026 · GMT+7"]
function fmtTime(iso: string): [string, string] {
  const [date, time = ""] = iso.split("T");
  const [y, m, d] = date.split("-");
  return [`${d}/${m} · ${time.slice(0, 5)}`, `${y} · GMT+7`];
}

function Stars({ value }: { value: number }) {
  return <span aria-label={`${value} sao`} style={{ color: "#b45309", letterSpacing: 1, fontSize: 11 }}>{"★".repeat(value)}<span style={{ color: "#e2c9a3" }}>{"★".repeat(5 - value)}</span></span>;
}

export default function W7_Feedbacks() {
  const [data, setData] = useState<FeedbackPage | null>(null);
  const [artifacts, setArtifacts] = useState<Artifact[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<{ tone: "error" | "ok"; text: string } | null>(null);

  const [query, setQuery] = useState("");
  const [debounced, setDebounced] = useState("");
  const [artifactId, setArtifactId] = useState("");
  const [rating, setRating] = useState("");
  const [status, setStatus] = useState("");
  const [sort, setSort] = useState("newest");
  const [page, setPage] = useState(1);
  const [reloadKey, setReloadKey] = useState(0);

  const [hiding, setHiding] = useState<Feedback | null>(null);
  const [reason, setReason] = useState(HIDE_REASONS[0]);
  const [otherText, setOtherText] = useState("");
  const [showRules, setShowRules] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebounced(query.trim());
      setPage(1);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    apiFetch<Artifact[]>("/api/artifacts?lang=vi").then(setArtifacts).catch(() => setArtifacts([]));
  }, []);

  useEffect(() => {
    let active = true;
    const params = new URLSearchParams({ sort, page: String(page), page_size: String(PAGE_SIZE) });
    if (debounced) params.set("search", debounced);
    if (artifactId) params.set("artifact_id", artifactId);
    if (rating) params.set("rating", rating);
    if (status) params.set("is_visible", status);
    apiFetch<FeedbackPage>(`/api/feedbacks?${params.toString()}`)
      .then((res) => active && setData(res))
      .catch((e: Error) => active && setNotice({ tone: "error", text: e.message }))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [debounced, artifactId, rating, status, sort, page, reloadKey]);

  const refresh = () => {
    setLoading(true);
    setReloadKey((n) => n + 1);
  };
  const filterChange = (setter: (v: string) => void) => (v: string) => {
    setter(v);
    setPage(1);
    setLoading(true);
  };

  const setVisibility = async (fb: Feedback, visible: boolean, hideReason?: string) => {
    setBusy(true);
    setNotice(null);
    try {
      await apiFetch(`/api/feedbacks/${fb.feedback_id}/visibility`, {
        method: "PUT",
        body: JSON.stringify({ is_visible: visible, hide_reason: hideReason ?? null }),
      });
      setNotice({ tone: "ok", text: visible ? "Đã duyệt — bình luận hiển thị lại." : "Đã ẩn bình luận." });
      setHiding(null);
      refresh();
    } catch (e) {
      setNotice({ tone: "error", text: e instanceof Error ? e.message : "Không cập nhật được bình luận." });
    } finally {
      setBusy(false);
    }
  };

  const openHide = (fb: Feedback) => {
    setReason(HIDE_REASONS[0]);
    setOtherText("");
    setHiding(fb);
  };
  const confirmHide = () => {
    if (!hiding) return;
    const finalReason = reason === OTHER ? otherText.trim() : reason;
    if (!finalReason) return setNotice({ tone: "error", text: "Nhập lý do ẩn." });
    void setVisibility(hiding, false, finalReason.slice(0, 200));
  };

  const stats = data?.stats;
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const num = (n: number | undefined) => (n ?? 0).toLocaleString("vi-VN");

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <h2 style={{ fontSize: 24, fontWeight: 800, color: "#0f172a" }}>Đánh giá & phản hồi</h2>
          <p style={{ fontSize: 13, color: "#475569", marginTop: 2 }}>Lắng nghe khách tham quan và giữ gìn một không gian chia sẻ văn minh.</p>
        </div>
        <button type="button" style={outlineBtnStyle} onClick={() => setShowRules(true)}>
          <ShieldCheck size={16} /> Quy tắc kiểm duyệt
        </button>
      </div>

      {notice ? <Banner tone={notice.tone} text={notice.text} onClose={() => setNotice(null)} /> : null}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 14 }}>
        <Kpi icon="/kpi_dg_tong_danh_gia.svg" iconColor="#1f2937" value={num(stats?.total_feedbacks)} label="Tổng đánh giá" color="#0f172a" />
        <Kpi icon="/diem_trung_binh.svg" iconColor="#b45309" value={stats && stats.total_feedbacks > 0 ? `${stats.avg_rating.toLocaleString("vi-VN")} / 5` : "—"} label="Điểm trung bình" color="#b45309" />
        <Kpi icon="/kpi_dg_dang_hien_thi.svg" iconColor="#15803d" value={num(stats?.visible_count)} label="Đang hiển thị" color="#15803d" />
        <Kpi icon="/kpi_dg_da_an.svg" iconColor="#64748b" value={num(stats?.hidden_count)} label="Đã ẩn" color="#475569" />
      </div>

      <div style={cardStyle}>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
          <label style={{ ...fieldBoxStyle, flex: 1, minWidth: 260 }}>
            <Search size={16} color="#64748b" />
            <input value={query} onChange={(e) => { setQuery(e.target.value); setLoading(true); }} placeholder="Tìm khách, hiện vật hoặc bình luận..." style={{ border: "none", outline: "none", flex: 1, fontSize: 13.5, background: "transparent" }} />
          </label>
          <Select value={artifactId} onChange={filterChange(setArtifactId)} label="Lọc hiện vật">
            <option value="">Tất cả hiện vật</option>
            {artifacts.map((a) => <option key={a.artifact_id} value={a.artifact_id}>{a.artifact_code} · {a.name_vi}</option>)}
          </Select>
          <Select value={rating} onChange={filterChange(setRating)} label="Lọc số sao">
            <option value="">Tất cả số sao</option>
            {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} sao</option>)}
          </Select>
          <Select value={status} onChange={filterChange(setStatus)} label="Lọc trạng thái">
            <option value="">Tất cả trạng thái</option>
            <option value="true">Hiển thị</option>
            <option value="false">Đã ẩn</option>
          </Select>
          <label style={{ ...fieldBoxStyle, gap: 6 }}>
            <ArrowDownUp size={15} color="#475569" />
            <select value={sort} onChange={(e) => { setSort(e.target.value); setPage(1); setLoading(true); }} aria-label="Sắp xếp" style={{ border: "none", background: "transparent", fontSize: 13.5, fontWeight: 600, cursor: "pointer" }}>
              {SORTS.map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </label>
        </div>

        <div style={{ overflowX: "auto", marginTop: 14, opacity: loading ? 0.6 : 1, transition: "opacity 0.15s" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13.5 }}>
            <thead>
              <tr style={{ backgroundColor: "#f1f5f9" }}>
                {["Tên khách", "Hiện vật", "Số sao", "Nội dung bình luận", "Thời gian gửi", "Trạng thái", "Kiểm duyệt"].map((h) => <th key={h} style={thStyle}>{h}</th>)}
              </tr>
            </thead>
            <tbody>
              {!data || data.items.length === 0 ? (
                <tr><td colSpan={7} style={{ padding: 28, textAlign: "center", color: "#64748b" }}>{loading ? "Đang tải..." : "Không có đánh giá phù hợp bộ lọc."}</td></tr>
              ) : data.items.map((fb) => {
                const [t1, t2] = fmtTime(fb.created_at);
                return (
                  <tr key={fb.feedback_id} style={{ borderBottom: "1px solid #e2e8f0", backgroundColor: fb.is_visible ? "transparent" : "#fafafa" }}>
                    <td style={tdStyle}>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <span style={avatarStyle}>{initials(fb.visitor_name)}</span>
                        <span style={{ color: "#0f172a" }}>{fb.visitor_name}</span>
                      </div>
                    </td>
                    <td style={tdStyle}>
                      <div style={{ color: "#0f172a" }}>{fb.artifact_name}</div>
                      <div style={{ fontSize: 11.5, color: "#94a3b8" }}>{fb.artifact_code}</div>
                    </td>
                    <td style={tdStyle}>
                      <div style={{ whiteSpace: "nowrap" }}>{fb.rating}/5 <span style={{ color: "#b45309" }}>☆</span></div>
                      <Stars value={fb.rating} />
                    </td>
                    <td style={{ ...tdStyle, maxWidth: 330, color: fb.is_visible ? "#0f172a" : "#64748b", lineHeight: 1.45 }}>
                      {fb.comment ? (fb.is_visible ? fb.comment : `[${fb.comment}]`) : <i style={{ color: "#94a3b8" }}>(Không có nhận xét)</i>}
                      {!fb.is_visible && fb.hide_reason ? <div style={{ fontSize: 11.5, color: "#b91c1c", marginTop: 3 }}>Lý do ẩn: {fb.hide_reason}</div> : null}
                    </td>
                    <td style={{ ...tdStyle, whiteSpace: "nowrap" }}>
                      <div style={{ fontSize: 12.5 }}>{t1}</div>
                      <div style={{ fontSize: 11.5, color: "#94a3b8" }}>{t2}</div>
                    </td>
                    <td style={tdStyle}>
                      <span style={{ ...statusChipStyle, color: fb.is_visible ? "#15803d" : "#64748b", backgroundColor: fb.is_visible ? "#ecfdf3" : "#f1f5f9" }}>{fb.is_visible ? "Hiển thị" : "Đã ẩn"}</span>
                    </td>
                    <td style={tdStyle}>
                      {fb.is_visible ? (
                        <button type="button" style={actionBtnStyle} disabled={busy} onClick={() => openHide(fb)}><EyeOff size={15} /> Ẩn</button>
                      ) : (
                        <button type="button" style={actionBtnStyle} disabled={busy} onClick={() => void setVisibility(fb, true)}><Check size={15} /> Duyệt</button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 14 }}>
          <span style={{ fontSize: 13, color: "#475569" }}>
            {total === 0 ? "Không có kết quả" : `Hiển thị ${(page - 1) * PAGE_SIZE + 1}–${Math.min(page * PAGE_SIZE, total)} trên ${total.toLocaleString("vi-VN")} kết quả`}
          </span>
          <Pager page={Math.min(page, pageCount)} pageCount={pageCount} onChange={(p) => { setPage(p); setLoading(true); }} />
        </div>
      </div>

      <p style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 12.5, color: "#475569" }}>
        <ShieldCheck size={15} /> Ẩn bình luận không phù hợp thay vì xóa. Nội dung đã ẩn có thể được duyệt để hiển thị trở lại.
      </p>

      {hiding ? (
        <Modal onClose={() => !busy && setHiding(null)} title="Ẩn bình luận">
          <p style={{ fontSize: 13, color: "#475569", marginTop: 6 }}>“{hiding.comment ?? "Không có nhận xét"}” — {hiding.visitor_name}</p>
          <div style={{ marginTop: 12, display: "flex", flexDirection: "column", gap: 8 }}>
            {HIDE_REASONS.map((r) => (
              <label key={r} style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13.5, color: "#0f172a" }}>
                <input type="radio" name="reason" checked={reason === r} onChange={() => setReason(r)} style={{ accentColor: BRAND_RED }} /> {r}
              </label>
            ))}
            {reason === OTHER ? <input value={otherText} maxLength={200} onChange={(e) => setOtherText(e.target.value)} placeholder="Nhập lý do..." style={inputStyle} /> : null}
          </div>
          <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 18 }}>
            <button type="button" style={outlineBtnStyle} disabled={busy} onClick={() => setHiding(null)}>Hủy</button>
            <button type="button" style={primaryBtnStyle} disabled={busy} onClick={confirmHide}>{busy ? "Đang lưu..." : "Ẩn bình luận"}</button>
          </div>
        </Modal>
      ) : null}

      {showRules ? (
        <Modal onClose={() => setShowRules(false)} title="Quy tắc kiểm duyệt">
          <p style={{ fontSize: 13, color: "#475569", marginTop: 6 }}>Ẩn (không xóa) bình luận thuộc các trường hợp sau; có thể duyệt lại bất cứ lúc nào:</p>
          <ul style={{ margin: "10px 0 0 18px", fontSize: 13.5, color: "#0f172a", lineHeight: 1.8 }}>
            {HIDE_REASONS.filter((r) => r !== OTHER).map((r) => <li key={r}>{r}</li>)}
          </ul>
          <p style={{ fontSize: 12.5, color: "#475569", marginTop: 10 }}>Bình luận đã ẩn không được tính vào điểm trung bình (ở trang này và Dashboard).</p>
          <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 16 }}>
            <button type="button" style={primaryBtnStyle} onClick={() => setShowRules(false)}>Đã hiểu</button>
          </div>
        </Modal>
      ) : null}
    </div>
  );
}

function Kpi({ icon, iconColor, value, label, color }: { icon: string; iconColor: string; value: string; label: string; color: string }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 14, backgroundColor: "#fff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "16px 18px" }}>
      <MaskIcon src={icon} color={iconColor} size={22} />
      <div>
        <div style={{ fontSize: 26, fontWeight: 800, color, lineHeight: 1.1 }}>{value}</div>
        <div style={{ fontSize: 12.5, color: "#475569", marginTop: 3 }}>{label}</div>
      </div>
    </div>
  );
}

function Select({ value, onChange, label, children }: { value: string; onChange: (v: string) => void; label: string; children: ReactNode }) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)} aria-label={label} style={{ ...fieldBoxStyle, fontSize: 13.5, minWidth: 150, maxWidth: 230, cursor: "pointer" }}>
      {children}
    </select>
  );
}

function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div style={overlayStyle} role="dialog" aria-modal="true" aria-label={title} onClick={onClose}>
      <div style={modalStyle} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ fontSize: 18, fontWeight: 700, color: "#0f172a" }}>{title}</h3>
        {children}
      </div>
    </div>
  );
}

const cardStyle: CSSProperties = { backgroundColor: "#fff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "18px 20px" };
const fieldBoxStyle: CSSProperties = { display: "flex", alignItems: "center", gap: 8, height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", color: "#0f172a" };
const thStyle: CSSProperties = { textAlign: "left", padding: "12px", fontSize: 12.5, fontWeight: 600, color: "#334155" };
const tdStyle: CSSProperties = { padding: "12px", verticalAlign: "middle" };
const avatarStyle: CSSProperties = { width: 28, height: 28, borderRadius: "50%", backgroundColor: "#f1f5f9", color: "#64748b", fontSize: 10.5, fontWeight: 600, display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0 };
const statusChipStyle: CSSProperties = { fontSize: 12, fontWeight: 600, padding: "4px 10px", borderRadius: 5 };
const actionBtnStyle: CSSProperties = { display: "inline-flex", alignItems: "center", gap: 6, height: 34, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", fontSize: 13, fontWeight: 600, color: "#0f172a", cursor: "pointer" };
const outlineBtnStyle: CSSProperties = { display: "inline-flex", alignItems: "center", gap: 8, height: 40, padding: "0 16px", border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", fontSize: 13.5, fontWeight: 600, color: "#0f172a", cursor: "pointer" };
const primaryBtnStyle: CSSProperties = { height: 40, padding: "0 18px", border: "none", borderRadius: 8, backgroundColor: BRAND_RED, color: "#fff", fontSize: 13.5, fontWeight: 700, cursor: "pointer" };
const inputStyle: CSSProperties = { width: "100%", height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 13.5 };
const overlayStyle: CSSProperties = { position: "fixed", inset: 0, zIndex: 50, backgroundColor: "rgba(15,23,42,0.45)", display: "flex", alignItems: "center", justifyContent: "center" };
const modalStyle: CSSProperties = { width: 440, maxWidth: "92vw", backgroundColor: "#fff", borderRadius: 12, padding: "22px 24px" };
