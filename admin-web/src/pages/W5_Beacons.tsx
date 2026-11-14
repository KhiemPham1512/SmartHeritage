import { useCallback, useEffect, useMemo, useState, type CSSProperties, type FormEvent } from "react";
import { apiFetch } from "../api";

type BeaconStatus = "ACTIVE" | "LOW_BATTERY" | "LOST_SIGNAL";
type Beacon = {
  beacon_id: number;
  beacon_code: string;
  artifact_id: number | null;
  artifact_code?: string | null;
  artifact_name_vi?: string | null;
  zone_name: string;
  uuid: string;
  major: number;
  minor: number;
  tx_power: number;
  trigger_distance: number;
  battery_level: number;
  map_x: number;
  map_y: number;
  status: BeaconStatus;
  last_seen_at?: string | null;
};
type BeaconStats = {
  total_beacons: number;
  active_count: number;
  low_battery_count: number;
  lost_signal_count: number;
};
type Artifact = { artifact_id: number; artifact_code: string; name_vi: string };
type BeaconForm = {
  beacon_code: string;
  artifact_id: string;
  zone_name: string;
  uuid: string;
  major: string;
  minor: string;
  tx_power: string;
  trigger_distance: string;
  battery_level: string;
  map_x: string;
  map_y: string;
  status: BeaconStatus;
};

const RED = "#80140e";
const cardStyle: CSSProperties = {
  backgroundColor: "#ffffff", border: "1.5px solid #d4cdc3", borderRadius: 11, padding: "16px 20px",
};
const primaryButton: CSSProperties = {
  backgroundColor: RED, color: "#ffffff", border: 0, borderRadius: 7,
  padding: "9px 14px", cursor: "pointer", fontWeight: 700,
};
const secondaryButton: CSSProperties = { ...primaryButton, backgroundColor: "#64748b" };
const inputStyle: CSSProperties = {
  width: "100%", boxSizing: "border-box", padding: "9px 10px", border: "1px solid #cbd5e1",
  borderRadius: 6, marginTop: 5, color: "#0f172a", backgroundColor: "#ffffff",
};
const labelStyle: CSSProperties = { display: "block", color: "#334155", fontSize: 13, fontWeight: 700 };
const tableCellStyle: CSSProperties = { padding: "11px 10px", borderBottom: "1px solid #eee", verticalAlign: "top" };

const EMPTY_FORM: BeaconForm = {
  beacon_code: "", artifact_id: "", zone_name: "Tầng 1 - Phòng Đông Sơn",
  uuid: "FDA50693-A4E2-4FB1-AFCF-C6EB07647825", major: "1", minor: "106",
  tx_power: "-59", trigger_distance: "2", battery_level: "100", map_x: "340", map_y: "215", status: "ACTIVE",
};

function statusLabel(status: BeaconStatus) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "LOW_BATTERY") return "Pin yếu";
  return "Mất tín hiệu";
}

function statusStyle(status: BeaconStatus): CSSProperties {
  if (status === "ACTIVE") return { backgroundColor: "#dcfce7", color: "#166534" };
  if (status === "LOW_BATTERY") return { backgroundColor: "#fef3c7", color: "#92400e" };
  return { backgroundColor: "#fde8e6", color: RED };
}

export default function W5_Beacons() {
  const [stats, setStats] = useState<BeaconStats>({ total_beacons: 0, active_count: 0, low_battery_count: 0, lost_signal_count: 0 });
  const [items, setItems] = useState<Beacon[]>([]);
  const [artifacts, setArtifacts] = useState<Artifact[]>([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [form, setForm] = useState<BeaconForm>(EMPTY_FORM);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const query = statusFilter ? `?status=${encodeURIComponent(statusFilter)}` : "";
      const [statsData, beaconData, artifactData] = await Promise.all([
        apiFetch<BeaconStats>("/api/beacons/stats"),
        apiFetch<Beacon[]>(`/api/beacons${query}`),
        apiFetch<Artifact[]>("/api/artifacts?lang=vi"),
      ]);
      setStats(statsData);
      setItems(beaconData);
      setArtifacts(artifactData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không tải được dữ liệu iBeacon.");
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { void loadData(); }, [loadData]);

  const filteredItems = useMemo(() => {
    const keyword = search.trim().toLocaleLowerCase("vi");
    if (!keyword) return items;
    return items.filter((beacon) => [
      beacon.beacon_code, beacon.uuid, beacon.artifact_code ?? "", beacon.artifact_name_vi ?? "", beacon.zone_name,
    ].some((value) => value.toLocaleLowerCase("vi").includes(keyword)));
  }, [items, search]);

  const resetForm = () => {
    setForm(EMPTY_FORM);
    setEditingId(null);
    setFormOpen(false);
  };

  const openCreateForm = () => {
    setError(""); setNotice(""); setForm(EMPTY_FORM); setEditingId(null); setFormOpen(true);
  };

  const openEditForm = (beacon: Beacon) => {
    setError(""); setNotice("");
    setEditingId(beacon.beacon_id);
    setForm({
      beacon_code: beacon.beacon_code,
      artifact_id: beacon.artifact_id == null ? "" : String(beacon.artifact_id),
      zone_name: beacon.zone_name,
      uuid: beacon.uuid,
      major: String(beacon.major),
      minor: String(beacon.minor),
      tx_power: String(beacon.tx_power),
      trigger_distance: String(beacon.trigger_distance),
      battery_level: String(beacon.battery_level),
      map_x: String(beacon.map_x),
      map_y: String(beacon.map_y),
      status: beacon.status,
    });
    setFormOpen(true);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(""); setNotice("");
    const distance = Number(form.trigger_distance);
    const battery = Number(form.battery_level);
    const major = Number(form.major);
    const minor = Number(form.minor);
    if (!form.beacon_code.trim() || !form.zone_name.trim() || !form.uuid.trim()) {
      setError("Vui lòng nhập mã thiết bị, UUID và khu vực."); return;
    }
    if (![major, minor, Number(form.tx_power), distance, battery, Number(form.map_x), Number(form.map_y)].every(Number.isFinite)
      || major < 0 || minor < 0 || distance <= 0 || battery < 0 || battery > 100) {
      setError("Thông số số không hợp lệ. Pin phải từ 0–100% và khoảng cách kích hoạt phải lớn hơn 0."); return;
    }

    const payload: Record<string, unknown> = {
      beacon_code: form.beacon_code.trim(),
      zone_name: form.zone_name.trim(),
      uuid: form.uuid.trim(),
      major, minor,
      tx_power: Number(form.tx_power),
      trigger_distance: distance,
      battery_level: battery,
      map_x: Number(form.map_x),
      map_y: Number(form.map_y),
      status: form.status,
    };
    // API hiện tại dùng COALESCE cho artifact_id khi cập nhật; bỏ trường này nếu không chọn
    // để tránh vô tình gỡ liên kết hiện vật đang có.
    if (form.artifact_id) payload.artifact_id = Number(form.artifact_id);
    else if (!editingId) payload.artifact_id = null;

    setSaving(true);
    try {
      if (editingId !== null) {
        await apiFetch(`/api/beacons/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
        setNotice("Đã cập nhật thiết bị iBeacon.");
      } else {
        await apiFetch("/api/beacons", { method: "POST", body: JSON.stringify(payload) });
        setNotice("Đã thêm thiết bị iBeacon.");
      }
      resetForm();
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể lưu thiết bị iBeacon.");
    } finally {
      setSaving(false);
    }
  };

  const deleteBeacon = async (beacon: Beacon) => {
    if (!window.confirm(`Bạn có chắc muốn xóa thiết bị ${beacon.beacon_code}?`)) return;
    setError(""); setNotice("");
    try {
      await apiFetch(`/api/beacons/${beacon.beacon_id}`, { method: "DELETE" });
      setNotice(`Đã xóa thiết bị ${beacon.beacon_code}.`);
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể xóa thiết bị.");
    }
  };

  const kpis = [
    { label: "Tổng thiết bị", value: stats.total_beacons, color: "#0f172a" },
    { label: "Đang hoạt động", value: stats.active_count, color: "#166534" },
    { label: "Pin yếu", value: stats.low_battery_count, color: "#b45309" },
    { label: "Mất tín hiệu", value: stats.lost_signal_count, color: RED },
  ];

  return (
    <div style={{ display: "grid", gap: 16, color: "#0f172a" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
        <div>
          <h2 style={{ margin: "0 0 5px", fontWeight: 800 }}>Quản lý thiết bị iBeacon</h2>
          <p style={{ margin: 0, color: "#334155", fontWeight: 600 }}>Theo dõi trạng thái, pin và hiện vật được gán cho từng thiết bị.</p>
        </div>
        <button type="button" style={primaryButton} onClick={openCreateForm}>＋ Thêm iBeacon</button>
      </div>

      {error && <div role="alert" style={{ ...cardStyle, color: RED, backgroundColor: "#fff7f6" }}>{error}</div>}
      {notice && <div role="status" style={{ ...cardStyle, color: "#166534", backgroundColor: "#f0fdf4" }}>{notice}</div>}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))", gap: 12 }}>
        {kpis.map((kpi) => (
          <div key={kpi.label} style={cardStyle}>
            <div style={{ fontSize: 13, color: "#334155", fontWeight: 600 }}>{kpi.label}</div>
            <div style={{ fontSize: 30, fontWeight: 800, color: kpi.color, marginTop: 7 }}>{kpi.value}</div>
          </div>
        ))}
      </div>

      {formOpen && (
        <form onSubmit={handleSubmit} style={{ ...cardStyle, display: "grid", gap: 13 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
            <h3 style={{ margin: 0 }}>{editingId === null ? "Thêm thiết bị iBeacon" : "Cập nhật thiết bị iBeacon"}</h3>
            <button type="button" style={secondaryButton} onClick={resetForm}>Đóng</button>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: 12 }}>
            <label style={labelStyle}>Mã iBeacon<input required style={inputStyle} value={form.beacon_code} placeholder="BC-006" onChange={(e) => setForm({ ...form, beacon_code: e.target.value })} /></label>
            <label style={labelStyle}>UUID<input required style={inputStyle} value={form.uuid} onChange={(e) => setForm({ ...form, uuid: e.target.value })} /></label>
            <label style={labelStyle}>Major<input required type="number" min="0" style={inputStyle} value={form.major} onChange={(e) => setForm({ ...form, major: e.target.value })} /></label>
            <label style={labelStyle}>Minor<input required type="number" min="0" style={inputStyle} value={form.minor} onChange={(e) => setForm({ ...form, minor: e.target.value })} /></label>
            <label style={labelStyle}>Khu vực<input required style={inputStyle} value={form.zone_name} onChange={(e) => setForm({ ...form, zone_name: e.target.value })} /></label>
            <label style={labelStyle}>TX Power (dBm)<input required type="number" style={inputStyle} value={form.tx_power} onChange={(e) => setForm({ ...form, tx_power: e.target.value })} /></label>
            <label style={labelStyle}>Khoảng cách kích hoạt (m)<input required type="number" min="0.1" step="0.1" style={inputStyle} value={form.trigger_distance} onChange={(e) => setForm({ ...form, trigger_distance: e.target.value })} /></label>
            <label style={labelStyle}>Mức pin (%)<input required type="number" min="0" max="100" style={inputStyle} value={form.battery_level} onChange={(e) => setForm({ ...form, battery_level: e.target.value })} /></label>
            <label style={labelStyle}>Tọa độ X<input required type="number" step="any" style={inputStyle} value={form.map_x} onChange={(e) => setForm({ ...form, map_x: e.target.value })} /></label>
            <label style={labelStyle}>Tọa độ Y<input required type="number" step="any" style={inputStyle} value={form.map_y} onChange={(e) => setForm({ ...form, map_y: e.target.value })} /></label>
            <label style={labelStyle}>Hiện vật được gán
              <select style={inputStyle} value={form.artifact_id} onChange={(e) => setForm({ ...form, artifact_id: e.target.value })}>
                <option value="">{editingId === null ? "Chưa gán hiện vật" : "Giữ nguyên hiện vật hiện tại"}</option>
                {artifacts.map((artifact) => <option key={artifact.artifact_id} value={artifact.artifact_id}>{artifact.artifact_code} — {artifact.name_vi}</option>)}
              </select>
            </label>
            <label style={labelStyle}>Trạng thái
              <select style={inputStyle} value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as BeaconStatus })}>
                <option value="ACTIVE">Đang hoạt động</option><option value="LOW_BATTERY">Pin yếu</option><option value="LOST_SIGNAL">Mất tín hiệu</option>
              </select>
            </label>
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <button type="submit" disabled={saving} style={{ ...primaryButton, opacity: saving ? 0.65 : 1 }}>{saving ? "Đang lưu..." : "Lưu thiết bị"}</button>
            <button type="button" style={secondaryButton} onClick={resetForm}>Hủy</button>
          </div>
        </form>
      )}

      <section style={cardStyle}>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginBottom: 14 }}>
          <input aria-label="Tìm kiếm iBeacon" placeholder="Tìm mã, UUID, hiện vật, khu vực..." value={search} onChange={(e) => setSearch(e.target.value)} style={{ ...inputStyle, marginTop: 0, maxWidth: 360 }} />
          <select aria-label="Lọc trạng thái" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} style={{ ...inputStyle, marginTop: 0, width: 205 }}>
            <option value="">Tất cả trạng thái</option><option value="ACTIVE">Đang hoạt động</option><option value="LOW_BATTERY">Pin yếu</option><option value="LOST_SIGNAL">Mất tín hiệu</option>
          </select>
          <button type="button" style={secondaryButton} onClick={() => void loadData()}>Làm mới</button>
        </div>
        {loading ? <p>Đang tải danh sách thiết bị...</p> : (
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13, minWidth: 920 }}>
              <thead><tr>{["Mã thiết bị", "Hiện vật", "Khu vực", "Major / Minor", "Pin", "Khoảng cách", "Trạng thái", "Thao tác"].map((heading) => <th key={heading} style={{ ...tableCellStyle, textAlign: "left", color: "#334155", fontWeight: 800, borderBottom: "2px solid #d4cdc3" }}>{heading}</th>)}</tr></thead>
              <tbody>
                {filteredItems.map((beacon) => (
                  <tr key={beacon.beacon_id}>
                    <td style={tableCellStyle}><strong>{beacon.beacon_code}</strong><div style={{ color: "#64748b", fontSize: 11, marginTop: 3 }}>{beacon.uuid}</div></td>
                    <td style={tableCellStyle}>{beacon.artifact_code ?? "—"}<div>{beacon.artifact_name_vi ?? "Chưa gán"}</div></td>
                    <td style={tableCellStyle}>{beacon.zone_name}</td>
                    <td style={tableCellStyle}>{beacon.major} / {beacon.minor}</td>
                    <td style={tableCellStyle}><strong style={{ color: beacon.battery_level <= 20 ? "#b45309" : "#166534" }}>{beacon.battery_level}%</strong></td>
                    <td style={tableCellStyle}>{beacon.trigger_distance} m</td>
                    <td style={tableCellStyle}><span style={{ ...statusStyle(beacon.status), display: "inline-block", padding: "4px 8px", borderRadius: 6, fontWeight: 800, whiteSpace: "nowrap" }}>{statusLabel(beacon.status)}</span></td>
                    <td style={{ ...tableCellStyle, whiteSpace: "nowrap" }}>
                      <button type="button" style={{ ...primaryButton, padding: "6px 9px", marginRight: 5 }} onClick={() => openEditForm(beacon)}>Sửa</button>
                      <button type="button" style={{ ...secondaryButton, padding: "6px 9px" }} onClick={() => void deleteBeacon(beacon)}>Xóa</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filteredItems.length === 0 && <p style={{ padding: 14, color: "#64748b" }}>Không tìm thấy thiết bị phù hợp.</p>}
          </div>
        )}
      </section>
    </div>
  );
}
