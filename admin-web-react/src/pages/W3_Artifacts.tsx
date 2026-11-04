import { useEffect, useMemo, useState, type ReactNode } from "react";
import { Search, RefreshCw, Plus, Pencil, Trash2, Image as ImageIcon, Radio, Package, Eye, EyeOff } from "lucide-react";
import { apiFetch, mediaUrl } from "../api";
import type { Artifact, ArtifactStats } from "../types";

interface Props { onAdd: () => void; onEdit: (id: number) => void; }
const emptyStats: ArtifactStats = { total_artifacts: 0, published_count: 0, hidden_or_draft_count: 0, assigned_beacon_count: 0 };
const statusText: Record<string, string> = { PUBLISHED: "Đang hiển thị", HIDDEN: "Tạm ẩn", DRAFT: "Bản nháp" };

export default function W3_Artifacts({ onAdd, onEdit }: Props) {
  const [items, setItems] = useState<Artifact[]>([]);
  const [stats, setStats] = useState<ArtifactStats>(emptyStats);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [zone, setZone] = useState("ALL");

  const load = async () => {
    setLoading(true); setError(""); setNotice("");
    try {
      const list = await apiFetch<Artifact[]>("/artifacts?lang=vi");
      setItems(Array.isArray(list) ? list : []);
      try {
        const summary = await apiFetch<ArtifactStats>("/artifacts/stats");
        setStats(summary);
      } catch {
        // Keep the real list visible if the optional KPI endpoint is temporarily unavailable.
        setStats({
          total_artifacts: list.length,
          published_count: list.filter(item => item.status === "PUBLISHED").length,
          hidden_or_draft_count: list.filter(item => item.status === "HIDDEN" || item.status === "DRAFT").length,
          assigned_beacon_count: list.filter(item => Boolean(item.beacon_id)).length,
        });
      }
    } catch (e) { setError(e instanceof Error ? e.message : "Không tải được dữ liệu hiện vật."); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);

  const zones = useMemo(() => [...new Set(items.map(x => x.zone_name).filter(Boolean))].sort(), [items]);
  const filtered = useMemo(() => items.filter(item => {
    const haystack = `${item.artifact_code} ${item.name_vi} ${item.name_en}`.toLocaleLowerCase("vi");
    return haystack.includes(query.trim().toLocaleLowerCase("vi"))
      && (status === "ALL" || item.status === status)
      && (zone === "ALL" || item.zone_name === zone);
  }), [items, query, status, zone]);

  const remove = async (item: Artifact) => {
    if (!window.confirm(`Bạn chắc chắn muốn xóa hiện vật ${item.artifact_code} – ${item.name_vi}? Thao tác này không thể hoàn tác.`)) return;
    setDeleting(item.artifact_id); setError(""); setNotice("");
    try {
      await apiFetch(`/artifacts/${item.artifact_id}`, { method: "DELETE" });
      await load();
      setNotice(`Đã xóa hiện vật ${item.artifact_code}.`);
    } catch (e) { setError(e instanceof Error ? e.message : "Không thể xóa hiện vật."); }
    finally { setDeleting(null); }
  };

  return <section className="content">
    <div className="page-intro"><div><div className="eyebrow">CONTENT MANAGEMENT SYSTEM</div><h1>Quản lý hiện vật</h1><p>Quản lý nội dung di sản, tư liệu đa ngôn ngữ và trạng thái trưng bày.</p></div><button className="btn btn-primary" onClick={onAdd}><Plus size={18}/> Thêm hiện vật</button></div>
    {error && <div className="alert error" role="alert">{error}<button onClick={() => setError("")}>Đóng</button></div>}
    {notice && <div className="alert success" role="status">{notice}<button onClick={() => setNotice("")}>Đóng</button></div>}
    <div className="stats-grid">
      <Stat icon={<Package/>} label="Tổng số hiện vật" value={stats.total_artifacts}/>
      <Stat icon={<Eye/>} label="Đang hiển thị" value={stats.published_count} tone="green"/>
      <Stat icon={<EyeOff/>} label="Tạm ẩn / Bản nháp" value={stats.hidden_or_draft_count} tone="amber"/>
      <Stat icon={<Radio/>} label="Đã gán iBeacon" value={stats.assigned_beacon_count} tone="blue"/>
    </div>
    <div className="panel">
      <div className="panel-head"><div><h2>Danh sách hiện vật</h2><p>{filtered.length} kết quả phù hợp</p></div><button className="btn btn-outline" onClick={() => void load()} disabled={loading}><RefreshCw size={16} className={loading ? "spin" : ""}/> Làm mới</button></div>
      <div className="filters">
        <label className="searchbox"><Search size={18}/><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Tìm theo mã hoặc tên hiện vật..."/></label>
        <select aria-label="Lọc trạng thái" value={status} onChange={e => setStatus(e.target.value)}><option value="ALL">Tất cả trạng thái</option><option value="PUBLISHED">Đang hiển thị</option><option value="HIDDEN">Tạm ẩn</option><option value="DRAFT">Bản nháp</option></select>
        <select aria-label="Lọc khu trưng bày" value={zone} onChange={e => setZone(e.target.value)}><option value="ALL">Tất cả khu trưng bày</option>{zones.map(z => <option key={z} value={z}>{z}</option>)}</select>
      </div>
      <div className="table-scroll"><table className="artifact-table"><thead><tr><th>Mã hiện vật</th><th>Ảnh</th><th>Tên tiếng Việt</th><th>Tên tiếng Anh</th><th>Khu trưng bày</th><th>iBeacon</th><th>Ngôn ngữ</th><th>Trạng thái</th><th className="right">Thao tác</th></tr></thead>
        <tbody>{loading ? <tr><td colSpan={9} className="table-state">Đang tải dữ liệu từ máy chủ...</td></tr> : filtered.length === 0 ? <tr><td colSpan={9} className="table-state"><ImageIcon size={28}/><b>Chưa tìm thấy hiện vật</b><span>Thử thay đổi từ khóa hoặc bộ lọc.</span></td></tr> : filtered.map(item => <tr key={item.artifact_id}>
          <td><span className="code">{item.artifact_code}</span></td>
          <td>{item.image_url ? <img className="artifact-thumb" src={mediaUrl(item.image_url)} alt={item.name_vi} onError={e => { e.currentTarget.style.visibility = "hidden"; }}/> : <div className="image-placeholder"><ImageIcon size={18}/></div>}</td>
          <td><b className="artifact-name">{item.name_vi || "Chưa có tên tiếng Việt"}</b></td><td><span className="artifact-name-en">{item.name_en || "Chưa có tên tiếng Anh"}</span></td>
          <td>{item.zone_name || "—"}</td><td>{item.beacon_code ? <span className="beacon-tag"><Radio size={13}/>{item.beacon_code}</span> : <span className="muted-text">Chưa gán</span>}</td>
          <td><div className="lang-tags">{(item.translated_langs || "VI,EN").split(",").map(l => <span key={l}>{l.trim()}</span>)}</div></td><td><span className={`status status-${item.status.toLowerCase()}`}>{statusText[item.status] || item.status}</span></td>
          <td><div className="row-actions"><button className="icon-action" title="Sửa hiện vật" aria-label={`Sửa ${item.artifact_code}`} onClick={() => onEdit(item.artifact_id)}><Pencil size={16}/></button><button className="icon-action danger" title="Xóa hiện vật" aria-label={`Xóa ${item.artifact_code}`} disabled={deleting === item.artifact_id} onClick={() => void remove(item)}><Trash2 size={16}/></button></div></td>
        </tr>)}</tbody></table></div>
      <div className="table-foot">Dữ liệu được tải trực tiếp từ Backend Smart Heritage.</div>
    </div>
  </section>;
}
function Stat({ icon, label, value, tone = "green" }: { icon: ReactNode; label: string; value: number; tone?: string }) {
  return <div className="stat-card"><div className={`stat-icon ${tone}`}>{icon}</div><div><span>{label}</span><strong>{Number(value || 0).toLocaleString("vi-VN")}</strong></div></div>;
}
