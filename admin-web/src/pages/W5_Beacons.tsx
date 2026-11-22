// FILE: admin-web/src/pages/W5_Beacons.tsx — Quản lý thiết bị iBeacon (Figma W5)
import { useEffect, useMemo, useState, type CSSProperties, type ReactNode } from "react";
import { Search } from "lucide-react";
import { apiFetch } from "../api";
import { Banner, BRAND_RED, MaskIcon, Pager } from "../ui";

type BeaconStatus = "ACTIVE" | "LOW_BATTERY" | "LOST_SIGNAL";

interface Beacon {
  beacon_id: number;
  beacon_code: string;
  artifact_id: number | null;
  artifact_code: string | null;
  artifact_name_vi: string | null;
  zone_name: string;
  uuid: string;
  major: number;
  minor: number;
  tx_power: number;
  trigger_distance: number;
  battery_level: number;
  status: BeaconStatus;
  last_seen_at: string | null;
}

interface ArtifactOption {
  artifact_id: number;
  artifact_code: string;
  name_vi: string;
  beacon_code?: string | null;
}

interface FormState {
  beacon_code: string;
  uuid: string;
  major: string;
  minor: string;
  trigger_distance: string;
  artifact_id: string;
  zone_name: string;
  tx_power: string;
  battery_level: string;
  status: BeaconStatus;
}

const PAGE_SIZE = 5;
const DEFAULT_UUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825";
const UUID_RE = /^[0-9A-Fa-f]{8}(-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}$/;
const RADIUS_OPTIONS = [1, 1.5, 2, 2.5, 3, 4, 5];
const STATUS_INFO: Record<BeaconStatus, { label: string; color: string; bg: string; connect: string }> = {
  ACTIVE: { label: "Đang hoạt động", color: "#15803d", bg: "#ecfdf3", connect: "Đang kết nối" },
  LOW_BATTERY: { label: "Pin yếu", color: "#b45309", bg: "#fff4e0", connect: "Pin yếu" },
  LOST_SIGNAL: { label: "Mất tín hiệu", color: "#b91c1c", bg: "#fdecea", connect: "Mất tín hiệu" },
};

const fmtRadius = (n: number) => `${String(n).replace(".", ",")} m`;
const pad2 = (n: number) => String(n).padStart(2, "0");

// last_seen_at là giờ máy chủ dạng "2026-09-30T17:30:12"
function fmtLastSeen(iso: string | null): [string, string] {
  if (!iso) return ["—", "Chưa nhận tín hiệu"];
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return [iso, ""];
  const date = `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`;
  const time = `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;
  if (Date.now() - d.getTime() > 24 * 3600 * 1000) return [`${date.slice(0, 5)} · ${time.slice(0, 5)}`, "Hơn 24 giờ trước"];
  return [time, date];
}

function nextBeaconDefaults(items: Beacon[]): FormState {
  const num = (s: string) => Number(/(\d+)$/.exec(s)?.[1] ?? 0);
  const code = `BC-${String(items.reduce((m, b) => Math.max(m, num(b.beacon_code)), 0) + 1).padStart(3, "0")}`;
  const minor = items.reduce((m, b) => Math.max(m, b.minor), 100) + 1;
  return {
    beacon_code: code, uuid: DEFAULT_UUID, major: "1", minor: String(minor), trigger_distance: "2", artifact_id: "",
    zone_name: "Tầng 1 - Phòng Đông Sơn", tx_power: "-59", battery_level: "100", status: "ACTIVE",
  };
}

function toForm(b: Beacon): FormState {
  return {
    beacon_code: b.beacon_code, uuid: b.uuid, major: String(b.major), minor: String(b.minor),
    trigger_distance: String(b.trigger_distance), artifact_id: b.artifact_id === null ? "" : String(b.artifact_id),
    zone_name: b.zone_name ?? "", tx_power: String(b.tx_power), battery_level: String(b.battery_level), status: b.status,
  };
}

export default function W5_Beacons() {
  const [items, setItems] = useState<Beacon[]>([]);
  const [artifacts, setArtifacts] = useState<ArtifactOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<{ tone: "error" | "ok"; text: string } | null>(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [page, setPage] = useState(1);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState<FormState | null>(null);
  const [advanced, setAdvanced] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    Promise.all([apiFetch<Beacon[]>("/api/beacons"), apiFetch<ArtifactOption[]>("/api/artifacts?lang=vi")])
      .then(([beacons, arts]) => {
        if (!active) return;
        setItems(beacons);
        setArtifacts(arts);
        // Chưa chọn gì → chọn thiết bị đầu tiên để form bên dưới luôn có dữ liệu (như Figma)
        setSelectedId((prev) => prev ?? beacons[0]?.beacon_id ?? null);
        setForm((prev) => prev ?? (beacons[0] ? toForm(beacons[0]) : null));
      })
      .catch((e: Error) => active && setNotice({ tone: "error", text: e.message }))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [reloadKey]);

  const reload = () => setReloadKey((n) => n + 1);

  const stats = useMemo(
    () => ({
      total: items.length,
      active: items.filter((b) => b.status === "ACTIVE").length,
      low: items.filter((b) => b.status === "LOW_BATTERY").length,
      lost: items.filter((b) => b.status === "LOST_SIGNAL").length,
    }),
    [items],
  );

  const filtered = useMemo(() => {
    const q = search.trim().toLocaleLowerCase("vi");
    return items.filter(
      (b) =>
        (!statusFilter || b.status === statusFilter) &&
        [b.beacon_code, b.uuid, b.zone_name, b.artifact_code ?? "", b.artifact_name_vi ?? ""].some((v) => v.toLocaleLowerCase("vi").includes(q)),
    );
  }, [items, search, statusFilter]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const rows = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const selected = items.find((b) => b.beacon_id === selectedId) ?? null;

  const select = (b: Beacon) => {
    setCreating(false);
    setSelectedId(b.beacon_id);
    setForm(toForm(b));
    setNotice(null);
  };

  const startCreate = () => {
    setCreating(true);
    setSelectedId(null);
    setForm(nextBeaconDefaults(items));
    setAdvanced(false);
    setNotice(null);
  };

  const cancel = () => {
    if (selected) return select(selected);
    setCreating(false);
    if (items[0]) select(items[0]);
  };

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((f) => (f ? { ...f, [key]: value } : f));

  const save = async () => {
    if (!form) return;
    const major = Number(form.major);
    const minor = Number(form.minor);
    const radius = Number(form.trigger_distance);
    const tx = Number(form.tx_power);
    const battery = Number(form.battery_level);
    const fail = (text: string) => setNotice({ tone: "error", text });
    if (!form.beacon_code.trim()) return fail("Nhập tên / mã beacon.");
    if (!UUID_RE.test(form.uuid.trim())) return fail("UUID không hợp lệ (dạng XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX).");
    if (!Number.isInteger(major) || major < 0 || major > 65535) return fail("Major phải là số nguyên 0–65535.");
    if (!Number.isInteger(minor) || minor < 0 || minor > 65535) return fail("Minor phải là số nguyên 0–65535.");
    if (!(radius > 0 && radius <= 50)) return fail("Bán kính kích hoạt phải lớn hơn 0 và không quá 50 m.");
    if (!Number.isInteger(tx) || tx > 0 || tx < -100) return fail("Công suất đo ở 1 m (TxPower) phải là số âm, từ -100 đến 0.");
    if (!Number.isInteger(battery) || battery < 0 || battery > 100) return fail("Pin phải từ 0 đến 100%.");

    const payload = {
      beacon_code: form.beacon_code.trim(), uuid: form.uuid.trim().toUpperCase(), major, minor,
      trigger_distance: radius, zone_name: form.zone_name.trim() || "Chưa đặt khu vực", tx_power: tx,
      battery_level: battery, status: form.status, artifact_id: form.artifact_id ? Number(form.artifact_id) : null,
    };
    setSaving(true);
    setNotice(null);
    try {
      if (creating) {
        const created = await apiFetch<{ beacon_id: number }>("/api/beacons", { method: "POST", body: JSON.stringify(payload) });
        setCreating(false);
        setSelectedId(created.beacon_id);
        setNotice({ tone: "ok", text: `Đã thêm thiết bị ${payload.beacon_code}.` });
      } else if (selected) {
        await apiFetch(`/api/beacons/${selected.beacon_id}`, { method: "PUT", body: JSON.stringify(payload) });
        setNotice({ tone: "ok", text: `Đã lưu thiết bị ${payload.beacon_code}.` });
      }
      reload();
    } catch (e) {
      fail(e instanceof Error ? e.message : "Không lưu được thiết bị.");
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    if (!selected || !window.confirm(`Xóa thiết bị ${selected.beacon_code}? Hiện vật đang gắn sẽ không còn beacon.`)) return;
    setSaving(true);
    try {
      await apiFetch(`/api/beacons/${selected.beacon_id}`, { method: "DELETE" });
      setNotice({ tone: "ok", text: `Đã xóa thiết bị ${selected.beacon_code}.` });
      setSelectedId(null);
      setForm(null);
      reload();
    } catch (e) {
      setNotice({ tone: "error", text: e instanceof Error ? e.message : "Không xóa được thiết bị." });
    } finally {
      setSaving(false);
    }
  };

  const info = STATUS_INFO[(creating ? form?.status : selected?.status) ?? "ACTIVE"];
  const radiusOptions = form && !RADIUS_OPTIONS.includes(Number(form.trigger_distance)) ? [...RADIUS_OPTIONS, Number(form.trigger_distance)].sort((a, b) => a - b) : RADIUS_OPTIONS;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <h2 style={{ fontSize: 24, fontWeight: 800, color: "#0f172a" }}>Quản lý thiết bị iBeacon</h2>
          <p style={{ fontSize: 13, color: "#475569", marginTop: 2 }}>Kết nối, gán hiện vật và theo dõi tình trạng thiết bị trong khu trưng bày.</p>
        </div>
        <button type="button" style={primaryBtnStyle} onClick={startCreate}>+ Thêm iBeacon</button>
      </div>

      {notice ? <Banner tone={notice.tone} text={notice.text} onClose={() => setNotice(null)} /> : null}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 14 }}>
        <Kpi icon="/tb_tong_thiet_bi.svg" color="#1f2937" value={stats.total} label="Tổng thiết bị" />
        <Kpi icon="/tb_dang_hoat_dong.svg" color="#15803d" value={stats.active} label="Đang hoạt động" />
        <Kpi icon="/tb_pin_yeu.svg" color="#b45309" value={stats.low} label="Pin yếu" />
        <Kpi icon="/tb_mat_tin_hieu.svg" color="#b91c1c" value={stats.lost} label="Mất tín hiệu" />
      </div>

      <div style={cardStyle}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 12, flexWrap: "wrap" }}>
          <div>
            <h3 style={cardTitleStyle}>Danh sách iBeacon</h3>
            <p style={cardSubStyle}>Chọn thiết bị để chỉnh sửa thông số bên dưới.</p>
          </div>
          <div style={{ display: "flex", gap: 10 }}>
            <label style={{ ...fieldBoxStyle, width: 240 }}>
              <Search size={16} color="#64748b" />
              <input value={search} onChange={(e) => { setSearch(e.target.value); setPage(1); }} placeholder="Tìm mã hoặc tên Beacon..." style={{ border: "none", outline: "none", flex: 1, fontSize: 13.5, background: "transparent" }} />
            </label>
            <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(1); }} aria-label="Lọc trạng thái" style={{ ...fieldBoxStyle, cursor: "pointer", fontSize: 13.5 }}>
              <option value="">Tất cả trạng thái</option>
              {Object.entries(STATUS_INFO).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
            </select>
          </div>
        </div>

        <div style={{ overflowX: "auto", marginTop: 14 }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
            <thead>
              <tr style={{ backgroundColor: "#f1f5f9" }}>
                {["Tên / Mã Beacon", "UUID", "Major", "Minor", "Hiện vật được gán", "Kích hoạt", "Trạng thái", "Tín hiệu gần nhất"].map((h) => <th key={h} style={thStyle}>{h}</th>)}
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr><td colSpan={8} style={{ padding: 26, textAlign: "center", color: "#64748b" }}>{loading ? "Đang tải..." : "Không có thiết bị phù hợp."}</td></tr>
              ) : rows.map((b) => {
                const st = STATUS_INFO[b.status];
                const [t1, t2] = fmtLastSeen(b.last_seen_at);
                const isSel = !creating && b.beacon_id === selectedId;
                return (
                  <tr key={b.beacon_id} onClick={() => select(b)} style={{ cursor: "pointer", borderBottom: "1px solid #e2e8f0", backgroundColor: isSel ? "#fdeeed" : "transparent" }}>
                    <td style={tdStyle}>
                      <div style={{ fontWeight: 600, color: isSel ? BRAND_RED : "#0f172a" }}>{b.beacon_code}</div>
                      <div style={{ fontSize: 11.5, color: "#94a3b8" }}>{b.zone_name}</div>
                    </td>
                    <td style={{ ...tdStyle, fontFamily: "Consolas, monospace", fontSize: 11.5 }}>{b.uuid}</td>
                    <td style={tdStyle}>{b.major}</td>
                    <td style={tdStyle}>{b.minor}</td>
                    <td style={tdStyle}>
                      {b.artifact_name_vi ? (<><div>{b.artifact_name_vi}</div><div style={{ fontSize: 11.5, color: "#94a3b8" }}>{b.artifact_code}</div></>) : <span style={{ color: "#94a3b8" }}>Chưa gán</span>}
                    </td>
                    <td style={tdStyle}>{fmtRadius(b.trigger_distance)}</td>
                    <td style={tdStyle}><span style={{ ...chipStyle, color: st.color, backgroundColor: st.bg }}>{st.label}</span></td>
                    <td style={tdStyle}>
                      <div>{t1}</div>
                      <div style={{ fontSize: 11.5, color: "#94a3b8" }}>{t2}</div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 14 }}>
          <span style={{ fontSize: 13, color: "#475569" }}>
            {filtered.length === 0 ? "Không có kết quả" : `Hiển thị ${(currentPage - 1) * PAGE_SIZE + 1}–${Math.min(currentPage * PAGE_SIZE, filtered.length)} trên ${filtered.length} thiết bị`}
          </span>
          <Pager page={currentPage} pageCount={pageCount} onChange={setPage} />
        </div>
      </div>

      {form ? (
        <div style={cardStyle}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
            <div>
              <h3 style={cardTitleStyle}>{creating ? "Thêm iBeacon mới" : `Chỉnh sửa iBeacon · ${selected?.beacon_code}`}</h3>
              <p style={cardSubStyle}>{creating ? "Nhập thông số thiết bị; có thể gán hiện vật ngay hoặc sau." : `Thiết bị đang chọn: ${selected?.zone_name ?? ""}`}</p>
            </div>
            {!creating ? <span style={{ ...chipStyle, color: info.color, backgroundColor: info.bg }}>{info.connect}</span> : null}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1.1fr 2.4fr 0.8fr 0.8fr 1.1fr", gap: 14, marginTop: 14 }}>
            <Field label="Tên / Mã Beacon *"><input value={form.beacon_code} onChange={(e) => set("beacon_code", e.target.value)} style={inputStyle} /></Field>
            <Field label="UUID *"><input value={form.uuid} onChange={(e) => set("uuid", e.target.value)} style={{ ...inputStyle, fontFamily: "Consolas, monospace" }} /></Field>
            <Field label="Major *"><input type="number" min={0} max={65535} value={form.major} onChange={(e) => set("major", e.target.value)} style={inputStyle} /></Field>
            <Field label="Minor *"><input type="number" min={0} max={65535} value={form.minor} onChange={(e) => set("minor", e.target.value)} style={inputStyle} /></Field>
            <Field label="Bán kính phát sóng *">
              <select value={form.trigger_distance} onChange={(e) => set("trigger_distance", e.target.value)} style={inputStyle}>
                {radiusOptions.map((r) => <option key={r} value={r}>{String(r).replace(".", ",")} mét</option>)}
              </select>
            </Field>
          </div>

          <div style={{ display: "flex", gap: 24, alignItems: "flex-end", marginTop: 14 }}>
            <div style={{ flex: 1.6 }}>
              <Field label="Gán vào hiện vật">
                <select value={form.artifact_id} onChange={(e) => set("artifact_id", e.target.value)} style={inputStyle}>
                  <option value="">— Chưa gán —</option>
                  {artifacts.map((a) => {
                    const other = a.beacon_code && a.beacon_code !== (creating ? "" : selected?.beacon_code);
                    return <option key={a.artifact_id} value={a.artifact_id}>{a.artifact_code} · {a.name_vi}{other ? ` (đang gắn ${a.beacon_code} — sẽ chuyển sang thiết bị này)` : ""}</option>;
                  })}
                </select>
              </Field>
            </div>
            <div style={{ flex: 1.4, fontSize: 12, color: "#64748b", lineHeight: 1.5, paddingBottom: 4 }}>
              <div>Bán kính kích hoạt gợi ý: 1–3 m.</div>
              <div>Mỗi iBeacon chỉ gán cho một hiện vật.</div>
            </div>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              {!creating && selected ? <button type="button" style={dangerLinkStyle} disabled={saving} onClick={() => void remove()}>Xóa thiết bị</button> : null}
              <button type="button" style={outlineBtnStyle} disabled={saving} onClick={cancel}>Hủy</button>
              <button type="button" style={primaryBtnStyle} disabled={saving} onClick={() => void save()}>{saving ? "Đang lưu..." : creating ? "Thêm thiết bị" : "Lưu thiết bị"}</button>
            </div>
          </div>

          <button type="button" style={advancedToggleStyle} onClick={() => setAdvanced(!advanced)} aria-expanded={advanced}>
            {advanced ? "▾" : "▸"} Thông số nâng cao
          </button>
          {advanced ? (
            <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr 1fr 1.2fr", gap: 14, marginTop: 8 }}>
              <Field label="Khu vực lắp đặt"><input value={form.zone_name} onChange={(e) => set("zone_name", e.target.value)} style={inputStyle} /></Field>
              <Field label="Công suất đo ở 1 m (dBm)"><input type="number" value={form.tx_power} onChange={(e) => set("tx_power", e.target.value)} style={inputStyle} /></Field>
              <Field label="Pin (%)"><input type="number" min={0} max={100} value={form.battery_level} onChange={(e) => set("battery_level", e.target.value)} style={inputStyle} /></Field>
              <Field label="Trạng thái">
                <select value={form.status} onChange={(e) => set("status", e.target.value as BeaconStatus)} style={inputStyle}>
                  {Object.entries(STATUS_INFO).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
                </select>
              </Field>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function Kpi({ icon, color, value, label }: { icon: string; color: string; value: number; label: string }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 14, backgroundColor: "#fff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "16px 18px" }}>
      <MaskIcon src={icon} color={color} size={24} />
      <div style={{ display: "flex", alignItems: "baseline", gap: 12 }}>
        <span style={{ fontSize: 28, fontWeight: 800, color, lineHeight: 1 }}>{value.toLocaleString("vi-VN")}</span>
        <span style={{ fontSize: 13, color: "#475569" }}>{label}</span>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label style={{ display: "block" }}>
      <span style={{ fontSize: 13, fontWeight: 600, color: "#1e293b" }}>{label}</span>
      <div style={{ marginTop: 6 }}>{children}</div>
    </label>
  );
}

const cardStyle: CSSProperties = { backgroundColor: "#fff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "18px 20px" };
const cardTitleStyle: CSSProperties = { fontSize: 16, fontWeight: 600, color: "#0f172a" };
const cardSubStyle: CSSProperties = { fontSize: 12.5, color: "#475569", marginTop: 2 };
const fieldBoxStyle: CSSProperties = { display: "flex", alignItems: "center", gap: 8, height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", color: "#0f172a" };
const thStyle: CSSProperties = { textAlign: "left", padding: "12px", fontSize: 12.5, fontWeight: 600, color: "#334155" };
const tdStyle: CSSProperties = { padding: "10px 12px", verticalAlign: "middle" };
const chipStyle: CSSProperties = { fontSize: 12, fontWeight: 600, padding: "4px 10px", borderRadius: 5, whiteSpace: "nowrap" };
const inputStyle: CSSProperties = { width: "100%", height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 13.5, color: "#0f172a", backgroundColor: "#fff" };
const primaryBtnStyle: CSSProperties = { height: 40, padding: "0 18px", border: "none", borderRadius: 8, backgroundColor: BRAND_RED, color: "#fff", fontSize: 13.5, fontWeight: 700, cursor: "pointer", whiteSpace: "nowrap" };
const outlineBtnStyle: CSSProperties = { height: 40, padding: "0 18px", border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", fontSize: 13.5, fontWeight: 600, color: "#0f172a", cursor: "pointer" };
const dangerLinkStyle: CSSProperties = { background: "none", border: "none", color: "#b91c1c", fontSize: 13, cursor: "pointer", whiteSpace: "nowrap" };
const advancedToggleStyle: CSSProperties = { marginTop: 14, background: "none", border: "none", color: "#475569", fontSize: 13, fontWeight: 600, cursor: "pointer", padding: 0 };
