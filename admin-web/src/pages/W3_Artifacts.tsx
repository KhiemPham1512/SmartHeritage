// FILE: admin-web/src/pages/W3_Artifacts.tsx — Quản lý hiện vật (Figma W3)
import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { Search, RotateCcw, Image as ImageIcon } from "lucide-react";
import { apiFetch, mediaUrl } from "../api";
import type { Artifact } from "../types";
import { BRAND_RED, MaskIcon, Pager } from "../ui";

interface Props {
  onAdd: () => void;
  onEdit: (id: number) => void;
}

type Tab = "ALL" | "PUBLISHED" | "DRAFT";
const PAGE_SIZE = 8;

const STATUS_BADGE: Record<string, { label: string; color: string; bg: string }> = {
  PUBLISHED: { label: "Đang hiển thị", color: "#15803d", bg: "#ecfdf3" },
  DRAFT: { label: "Bản nháp", color: "#b45309", bg: "#fff7ed" },
  HIDDEN: { label: "Tạm ẩn", color: "#475569", bg: "#f1f5f9" },
};
const LANG_BADGE: Record<string, { color: string; bg: string }> = {
  VI: { color: "#9b1c1c", bg: "#fdecea" },
  EN: { color: "#1d4ed8", bg: "#eef4ff" },
};
const OTHER_LANG_BADGE = { color: "#475569", bg: "#f1f5f9" };

export default function W3_Artifacts({ onAdd, onEdit }: Props) {
  const [items, setItems] = useState<Artifact[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [query, setQuery] = useState("");
  const [zone, setZone] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [tab, setTab] = useState<Tab>("ALL");
  const [page, setPage] = useState(1);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    apiFetch<Artifact[]>("/api/artifacts?lang=vi")
      .then((list) => active && setItems(list))
      .catch((e: Error) => active && setError(e.message))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [reloadKey]);

  const reload = () => {
    setLoading(true);
    setReloadKey((n) => n + 1);
  };

  const zones = useMemo(() => [...new Set(items.map((x) => x.zone_name).filter(Boolean))].sort(), [items]);
  const counts = useMemo(
    () => ({
      total: items.length,
      published: items.filter((x) => x.status === "PUBLISHED").length,
      draft: items.filter((x) => x.status === "DRAFT").length,
      translated: items.filter((x) => /VI/.test(x.translated_langs) && /EN/.test(x.translated_langs)).length,
    }),
    [items],
  );

  const filtered = useMemo(() => {
    const q = query.trim().toLocaleLowerCase("vi");
    return items.filter(
      (x) =>
        `${x.artifact_code} ${x.name_vi} ${x.name_en}`.toLocaleLowerCase("vi").includes(q) &&
        (zone === "ALL" || x.zone_name === zone) &&
        (status === "ALL" || x.status === status) &&
        (tab === "ALL" || x.status === tab),
    );
  }, [items, query, zone, status, tab]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pageItems = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  // Đổi bộ lọc → quay về trang 1
  const withReset = <T,>(setter: (v: T) => void) => (v: T) => {
    setter(v);
    setPage(1);
  };

  const remove = async (item: Artifact) => {
    if (!window.confirm(`Xóa hiện vật ${item.artifact_code} – ${item.name_vi}? Thao tác này không thể hoàn tác.`)) return;
    setDeleting(item.artifact_id);
    setError("");
    try {
      await apiFetch(`/api/artifacts/${item.artifact_id}`, { method: "DELETE" });
      setNotice(`Đã xóa hiện vật ${item.artifact_code}.`);
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Không thể xóa hiện vật.");
    } finally {
      setDeleting(null);
    }
  };

  const resetFilters = () => {
    setQuery("");
    setZone("ALL");
    setStatus("ALL");
    setTab("ALL");
    setPage(1);
  };

  const tabs: Array<[Tab, string]> = [
    ["ALL", `Tất cả hiện vật (${counts.total})`],
    ["PUBLISHED", `Đang hiển thị (${counts.published})`],
    ["DRAFT", `Bản nháp (${counts.draft})`],
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      <div style={headerRowStyle}>
        <div>
          <h2 style={titleStyle}>Quản lý hiện vật</h2>
          <p style={subtitleStyle}>Lưu giữ câu chuyện, kết nối hiện vật với trải nghiệm tham quan.</p>
        </div>
        <button type="button" style={primaryBtnStyle} onClick={onAdd}>+ Thêm hiện vật mới</button>
      </div>

      {error ? <Banner tone="error" text={error} onClose={() => setError("")} /> : null}
      {notice ? <Banner tone="ok" text={notice} onClose={() => setNotice("")} /> : null}

      <div style={kpiGridStyle}>
        <Kpi icon="/tong_hien_vat.svg" value={counts.total} label="Tổng hiện vật" />
        <Kpi icon="/dang_hien_thi.svg" value={counts.published} label="Đang hiển thị" />
        <Kpi icon="/ban_nhap.svg" value={counts.draft} label="Bản nháp" />
        <Kpi icon="/da_dich.svg" value={counts.translated} label="Đã dịch VI & EN" />
      </div>

      <div style={panelStyle}>
        <div style={filterRowStyle}>
          <label style={searchBoxStyle}>
            <Search size={17} color="#64748b" />
            <input
              value={query}
              onChange={(e) => withReset(setQuery)(e.target.value)}
              placeholder="Tìm tên hoặc mã hiện vật..."
              style={searchInputStyle}
            />
          </label>
          <select value={zone} onChange={(e) => withReset(setZone)(e.target.value)} style={selectStyle} aria-label="Lọc khu vực">
            <option value="ALL">Tất cả khu vực / khu di tích</option>
            {zones.map((z) => <option key={z} value={z}>{z}</option>)}
          </select>
          <select value={status} onChange={(e) => withReset(setStatus)(e.target.value)} style={selectStyle} aria-label="Lọc trạng thái">
            <option value="ALL">Tất cả trạng thái</option>
            <option value="PUBLISHED">Đang hiển thị</option>
            <option value="DRAFT">Bản nháp</option>
            <option value="HIDDEN">Tạm ẩn</option>
          </select>
          <button type="button" style={{ ...outlineBtnStyle, marginLeft: "auto" }} onClick={resetFilters} title="Xóa mọi bộ lọc">
            <RotateCcw size={15} /> Bộ lọc
          </button>
        </div>

        <div style={tabRowStyle} role="tablist">
          {tabs.map(([key, label]) => (
            <button
              key={key}
              type="button"
              role="tab"
              aria-selected={tab === key}
              onClick={() => withReset(setTab)(key)}
              style={{ ...tabStyle, color: tab === key ? BRAND_RED : "#475569", borderBottomColor: tab === key ? BRAND_RED : "transparent" }}
            >
              {label}
            </button>
          ))}
        </div>

        <div style={{ overflowX: "auto" }}>
          <table style={tableStyle}>
            <thead>
              <tr style={theadRowStyle}>
                {["Mã hiện vật", "Ảnh", "Tên hiện vật", "iBeacon đang gắn", "Ngôn ngữ đã dịch", "Trạng thái", "Thao tác"].map((h) => (
                  <th key={h} style={thStyle}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={7} style={stateCellStyle}>Đang tải dữ liệu từ máy chủ...</td></tr>
              ) : pageItems.length === 0 ? (
                <tr><td colSpan={7} style={stateCellStyle}>Không có hiện vật phù hợp bộ lọc.</td></tr>
              ) : (
                pageItems.map((item) => {
                  const badge = STATUS_BADGE[item.status] ?? STATUS_BADGE.HIDDEN;
                  return (
                    <tr key={item.artifact_id} style={rowStyle}>
                      <td style={{ ...tdStyle, color: "#475569" }}>{item.artifact_code}</td>
                      <td style={tdStyle}>
                        {item.image_url ? (
                          <img src={mediaUrl(item.image_url)} alt={item.name_vi} style={thumbStyle} />
                        ) : (
                          <div style={{ ...thumbStyle, display: "flex", alignItems: "center", justifyContent: "center", backgroundColor: "#f1f5f9" }}>
                            <ImageIcon size={18} color="#94a3b8" />
                          </div>
                        )}
                      </td>
                      <td style={tdStyle}>
                        <div style={{ fontWeight: 600, color: "#0f172a" }}>{item.name_vi}</div>
                        <div style={{ fontSize: 12, color: "#64748b", marginTop: 2 }}>{item.zone_name}</div>
                      </td>
                      <td style={tdStyle}>
                        <span style={{ display: "inline-flex", alignItems: "center", gap: 6, color: item.beacon_code ? "#0f172a" : "#94a3b8" }}>
                          <MaskIcon src="/thiet_bi_ibeacon.svg" color={item.beacon_code ? "#16a34a" : "#cbd5e1"} size={15} />
                          {item.beacon_code ?? "Chưa gán"}
                        </span>
                      </td>
                      <td style={tdStyle}>
                        <div style={{ display: "flex", gap: 6 }}>
                          {(item.translated_langs || "VI").split(",").map((l) => {
                            const code = l.trim();
                            const c = LANG_BADGE[code] ?? OTHER_LANG_BADGE;
                            return <span key={code} style={{ ...langBadgeStyle, color: c.color, backgroundColor: c.bg }}>{code}</span>;
                          })}
                        </div>
                      </td>
                      <td style={tdStyle}>
                        <span style={{ ...statusBadgeStyle, color: badge.color, backgroundColor: badge.bg }}>{badge.label}</span>
                      </td>
                      <td style={tdStyle}>
                        <button type="button" style={editLinkStyle} onClick={() => onEdit(item.artifact_id)}>Sửa</button>
                        <button type="button" style={deleteLinkStyle} disabled={deleting === item.artifact_id} onClick={() => void remove(item)}>
                          {deleting === item.artifact_id ? "Đang xóa..." : "Xóa"}
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div style={footerRowStyle}>
          <span style={{ color: "#475569", fontSize: 13 }}>
            {filtered.length === 0
              ? "Không có kết quả"
              : `Hiển thị ${(currentPage - 1) * PAGE_SIZE + 1}–${Math.min(currentPage * PAGE_SIZE, filtered.length)} trên ${filtered.length} kết quả`}
          </span>
          <Pager page={currentPage} pageCount={pageCount} onChange={setPage} />
        </div>
      </div>

      <p style={{ fontSize: 12.5, color: "#475569" }}>
        Mẹo: Mở “Sửa” để quản lý bài viết, hình ảnh và thuyết minh theo từng ngôn ngữ.
      </p>
    </div>
  );
}

function Kpi({ icon, value, label }: { icon: string; value: number; label: string }) {
  return (
    <div style={kpiCardStyle}>
      <div style={kpiIconBoxStyle}><MaskIcon src={icon} size={20} /></div>
      <div>
        <div style={{ fontSize: 26, fontWeight: 800, color: "#0f172a", lineHeight: 1.1 }}>{value.toLocaleString("vi-VN")}</div>
        <div style={{ fontSize: 12.5, color: "#475569", marginTop: 3 }}>{label}</div>
      </div>
    </div>
  );
}

function Banner({ tone, text, onClose }: { tone: "error" | "ok"; text: string; onClose: () => void }) {
  const c = tone === "error" ? { color: "#b91c1c", bg: "#fef2f2", border: "#fca5a5" } : { color: "#166534", bg: "#f0fdf4", border: "#86efac" };
  return (
    <div role={tone === "error" ? "alert" : "status"} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "8px 12px", borderRadius: 8, border: `1px solid ${c.border}`, backgroundColor: c.bg, color: c.color, fontSize: 13, fontWeight: 600 }}>
      <span>{text}</span>
      <button type="button" onClick={onClose} style={{ background: "none", border: "none", color: c.color, cursor: "pointer", fontWeight: 700 }}>Đóng</button>
    </div>
  );
}

const headerRowStyle: CSSProperties = { display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 };
const titleStyle: CSSProperties = { fontSize: 24, fontWeight: 800, color: "#0f172a" };
const subtitleStyle: CSSProperties = { fontSize: 13, color: "#475569", marginTop: 2 };
const primaryBtnStyle: CSSProperties = { height: 40, padding: "0 18px", backgroundColor: BRAND_RED, color: "#fff", border: "none", borderRadius: 8, fontSize: 13.5, fontWeight: 700, cursor: "pointer" };
const kpiGridStyle: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 14 };
const kpiCardStyle: CSSProperties = { display: "flex", alignItems: "center", gap: 14, backgroundColor: "#fff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "16px 18px" };
const kpiIconBoxStyle: CSSProperties = { width: 36, height: 36, borderRadius: 8, backgroundColor: "#fde8e6", display: "flex", alignItems: "center", justifyContent: "center" };
const panelStyle: CSSProperties = { backgroundColor: "#fff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "18px 20px" };
const filterRowStyle: CSSProperties = { display: "flex", alignItems: "center", gap: 12, flexWrap: "wrap" };
const searchBoxStyle: CSSProperties = { display: "flex", alignItems: "center", gap: 8, height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, width: 330 };
const searchInputStyle: CSSProperties = { border: "none", outline: "none", flex: 1, fontSize: 13.5, color: "#0f172a", background: "transparent" };
const selectStyle: CSSProperties = { height: 40, padding: "0 10px", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 13.5, color: "#0f172a", backgroundColor: "#fff", minWidth: 180 };
const outlineBtnStyle: CSSProperties = { height: 40, padding: "0 14px", display: "inline-flex", alignItems: "center", gap: 8, border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", fontSize: 13.5, fontWeight: 600, color: "#0f172a", cursor: "pointer" };
const tabRowStyle: CSSProperties = { display: "flex", gap: 24, margin: "16px 0 10px" };
const tabStyle: CSSProperties = { background: "none", border: "none", borderBottom: "2px solid transparent", padding: "4px 0", fontSize: 13.5, cursor: "pointer" };
const tableStyle: CSSProperties = { width: "100%", borderCollapse: "collapse", fontSize: 13.5 };
const theadRowStyle: CSSProperties = { backgroundColor: "#f1f5f9" };
const thStyle: CSSProperties = { textAlign: "left", padding: "12px", fontSize: 12.5, fontWeight: 600, color: "#334155" };
const rowStyle: CSSProperties = { borderBottom: "1px solid #e2e8f0" };
const tdStyle: CSSProperties = { padding: "8px 12px", verticalAlign: "middle" };
const thumbStyle: CSSProperties = { width: 44, height: 44, borderRadius: 6, objectFit: "cover" };
const langBadgeStyle: CSSProperties = { fontSize: 11.5, fontWeight: 600, padding: "3px 8px", borderRadius: 5 };
const statusBadgeStyle: CSSProperties = { fontSize: 12, fontWeight: 600, padding: "4px 8px", borderRadius: 5 };
const editLinkStyle: CSSProperties = { background: "none", border: "none", color: BRAND_RED, fontWeight: 700, cursor: "pointer", marginRight: 14, fontSize: 13.5 };
const deleteLinkStyle: CSSProperties = { background: "none", border: "none", color: "#64748b", cursor: "pointer", fontSize: 13.5 };
const stateCellStyle: CSSProperties = { padding: 28, textAlign: "center", color: "#64748b" };
const footerRowStyle: CSSProperties = { display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 14 };
