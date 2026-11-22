// FILE: admin-web/src/pages/W6_FloorMap.tsx — Bản đồ khu di tích: đặt vị trí iBeacon (Figma W6)
import { useEffect, useRef, useState, type ChangeEvent, type CSSProperties, type KeyboardEvent, type PointerEvent, type ReactNode } from "react";
import { Check, MapPin, MousePointer2, Upload } from "lucide-react";
import { API_BASE, apiFetch, uploadMedia } from "../api";
import { Banner, BRAND_RED } from "../ui";

type PinStatus = "ACTIVE" | "LOW_BATTERY" | "LOST_SIGNAL";

interface Pin {
  beacon_id: number;
  beacon_code: string;
  trigger_distance: number;
  beacon_status: PinStatus;
  map_x: number | null;
  map_y: number | null;
  zone_name: string;
  artifact_id: number | null;
  artifact_code: string | null;
  artifact_name: string | null;
}

interface SiteInfo {
  site_id: number;
  name_vi: string;
  floor_name: string | null;
  map_image_url: string | null;
  is_active: boolean;
}

interface MapData {
  site: { site_id: number; floor_name: string | null; map_image_url: string | null };
  pins: Pin[];
}

interface ArtifactOption {
  artifact_id: number;
  artifact_code: string;
  name_vi: string;
  beacon_code?: string | null;
}

interface Point {
  x: number;
  y: number;
}

const PX_PER_METER = 26; // khớp thước tỷ lệ "5 m = 130 px" trên ảnh sơ đồ mẫu
const DEFAULT_SIZE = { w: 900, h: 600 };
const PIN_ICON: Record<PinStatus, string> = { ACTIVE: "/pin_ibeacon_xanh_la.svg", LOW_BATTERY: "/pin_ibeacon_cam.svg", LOST_SIGNAL: "/pin_ibeacon_do.svg" };
const PIN_COLOR: Record<PinStatus, string> = { ACTIVE: "#16a34a", LOW_BATTERY: "#d97706", LOST_SIGNAL: "#b91c1c" };
const ZOOMS = [0.5, 0.75, 1, 1.25, 1.5, 2];

const clamp = (v: number, lo: number, hi: number) => Math.max(lo, Math.min(hi, v));
const fileOf = (url: string | null) => (url ? decodeURIComponent(url.split("/").pop() ?? url) : "chưa có ảnh");

export default function W6_FloorMap() {
  const [sites, setSites] = useState<SiteInfo[]>([]);
  const [siteId, setSiteId] = useState<number | null>(null);
  const [map, setMap] = useState<MapData | null>(null);
  const [artifacts, setArtifacts] = useState<ArtifactOption[]>([]);
  const [size, setSize] = useState(DEFAULT_SIZE);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [edits, setEdits] = useState<Record<number, Point>>({});
  const [artifactDraft, setArtifactDraft] = useState<Record<number, string>>({}); // hiện vật đang chọn nhưng chưa lưu, theo beacon
  const [zoom, setZoom] = useState(1);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<{ tone: "error" | "ok"; text: string } | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const mapRef = useRef<HTMLDivElement>(null);
  const dragging = useRef<number | null>(null);
  const moved = useRef(false);

  // Danh sách khu di tích + hiện vật (một lần)
  useEffect(() => {
    let active = true;
    Promise.all([apiFetch<SiteInfo[]>("/api/sites"), apiFetch<ArtifactOption[]>("/api/artifacts?lang=vi")])
      .then(([s, a]) => {
        if (!active) return;
        const live = s.filter((x) => x.is_active);
        setSites(live);
        setArtifacts(a);
        setSiteId((prev) => prev ?? live[0]?.site_id ?? null);
      })
      .catch((e: Error) => active && setNotice({ tone: "error", text: e.message }));
    return () => {
      active = false;
    };
  }, [reloadKey]);

  // Sơ đồ + pin của khu di tích đang chọn
  useEffect(() => {
    if (siteId === null) return;
    let active = true;
    apiFetch<MapData>(`/api/sites/map?site_id=${siteId}&lang=vi`)
      .then((m) => {
        if (!active) return;
        setMap(m);
        setSelectedId((prev) => (m.pins.some((p) => p.beacon_id === prev) ? prev : m.pins[0]?.beacon_id ?? null));
      })
      .catch((e: Error) => active && setNotice({ tone: "error", text: e.message }))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [siteId, reloadKey]);

  const pins = map?.pins ?? [];
  const selected = pins.find((p) => p.beacon_id === selectedId) ?? null;
  const posOf = (p: Pin): Point | null => edits[p.beacon_id] ?? (p.map_x === null || p.map_y === null ? null : { x: p.map_x, y: p.map_y });
  const selPos = selected ? posOf(selected) : null;
  const placedCount = pins.filter((p) => posOf(p) !== null).length;
  const dirtyIds = Object.keys(edits).map(Number);
  const mapUrl = map?.site.map_image_url ? (map.site.map_image_url.startsWith("http") ? map.site.map_image_url : API_BASE + map.site.map_image_url) : "";
  const site = sites.find((s) => s.site_id === siteId);

  const select = (pin: Pin) => {
    setSelectedId(pin.beacon_id);
    setNotice(null);
  };

  const toImage = (clientX: number, clientY: number): Point | null => {
    const r = mapRef.current?.getBoundingClientRect();
    if (!r || r.width === 0) return null;
    return { x: clamp(Math.round(((clientX - r.left) / r.width) * size.w), 0, size.w), y: clamp(Math.round(((clientY - r.top) / r.height) * size.h), 0, size.h) };
  };

  const setPos = (id: number, p: Point) => setEdits((e) => ({ ...e, [id]: p }));

  const onMapClick = (e: PointerEvent<HTMLDivElement>) => {
    if (!selected || moved.current) return;
    const p = toImage(e.clientX, e.clientY);
    if (p) setPos(selected.beacon_id, p);
  };

  const onPinDown = (pin: Pin) => (e: PointerEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    e.currentTarget.setPointerCapture(e.pointerId);
    dragging.current = pin.beacon_id;
    moved.current = false;
    if (pin.beacon_id !== selectedId) select(pin);
  };
  const onPinMove = (pin: Pin) => (e: PointerEvent<HTMLButtonElement>) => {
    if (dragging.current !== pin.beacon_id) return;
    const p = toImage(e.clientX, e.clientY);
    if (p) {
      moved.current = true;
      setPos(pin.beacon_id, p);
    }
  };
  const onPinUp = () => {
    dragging.current = null;
    window.setTimeout(() => (moved.current = false), 0);
  };
  const onPinKey = (pin: Pin) => (e: KeyboardEvent<HTMLButtonElement>) => {
    const step = e.shiftKey ? 20 : 5;
    const d: Record<string, Point> = { ArrowLeft: { x: -step, y: 0 }, ArrowRight: { x: step, y: 0 }, ArrowUp: { x: 0, y: -step }, ArrowDown: { x: 0, y: step } };
    const move = d[e.key];
    const cur = posOf(pin);
    if (!move || !cur) return;
    e.preventDefault();
    setPos(pin.beacon_id, { x: clamp(cur.x + move.x, 0, size.w), y: clamp(cur.y + move.y, 0, size.h) });
  };

  const setCoord = (axis: "x" | "y", raw: string) => {
    if (!selected) return;
    const base = selPos ?? { x: 0, y: 0 };
    const v = clamp(Math.round(Number(raw) || 0), 0, axis === "x" ? size.w : size.h);
    setPos(selected.beacon_id, { ...base, [axis]: v });
  };

  const persist = async (pin: Pin, p: Point, artifactId?: number) =>
    apiFetch(`/api/beacons/${pin.beacon_id}/coordinates`, {
      method: "PUT",
      body: JSON.stringify({ map_x: p.x, map_y: p.y, trigger_distance: pin.trigger_distance, ...(artifactId ? { artifact_id: artifactId } : {}) }),
    });

  const savedArtifact = selected && selected.artifact_id !== null ? String(selected.artifact_id) : "";
  const artifactSel = selected ? artifactDraft[selected.beacon_id] ?? savedArtifact : "";
  const artifactChanged = selected !== null && artifactSel !== savedArtifact;

  const run = async (jobs: Array<() => Promise<unknown>>, okText: string, doneIds: number[]) => {
    setSaving(true);
    setNotice(null);
    try {
      for (const job of jobs) await job();
      setEdits((e) => Object.fromEntries(Object.entries(e).filter(([id]) => !doneIds.includes(Number(id)))));
      setArtifactDraft((d) => Object.fromEntries(Object.entries(d).filter(([id]) => !doneIds.includes(Number(id)))));
      setReloadKey((n) => n + 1);
      setNotice({ tone: "ok", text: okText });
    } catch (e) {
      setNotice({ tone: "error", text: e instanceof Error ? e.message : "Không lưu được vị trí." });
    } finally {
      setSaving(false);
    }
  };

  const updateSelected = () => {
    if (!selected || !selPos) return;
    void run([() => persist(selected, selPos, artifactChanged ? Number(artifactSel) : undefined)], `Đã cập nhật ${selected.beacon_code} và đồng bộ vị trí hiện vật.`, [selected.beacon_id]);
  };

  const saveAll = () => {
    const jobs = pins.filter((p) => edits[p.beacon_id]).map((p) => () => persist(p, edits[p.beacon_id], p.beacon_id === selectedId && artifactChanged ? Number(artifactSel) : undefined));
    if (jobs.length === 0) return setNotice({ tone: "ok", text: "Không có thay đổi vị trí nào cần lưu." });
    void run(jobs, `Đã lưu vị trí ${jobs.length} iBeacon.`, dirtyIds);
  };

  const uploadPlan = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file || siteId === null) return;
    if (!/\.(png|jpe?g|webp)$/i.test(file.name)) return setNotice({ tone: "error", text: "Sơ đồ chỉ nhận ảnh PNG, JPG hoặc WebP." });
    setSaving(true);
    setNotice(null);
    try {
      const url = await uploadMedia(file);
      await apiFetch(`/api/sites/${siteId}/map`, { method: "PUT", body: JSON.stringify({ map_image_url: url }) });
      setReloadKey((n) => n + 1);
      setNotice({ tone: "ok", text: "Đã cập nhật ảnh sơ đồ mặt bằng." });
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : "Không tải được ảnh sơ đồ." });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <h2 style={{ fontSize: 24, fontWeight: 800, color: "#0f172a" }}>Bản đồ khu di tích</h2>
          <p style={{ fontSize: 13, color: "#475569", marginTop: 2 }}>Bố trí hiện vật và điểm phát iBeacon trên sơ đồ mặt bằng.</p>
        </div>
        <label style={{ ...primaryBtnStyle, display: "inline-flex", alignItems: "center", gap: 8 }}>
          <Upload size={16} /> Tải ảnh sơ đồ mặt bằng
          <input type="file" accept=".png,.jpg,.jpeg,.webp" hidden onChange={(e) => void uploadPlan(e)} disabled={saving} />
        </label>
      </div>

      {notice ? <Banner tone={notice.tone} text={notice.text} onClose={() => setNotice(null)} /> : null}

      <div style={{ ...cardStyle, display: "flex", alignItems: "center", gap: 12, padding: "12px 16px" }}>
        <select value={siteId ?? ""} onChange={(e) => { setLoading(true); setEdits({}); setArtifactDraft({}); setSiteId(Number(e.target.value)); }} aria-label="Khu di tích" style={{ ...selectStyle, minWidth: 230 }}>
          {sites.map((s) => <option key={s.site_id} value={s.site_id}>{s.name_vi}</option>)}
        </select>
        <select value="floor" disabled aria-label="Tầng" style={{ ...selectStyle, minWidth: 200 }}>
          <option value="floor">{map?.site.floor_name ?? site?.floor_name ?? "—"}</option>
        </select>
        <span style={{ marginLeft: "auto", fontSize: 12, fontWeight: 600, color: "#475569", backgroundColor: "#f1f5f9", padding: "5px 10px", borderRadius: 6 }}>{placedCount} điểm đã đặt</span>
        <button type="button" style={{ ...outlineBtnStyle, opacity: dirtyIds.length ? 1 : 0.7 }} disabled={saving} onClick={saveAll}>
          <Check size={16} /> Lưu vị trí{dirtyIds.length ? ` (${dirtyIds.length})` : ""}
        </button>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1.85fr 0.8fr", gap: 18, alignItems: "start" }}>
        {/* ---------- SƠ ĐỒ ---------- */}
        <div style={cardStyle}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
            <div>
              <h3 style={cardTitleStyle}>Sơ đồ mặt bằng · {map?.site.floor_name ?? ""}</h3>
              <p style={cardSubStyle}>{fileOf(map?.site.map_image_url ?? null)} · Sơ đồ minh họa</p>
            </div>
            <span style={{ fontSize: 12, fontWeight: 600, color: "#b91c1c", backgroundColor: "#fdecea", padding: "5px 10px", borderRadius: 6 }}>Chế độ đặt vị trí</span>
          </div>

          <div style={{ position: "relative", marginTop: 12, border: "1px solid #d9cdb4", borderRadius: 8, overflow: "auto", maxHeight: 640, backgroundColor: "#f8f4ec" }}>
            <div
              ref={mapRef}
              onPointerUp={onMapClick}
              style={{ position: "relative", width: `${zoom * 100}%`, aspectRatio: `${size.w} / ${size.h}`, cursor: selected ? "crosshair" : "default", touchAction: "none", userSelect: "none" }}
            >
              {mapUrl ? (
                <img
                  src={mapUrl}
                  alt="Sơ đồ mặt bằng khu di tích"
                  draggable={false}
                  onLoad={(e) => setSize({ w: e.currentTarget.naturalWidth || DEFAULT_SIZE.w, h: e.currentTarget.naturalHeight || DEFAULT_SIZE.h })}
                  style={{ position: "absolute", inset: 0, width: "100%", height: "100%", pointerEvents: "none" }}
                />
              ) : (
                <div style={{ padding: 24, color: "#64748b" }}>{loading ? "Đang tải sơ đồ..." : "Chưa có ảnh sơ đồ. Bấm “Tải ảnh sơ đồ mặt bằng”."}</div>
              )}

              {selected && selPos ? (
                <div
                  aria-hidden
                  style={{
                    position: "absolute", left: `${(selPos.x / size.w) * 100}%`, top: `${(selPos.y / size.h) * 100}%`, transform: "translate(-50%, -50%)",
                    width: `${((selected.trigger_distance * PX_PER_METER * 2) / size.w) * 100}%`, aspectRatio: "1 / 1", borderRadius: "50%",
                    border: "1.5px dashed #b91c1c", backgroundColor: "rgba(185,28,28,0.05)", pointerEvents: "none",
                  }}
                />
              ) : null}

              {pins.map((pin) => {
                const p = posOf(pin);
                if (!p) return null;
                const isSel = pin.beacon_id === selectedId;
                return (
                  <button
                    key={pin.beacon_id}
                    type="button"
                    aria-label={`${pin.beacon_code}, tọa độ ${p.x}, ${p.y}. Dùng phím mũi tên để di chuyển.`}
                    onPointerDown={onPinDown(pin)}
                    onPointerMove={onPinMove(pin)}
                    onPointerUp={onPinUp}
                    onKeyDown={onPinKey(pin)}
                    onClick={(e) => e.stopPropagation()}
                    style={{ position: "absolute", left: `${(p.x / size.w) * 100}%`, top: `${(p.y / size.h) * 100}%`, transform: "translate(-50%, -50%)", background: "none", border: "none", padding: 0, cursor: "grab", display: "flex", flexDirection: "column", alignItems: "center", zIndex: isSel ? 3 : 2 }}
                  >
                    <img src={PIN_ICON[pin.beacon_status]} alt="" draggable={false} style={{ width: 30, height: 30, filter: isSel ? "drop-shadow(0 0 0 #fff)" : "none", outline: isSel ? `2px solid ${PIN_COLOR[pin.beacon_status]}` : "none", outlineOffset: 2, borderRadius: "50%" }} />
                    <span style={{ marginTop: 3, fontSize: 10.5, fontWeight: 700, color: "#334155", backgroundColor: "#fff", border: "1px solid #cbd5e1", borderRadius: 4, padding: "0 5px" }}>{pin.beacon_code}</span>
                  </button>
                );
              })}

              {selected && selPos && selected.artifact_name ? (
                <div style={{ position: "absolute", left: `${(selPos.x / size.w) * 100}%`, top: `${(selPos.y / size.h) * 100}%`, transform: "translate(-50%, 34px)", zIndex: 4, pointerEvents: "none", backgroundColor: BRAND_RED, color: "#fff", fontSize: 11.5, fontWeight: 600, padding: "4px 10px", borderRadius: 4, whiteSpace: "nowrap" }}>
                  {selected.artifact_code} · {selected.artifact_name}
                </div>
              ) : null}
            </div>
          </div>

          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 12 }}>
            <div style={{ display: "flex", gap: 16, fontSize: 12, color: "#475569" }}>
              <Legend color="#16a34a" label="Hoạt động" />
              <Legend color="#d97706" label="Pin yếu" />
              <Legend color="#b91c1c" label="Mất tín hiệu" />
              <span>Vòng nét đứt: bán kính kích hoạt</span>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 4, border: "1px solid #cbd5e1", borderRadius: 8, padding: "2px 4px" }}>
              <button type="button" style={zoomBtnStyle} aria-label="Thu nhỏ" disabled={zoom <= ZOOMS[0]} onClick={() => setZoom(ZOOMS[Math.max(0, ZOOMS.indexOf(zoom) - 1)])}>−</button>
              <span style={{ fontSize: 12, fontWeight: 600, minWidth: 40, textAlign: "center" }}>{Math.round(zoom * 100)}%</span>
              <button type="button" style={zoomBtnStyle} aria-label="Phóng to" disabled={zoom >= ZOOMS[ZOOMS.length - 1]} onClick={() => setZoom(ZOOMS[Math.min(ZOOMS.length - 1, ZOOMS.indexOf(zoom) + 1)])}>+</button>
            </div>
          </div>
        </div>

        {/* ---------- PANEL PHẢI ---------- */}
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <div style={cardStyle}>
            <h3 style={cardTitleStyle}>Điểm đang chọn</h3>
            {selected ? (
              <>
                <p style={cardSubStyle}>{selected.beacon_code} · {selected.zone_name}</p>
                <Field label="Thiết bị iBeacon">
                  <select value={selected.beacon_id} onChange={(e) => { const p = pins.find((x) => x.beacon_id === Number(e.target.value)); if (p) select(p); }} style={inputStyle}>
                    {pins.map((p) => <option key={p.beacon_id} value={p.beacon_id}>{p.beacon_code}</option>)}
                  </select>
                </Field>
                <Field label="Hiện vật được gán">
                  <select value={artifactSel} onChange={(e) => setArtifactDraft((d) => ({ ...d, [selected.beacon_id]: e.target.value }))} style={inputStyle}>
                    {selected.artifact_id === null ? <option value="">— Chưa gán —</option> : null}
                    {artifacts.map((a) => <option key={a.artifact_id} value={a.artifact_id}>{a.artifact_code} · {a.name_vi}</option>)}
                  </select>
                </Field>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                  <Field label="Tọa độ X (px)"><input type="number" min={0} max={size.w} value={selPos?.x ?? ""} onChange={(e) => setCoord("x", e.target.value)} style={inputStyle} /></Field>
                  <Field label="Tọa độ Y (px)"><input type="number" min={0} max={size.h} value={selPos?.y ?? ""} onChange={(e) => setCoord("y", e.target.value)} style={inputStyle} /></Field>
                </div>
                <p style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 12.5, color: selPos ? "#b91c1c" : "#64748b", marginTop: 12 }}>
                  <MapPin size={14} /> {selPos ? `Đã đặt · ${selected.zone_name}` : "Chưa đặt vị trí — click lên sơ đồ"}
                  {edits[selected.beacon_id] ? <b style={{ color: "#b45309" }}> (chưa lưu)</b> : null}
                </p>
                <p style={{ fontSize: 12, color: "#64748b", lineHeight: 1.55, marginTop: 8 }}>
                  Bán kính kích hoạt: {String(selected.trigger_distance).replace(".", ",")} m (đổi ở màn Thiết bị iBeacon).<br />
                  Gốc tọa độ (0, 0) ở góc trên trái sơ đồ.
                </p>
                <button type="button" style={{ ...primaryBtnStyle, marginTop: 14 }} disabled={saving || !selPos || (!edits[selected.beacon_id] && !artifactChanged)} onClick={updateSelected}>
                  {saving ? "Đang lưu..." : "Cập nhật tọa độ"}
                </button>
              </>
            ) : (
              <p style={{ ...cardSubStyle, marginTop: 10 }}>{loading ? "Đang tải..." : "Khu di tích này chưa có iBeacon. Thêm thiết bị ở màn Thiết bị iBeacon."}</p>
            )}
          </div>

          <div style={cardStyle}>
            <h3 style={cardTitleStyle}>Đặt vị trí trên sơ đồ</h3>
            <p style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13.5, color: "#0f172a", marginTop: 12 }}>
              <MousePointer2 size={16} color={BRAND_RED} /> Click để đặt / Kéo để di chuyển
            </p>
            <p style={{ fontSize: 12.5, color: "#475569", lineHeight: 1.55, marginTop: 8 }}>
              Chọn một iBeacon, sau đó click lên sơ đồ hoặc kéo biểu tượng đến vị trí mong muốn. Có thể dùng phím mũi tên để nhích từng 5 px (giữ Shift: 20 px).
            </p>
            <p style={{ fontSize: 12, color: "#b45309", marginTop: 8 }}>Thay đổi chỉ có hiệu lực sau khi bấm “Cập nhật tọa độ” hoặc “Lưu vị trí”.</p>
          </div>
        </div>
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

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
      <span style={{ width: 8, height: 8, borderRadius: "50%", backgroundColor: color }} /> {label}
    </span>
  );
}

const cardStyle: CSSProperties = { backgroundColor: "#fff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "18px 20px" };
const cardTitleStyle: CSSProperties = { fontSize: 16, fontWeight: 600, color: "#0f172a" };
const cardSubStyle: CSSProperties = { fontSize: 12.5, color: "#475569", marginTop: 2 };
const selectStyle: CSSProperties = { height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 13.5, color: "#0f172a", backgroundColor: "#fff" };
const inputStyle: CSSProperties = { width: "100%", height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 13.5, color: "#0f172a", backgroundColor: "#fff" };
const primaryBtnStyle: CSSProperties = { height: 40, padding: "0 18px", border: "none", borderRadius: 8, backgroundColor: BRAND_RED, color: "#fff", fontSize: 13.5, fontWeight: 700, cursor: "pointer" };
const outlineBtnStyle: CSSProperties = { display: "inline-flex", alignItems: "center", gap: 8, height: 40, padding: "0 16px", border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", fontSize: 13.5, fontWeight: 600, color: "#0f172a", cursor: "pointer" };
const zoomBtnStyle: CSSProperties = { width: 28, height: 28, border: "none", background: "none", fontSize: 16, cursor: "pointer", color: "#0f172a" };
