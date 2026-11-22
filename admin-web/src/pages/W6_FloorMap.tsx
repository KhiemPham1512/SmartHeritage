import { useCallback, useEffect, useState, type CSSProperties, type MouseEvent } from "react";
import { API_BASE, apiFetch } from "../api";

type BeaconPin = {
  beacon_id: number;
  beacon_code: string;
  major: number;
  minor: number;
  trigger_distance: number;
  battery_level: number;
  beacon_status: "ACTIVE" | "LOW_BATTERY" | "LOST_SIGNAL";
  map_x: number | null;
  map_y: number | null;
  zone_name: string;
  artifact_id: number | null;
  artifact_code: string | null;
  artifact_name: string | null;
  summary?: string | null;
  image_url?: string | null;
};
type Site = {
  site_id: number;
  site_name: string;
  name_vi: string;
  name_en: string;
  floor_name: string;
  address: string;
  map_image_url: string;
};
type MapData = { site: Site; pins: BeaconPin[] };
type Artifact = { artifact_id: number; artifact_code: string; name_vi: string };
type ImageSize = { width: number; height: number };

const RED = "#80140e";
const cardStyle: CSSProperties = { backgroundColor: "#ffffff", border: "1.5px solid #d4cdc3", borderRadius: 11, padding: "16px 20px" };
const primaryButton: CSSProperties = { backgroundColor: RED, color: "#ffffff", border: 0, borderRadius: 7, padding: "9px 14px", cursor: "pointer", fontWeight: 700 };
const secondaryButton: CSSProperties = { ...primaryButton, backgroundColor: "#64748b" };
const inputStyle: CSSProperties = { width: "100%", boxSizing: "border-box", padding: "9px 10px", border: "1px solid #cbd5e1", borderRadius: 6, marginTop: 5, color: "#0f172a", backgroundColor: "#ffffff" };
const labelStyle: CSSProperties = { display: "block", color: "#334155", fontSize: 13, fontWeight: 700 };

function pinColor(status: BeaconPin["beacon_status"]) {
  if (status === "ACTIVE") return "#15803d";
  if (status === "LOW_BATTERY") return "#d97706";
  return RED;
}

function statusText(status: BeaconPin["beacon_status"]) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "LOW_BATTERY") return "Pin yếu";
  return "Mất tín hiệu";
}

export default function W6_FloorMap() {
  const [mapData, setMapData] = useState<MapData | null>(null);
  const [artifacts, setArtifacts] = useState<Artifact[]>([]);
  const [selectedPin, setSelectedPin] = useState<BeaconPin | null>(null);
  const [coordinateX, setCoordinateX] = useState("350");
  const [coordinateY, setCoordinateY] = useState("250");
  const [triggerDistance, setTriggerDistance] = useState("2");
  const [artifactId, setArtifactId] = useState("");
  const [imageSize, setImageSize] = useState<ImageSize>({ width: 700, height: 450 });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [map, artifactList] = await Promise.all([
        apiFetch<MapData>("/api/sites/map?site_id=1&lang=vi"),
        apiFetch<Artifact[]>("/api/artifacts?lang=vi"),
      ]);
      setMapData(map);
      setArtifacts(artifactList);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không tải được dữ liệu bản đồ.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { queueMicrotask(() => void loadData()); }, [loadData]); // Tải sau microtask: tránh setState đồng bộ trong effect (react-hooks/set-state-in-effect)

  const choosePin = (pin: BeaconPin) => {
    setSelectedPin(pin);
    setCoordinateX(String(pin.map_x ?? 0));
    setCoordinateY(String(pin.map_y ?? 0));
    setTriggerDistance(String(pin.trigger_distance ?? 2));
    setArtifactId(pin.artifact_id == null ? "" : String(pin.artifact_id));
    setNotice("");
    setError("");
  };

  const handleMapClick = (event: MouseEvent<HTMLDivElement>) => {
    if (!selectedPin) return;
    const bounds = event.currentTarget.getBoundingClientRect();
    const x = Math.max(0, Math.min(imageSize.width, Math.round(((event.clientX - bounds.left) / bounds.width) * imageSize.width)));
    const y = Math.max(0, Math.min(imageSize.height, Math.round(((event.clientY - bounds.top) / bounds.height) * imageSize.height)));
    setCoordinateX(String(x));
    setCoordinateY(String(y));
    setNotice("");
  };

  const saveCoordinates = async () => {
    if (!selectedPin) return;
    const x = Number(coordinateX);
    const y = Number(coordinateY);
    const distance = Number(triggerDistance);
    if (![x, y, distance].every(Number.isFinite) || x < 0 || y < 0 || distance <= 0) {
      setError("Tọa độ X/Y phải hợp lệ và khoảng cách kích hoạt phải lớn hơn 0."); return;
    }
    const payload: Record<string, unknown> = { map_x: x, map_y: y, trigger_distance: distance };
    // Backend giữ liên kết hiện vật cũ nếu artifact_id không được gửi.
    if (artifactId) payload.artifact_id = Number(artifactId);

    setSaving(true);
    setError(""); setNotice("");
    try {
      await apiFetch(`/api/beacons/${selectedPin.beacon_id}/coordinates`, { method: "PUT", body: JSON.stringify(payload) });
      // Tải lại sơ đồ và giữ thiết bị đang chọn để người quản trị tiếp tục chỉnh sửa.
      const refreshed = await apiFetch<MapData>("/api/sites/map?site_id=1&lang=vi");
      setMapData(refreshed);
      const updatedPin = refreshed.pins.find((pin) => pin.beacon_id === selectedPin.beacon_id);
      if (updatedPin) {
        setSelectedPin(updatedPin);
        setCoordinateX(String(updatedPin.map_x ?? x));
        setCoordinateY(String(updatedPin.map_y ?? y));
        setTriggerDistance(String(updatedPin.trigger_distance ?? distance));
        setArtifactId(updatedPin.artifact_id == null ? "" : String(updatedPin.artifact_id));
      }
      setNotice("Đã cập nhật tọa độ iBeacon và đồng bộ vị trí hiện vật.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể lưu tọa độ iBeacon.");
    } finally {
      setSaving(false);
    }
  };

  const mapImageUrl = mapData?.site.map_image_url
    ? (mapData.site.map_image_url.startsWith("http") ? mapData.site.map_image_url : `${API_BASE}${mapData.site.map_image_url}`)
    : "";

  return (
    <div style={{ display: "grid", gap: 16, color: "#0f172a" }}>
      <div>
        <h2 style={{ margin: "0 0 5px", fontWeight: 800 }}>Bản đồ vị trí iBeacon & khu di tích</h2>
        <p style={{ margin: 0, color: "#334155", fontWeight: 600 }}>Chọn một thiết bị, click lên sơ đồ để đặt tọa độ X/Y, sau đó lưu thay đổi.</p>
      </div>

      {error && <div role="alert" style={{ ...cardStyle, color: RED, backgroundColor: "#fff7f6" }}>{error}</div>}
      {notice && <div role="status" style={{ ...cardStyle, color: "#166534", backgroundColor: "#f0fdf4" }}>{notice}</div>}

      {loading ? <div style={cardStyle}>Đang tải sơ đồ mặt bằng...</div> : !mapData ? (
        <div style={cardStyle}>Không có dữ liệu bản đồ để hiển thị.</div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "minmax(0, 1.7fr) minmax(280px, 1fr)", gap: 16, alignItems: "start" }}>
          <section style={cardStyle}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 10, flexWrap: "wrap", marginBottom: 12 }}>
              <div>
                <h3 style={{ margin: "0 0 4px" }}>{mapData.site.site_name}</h3>
                <div style={{ color: "#334155", fontSize: 13, fontWeight: 600 }}>{mapData.site.floor_name} · {mapData.site.address}</div>
              </div>
              <span style={{ backgroundColor: "#fde8e6", border: "1.5px solid #eca9a4", color: RED, padding: "5px 9px", borderRadius: 6, fontWeight: 800, fontSize: 12 }}>{mapData.pins.length} iBeacon</span>
            </div>

            <div
              onClick={handleMapClick}
              role="button"
              tabIndex={selectedPin ? 0 : -1}
              aria-label="Sơ đồ mặt bằng. Chọn thiết bị rồi click để cập nhật tọa độ."
              onKeyDown={(event) => {
                if (!selectedPin || (event.key !== "Enter" && event.key !== " ")) return;
                event.preventDefault();
              }}
              style={{
                position: "relative", width: "100%", aspectRatio: `${imageSize.width} / ${imageSize.height}`,
                backgroundColor: "#f8f5ef", border: "1px solid #d4cdc3", borderRadius: 8,
                overflow: "hidden", cursor: selectedPin ? "crosshair" : "default",
              }}
            >
              {mapImageUrl ? (
                <img
                  src={mapImageUrl}
                  alt="Sơ đồ mặt bằng khu di tích"
                  onLoad={(event) => setImageSize({ width: event.currentTarget.naturalWidth || 700, height: event.currentTarget.naturalHeight || 450 })}
                  style={{ width: "100%", height: "100%", objectFit: "fill", display: "block", pointerEvents: "none" }}
                />
              ) : <div style={{ padding: 20, color: "#64748b" }}>Chưa có ảnh sơ đồ mặt bằng.</div>}

              {mapData.pins.map((pin) => {
                if (pin.map_x == null || pin.map_y == null) return null;
                return (
                  <button
                    key={pin.beacon_id}
                    type="button"
                    title={`${pin.beacon_code} — ${pin.artifact_name ?? "Chưa gán hiện vật"}`}
                    aria-label={`Chọn ${pin.beacon_code}`}
                    onClick={(event) => { event.stopPropagation(); choosePin(pin); }}
                    style={{
                      position: "absolute", left: `${(pin.map_x / imageSize.width) * 100}%`, top: `${(pin.map_y / imageSize.height) * 100}%`,
                      transform: "translate(-50%, -50%)", width: 30, height: 30, borderRadius: "50%",
                      border: selectedPin?.beacon_id === pin.beacon_id ? "3px solid #fbbf24" : "2px solid #ffffff",
                      backgroundColor: pinColor(pin.beacon_status), color: "#ffffff", fontSize: 10, fontWeight: 800,
                      boxShadow: "0 1px 5px #555", cursor: "pointer", padding: 0,
                    }}
                  >{pin.minor}</button>
                );
              })}

              {selectedPin && (
                <div
                  aria-hidden="true"
                  style={{ position: "absolute", left: `${(Number(coordinateX) / imageSize.width) * 100}%`, top: `${(Number(coordinateY) / imageSize.height) * 100}%`, transform: "translate(-50%, -50%)", pointerEvents: "none", width: 28, height: 28, border: `3px dashed ${RED}`, borderRadius: "50%" }}
                />
              )}
            </div>
            <p style={{ fontSize: 12, color: "#64748b", margin: "10px 0 0" }}>Chấm tròn là vị trí iBeacon. Chọn thiết bị từ bản đồ hoặc danh sách bên phải, sau đó click vị trí mới trên sơ đồ.</p>
            <div style={{ display: "flex", gap: 14, flexWrap: "wrap", marginTop: 12, fontSize: 12, color: "#334155", fontWeight: 600 }}>
              <span><span style={{ color: "#15803d" }}>●</span> Hoạt động</span><span><span style={{ color: "#d97706" }}>●</span> Pin yếu</span><span><span style={{ color: RED }}>●</span> Mất tín hiệu</span>
            </div>
          </section>

          <aside style={{ display: "grid", gap: 12 }}>
            <section style={cardStyle}>
              <h3 style={{ margin: "0 0 10px" }}>Danh sách iBeacon</h3>
              <div style={{ display: "grid", gap: 7, maxHeight: 300, overflowY: "auto" }}>
                {mapData.pins.map((pin) => (
                  <button
                    key={pin.beacon_id}
                    type="button"
                    onClick={() => choosePin(pin)}
                    style={{
                      textAlign: "left", padding: 10,
                      border: selectedPin?.beacon_id === pin.beacon_id ? `1.5px solid ${RED}` : "1px solid #e2e8f0",
                      borderRadius: 7, backgroundColor: selectedPin?.beacon_id === pin.beacon_id ? "#fde8e6" : "#ffffff",
                      cursor: "pointer", color: "#0f172a",
                    }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                      <strong>{pin.beacon_code}</strong><span style={{ color: pinColor(pin.beacon_status), fontSize: 11, fontWeight: 800 }}>{statusText(pin.beacon_status)}</span>
                    </div>
                    <div style={{ fontSize: 12, color: "#334155", marginTop: 3 }}>{pin.artifact_name ?? "Chưa gán hiện vật"} · {pin.zone_name}</div>
                    <div style={{ fontSize: 11, color: "#64748b", marginTop: 3 }}>X: {pin.map_x ?? "—"} · Y: {pin.map_y ?? "—"} · Pin: {pin.battery_level}%</div>
                  </button>
                ))}
                {mapData.pins.length === 0 && <p style={{ color: "#64748b", margin: 0 }}>Chưa có thiết bị iBeacon.</p>}
              </div>
            </section>

            <section style={cardStyle}>
              <h3 style={{ margin: "0 0 12px" }}>Cập nhật tọa độ</h3>
              {!selectedPin ? <p style={{ color: "#64748b", margin: 0 }}>Vui lòng chọn một iBeacon để chỉnh sửa vị trí.</p> : (
                <div style={{ display: "grid", gap: 10 }}>
                  <div style={{ color: RED, fontWeight: 800 }}>{selectedPin.beacon_code} — {selectedPin.artifact_name ?? "Chưa gán hiện vật"}</div>
                  <label style={labelStyle}>Tọa độ X<input type="number" min="0" step="any" value={coordinateX} onChange={(e) => setCoordinateX(e.target.value)} style={inputStyle} /></label>
                  <label style={labelStyle}>Tọa độ Y<input type="number" min="0" step="any" value={coordinateY} onChange={(e) => setCoordinateY(e.target.value)} style={inputStyle} /></label>
                  <label style={labelStyle}>Khoảng cách kích hoạt (m)<input type="number" min="0.1" step="0.1" value={triggerDistance} onChange={(e) => setTriggerDistance(e.target.value)} style={inputStyle} /></label>
                  <label style={labelStyle}>Gán hiện vật
                    <select value={artifactId} onChange={(e) => setArtifactId(e.target.value)} style={inputStyle}>
                      <option value="">Giữ nguyên hiện vật hiện tại</option>
                      {artifacts.map((artifact) => <option key={artifact.artifact_id} value={artifact.artifact_id}>{artifact.artifact_code} — {artifact.name_vi}</option>)}
                    </select>
                  </label>
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    <button type="button" disabled={saving} onClick={() => void saveCoordinates()} style={{ ...primaryButton, opacity: saving ? 0.65 : 1 }}>{saving ? "Đang lưu..." : "Lưu tọa độ"}</button>
                    <button type="button" disabled={saving} onClick={() => choosePin(selectedPin)} style={secondaryButton}>Khôi phục tọa độ cũ</button>
                  </div>
                </div>
              )}
            </section>
          </aside>
        </div>
      )}
    </div>
  );
}
