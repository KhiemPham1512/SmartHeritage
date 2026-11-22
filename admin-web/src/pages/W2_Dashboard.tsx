// FILE: admin-web/src/pages/W2_Dashboard.tsx — Thống kê & báo cáo (Figma W2), số liệu thật từ /api/analytics/dashboard
import { useEffect, useMemo, useState, type CSSProperties, type ReactNode } from "react";
import { ChevronDown, Download, FileDown, Users } from "lucide-react";
import { apiFetch, downloadWithAuth } from "../api";

interface DashboardResponse {
  range: { from: string; to: string };
  kpi: {
    total_visits: number;
    prev_visits: number;
    visits_change_pct: number | null;
    avg_duration_sec: number;
    total_artifacts: number;
    published_artifacts: number;
    active_beacons: number;
    total_beacons: number;
    low_battery_count: number;
    lost_signal_count: number;
    avg_rating: number;
    total_reviews: number;
  };
  top_artifacts: Array<{ artifact_id: number; artifact_code: string; name_vi: string; visit_count: number }>;
  daily_visits: Array<{ visit_date: string; visit_count: number }>;
  language_distribution: Array<{ language_code: string; usage_count: number }>;
  generated_at: string;
}

interface DateRange {
  from: string;
  to: string;
}

const RED = "#80140e";
const BAR_COLORS = ["#9b1c1c", "#c0564b", "#c0564b"]; // hạng 1–3 đậm như Figma, còn lại hồng nhạt
const BAR_LIGHT = "#d9a7a0";
const LANG_COLORS = ["#9b1c1c", "#b45309", "#2b547e"];
const LANG_OTHER = "#cbd5e1";
const LANG_NAMES: Record<string, string> = {
  VI: "Tiếng Việt", EN: "English", KO: "한국어", JA: "日本語", ZH: "中文", FR: "Français",
};

// ---------- tiện ích ngày & số ----------
function toISO(d: Date): string {
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
function addDays(iso: string, days: number): string {
  const d = new Date(iso + "T00:00:00");
  d.setDate(d.getDate() + days);
  return toISO(d);
}
function fmtDate(iso: string): string {
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y}`;
}
const fmtNum = (n: number) => n.toLocaleString("vi-VN");
function lastDays(n: number): DateRange {
  const to = toISO(new Date());
  return { from: addDays(to, -(n - 1)), to };
}
function thisMonth(): DateRange {
  const now = new Date();
  return { from: toISO(new Date(now.getFullYear(), now.getMonth(), 1)), to: toISO(now) };
}
// Trục Y "đẹp": chọn bước tròn (1; 1,25; 1,5; 2; 2,5; 3; 4; 5; 7,5 × 10^k, tối thiểu 1) rồi nhân số khoảng chia
function niceAxisMax(value: number, intervals: number): number {
  const raw = Math.max(value, 1) / intervals;
  const pow = 10 ** Math.floor(Math.log10(raw));
  const step = ([1, 1.25, 1.5, 2, 2.5, 3, 4, 5, 7.5, 10].find((c) => c * pow >= raw) ?? 10) * pow;
  return Math.max(1, step) * intervals;
}
function splitTwoLines(name: string): [string, string] {
  const words = name.split(/\s+/);
  const cut = Math.ceil(words.length / 2);
  return [words.slice(0, cut).join(" "), words.slice(cut).join(" ")];
}
function fmtDuration(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = Math.round(sec % 60);
  return m > 0 ? `${m} phút ${s} giây` : `${s} giây`;
}

export default function W2_Dashboard() {
  const [range, setRange] = useState<DateRange>(() => lastDays(30));
  const [draft, setDraft] = useState<DateRange>(range);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [granularity, setGranularity] = useState<"day" | "week">("day");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    apiFetch<DashboardResponse>(`/api/analytics/dashboard?from=${range.from}&to=${range.to}`)
      .then((res) => {
        if (!active) return;
        setData(res);
        setError("");
      })
      .catch((e: Error) => active && setError(e.message))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [range, reloadKey]);

  const applyRange = (next: DateRange) => {
    if (next.from > next.to) return;
    setPickerOpen(false);
    setLoading(true);
    setRange(next);
  };

  // Chuỗi lượt khách đủ mọi ngày trong khoảng (ngày không có lượt = 0), gộp theo tuần nếu chọn
  const series = useMemo(() => {
    if (!data) return [] as Array<{ label: string; value: number }>;
    const counts = new Map(data.daily_visits.map((d) => [d.visit_date, d.visit_count]));
    const days: Array<{ label: string; value: number }> = [];
    for (let d = data.range.from; d <= data.range.to; d = addDays(d, 1)) {
      days.push({ label: d, value: counts.get(d) ?? 0 });
    }
    if (granularity === "day") return days;
    const weeks: Array<{ label: string; value: number }> = [];
    days.forEach((d, i) => {
      if (i % 7 === 0) weeks.push({ label: d.label, value: 0 });
      weeks[weeks.length - 1].value += d.value;
    });
    return weeks;
  }, [data, granularity]);

  const k = data?.kpi;
  const rangeText = `${fmtDate(range.from)} – ${fmtDate(range.to)}`;

  return (
    <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 11 }}>
      {/* HÀNG 1: TIÊU ĐỀ, KHOẢNG NGÀY & XUẤT BÁO CÁO */}
      <div style={pageHeaderRowStyle}>
        <div>
          <h2 style={pageTitleStyle}>Thống kê & báo cáo</h2>
          <p style={pageSubtitleStyle}>Theo dõi hành trình khám phá di sản qua từng điểm chạm.</p>
        </div>

        <div className="no-print" style={{ display: "flex", alignItems: "center", gap: 10, position: "relative" }}>
          <button
            type="button"
            onClick={() => {
              setDraft(range);
              setPickerOpen(!pickerOpen);
            }}
            style={dateRangeBoxStyle}
            aria-expanded={pickerOpen}
          >
            <span>{rangeText}</span>
            <ChevronDown size={16} />
          </button>

          {pickerOpen ? (
            <div style={pickerPopoverStyle} role="dialog" aria-label="Chọn khoảng thời gian">
              <div style={{ display: "flex", gap: 6, marginBottom: 10 }}>
                <button type="button" style={presetBtnStyle} onClick={() => applyRange(lastDays(7))}>7 ngày</button>
                <button type="button" style={presetBtnStyle} onClick={() => applyRange(lastDays(30))}>30 ngày</button>
                <button type="button" style={presetBtnStyle} onClick={() => applyRange(thisMonth())}>Tháng này</button>
              </div>
              <label style={pickerLabelStyle}>
                Từ ngày
                <input type="date" value={draft.from} max={draft.to} onChange={(e) => setDraft({ ...draft, from: e.target.value })} style={dateInputStyle} />
              </label>
              <label style={pickerLabelStyle}>
                Đến ngày
                <input type="date" value={draft.to} min={draft.from} onChange={(e) => setDraft({ ...draft, to: e.target.value })} style={dateInputStyle} />
              </label>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 10 }}>
                <button type="button" style={presetBtnStyle} onClick={() => setPickerOpen(false)}>Hủy</button>
                <button
                  type="button"
                  style={{ ...pdfBtnStyle, height: 30 }}
                  disabled={!draft.from || !draft.to || draft.from > draft.to}
                  onClick={() => applyRange(draft)}
                >
                  Áp dụng
                </button>
              </div>
            </div>
          ) : null}

          <button
            type="button"
            onClick={() => {
              downloadWithAuth(
                `/api/analytics/export-csv?from=${range.from}&to=${range.to}`,
                `SmartHeritage_Report_${range.from}_${range.to}.csv`,
              ).catch((e: Error) => alert(e.message));
            }}
            style={excelBtnStyle}
          >
            <Download size={16} />
            <span>Xuất Excel</span>
          </button>

          <button type="button" onClick={() => window.print()} style={pdfBtnStyle}>
            <FileDown size={16} />
            <span>Xuất PDF</span>
          </button>
        </div>
      </div>

      {error ? (
        <div style={errorBoxStyle} role="alert">
          <span>Không tải được số liệu: {error}</span>
          <button type="button" style={presetBtnStyle} onClick={() => { setLoading(true); setReloadKey((n) => n + 1); }}>
            Thử lại
          </button>
        </div>
      ) : null}

      {/* HÀNG 2: 4 THẺ KPI */}
      <div style={{ ...kpiGridStyle, opacity: loading ? 0.55 : 1 }}>
        <KpiCard
          label="Tổng lượt khách"
          icon={<Users size={19} color={RED} />}
          value={k ? fmtNum(k.total_visits) : "—"}
          sub={
            !k ? "" : k.visits_change_pct === null
              ? "Chưa có dữ liệu kỳ trước"
              : `${k.visits_change_pct >= 0 ? "+" : ""}${k.visits_change_pct.toLocaleString("vi-VN")}% so với kỳ trước`
          }
          subColor={!k || k.visits_change_pct === null ? "#334155" : k.visits_change_pct >= 0 ? "#15803d" : "#b91c1c"}
        />
        <KpiCard
          label="Tổng số hiện vật"
          icon={<MaskIcon src="/logo_bao_tang.svg" />}
          value={k ? fmtNum(k.total_artifacts) : "—"}
          sub={k ? `${fmtNum(k.published_artifacts)} hiện vật đang trưng bày` : ""}
        />
        <KpiCard
          label="iBeacon đang hoạt động"
          icon={<MaskIcon src="/thiet_bi_ibeacon.svg" />}
          value={k ? `${k.active_beacons} / ${k.total_beacons}` : "—"}
          sub={k ? `${k.low_battery_count} pin yếu · ${k.lost_signal_count} mất tín hiệu` : ""}
        />
        <KpiCard
          label="Điểm đánh giá trung bình"
          icon={<MaskIcon src="/diem_trung_binh.svg" />}
          value={k && k.total_reviews > 0 ? `${k.avg_rating.toLocaleString("vi-VN")} / 5` : "—"}
          sub={k ? `Từ ${fmtNum(k.total_reviews)} lượt đánh giá` : ""}
        />
      </div>

      {/* HÀNG 3: TOP 10 HIỆN VẬT */}
      <div style={sectionCardStyle}>
        <div style={cardHeaderRowStyle}>
          <div>
            <h3 style={cardTitleStyle}>Top 10 hiện vật được xem nhiều nhất</h3>
            <p style={cardSubStyle}>
              Tổng lượt tương tác với hiện vật từ {rangeText} · Đơn vị: lượt xem
              {k && k.total_visits > 0 ? ` · Thời lượng xem TB: ${fmtDuration(k.avg_duration_sec)}` : ""}
            </p>
          </div>
          <span style={pinkBadgeStyle}>Theo lượt xem</span>
        </div>
        <TopArtifactsChart items={data?.top_artifacts ?? []} />
      </div>

      {/* HÀNG 4: LƯỢT KHÁCH THEO NGÀY & NGÔN NGỮ */}
      <div style={{ display: "grid", gridTemplateColumns: "1.62fr 1fr", gap: 14 }}>
        <div style={sectionCardStyle}>
          <div style={cardHeaderRowStyle}>
            <div>
              <h3 style={cardTitleStyle}>Lượt khách tương tác theo ngày</h3>
              <p style={cardSubStyle}>{rangeText} · Đơn vị: lượt khách</p>
            </div>
            <select
              className="no-print"
              value={granularity}
              onChange={(e) => setGranularity(e.target.value as "day" | "week")}
              style={smallSelectBoxStyle}
              aria-label="Gộp theo"
            >
              <option value="day">Theo ngày</option>
              <option value="week">Theo tuần</option>
            </select>
          </div>
          <VisitsLineChart series={series} />
        </div>

        <LanguageDonut items={data?.language_distribution ?? []} total={k?.total_visits ?? 0} />
      </div>

      <div style={bottomMetaRowStyle}>
        <span>
          {data
            ? `Cập nhật lần cuối: ${fmtDate(data.generated_at.slice(0, 10))}, ${data.generated_at.slice(11, 16)} · Múi giờ GMT+7`
            : ""}
        </span>
        <span>Nguồn: CSDL SmartHeritageDB</span>
      </div>
    </div>
  );
}

// ---------- thành phần con ----------
function MaskIcon({ src, color = RED, size = 19 }: { src: string; color?: string; size?: number }) {
  return (
    <span
      aria-hidden
      style={{
        width: size, height: size, display: "inline-block", backgroundColor: color,
        WebkitMaskImage: `url('${src}')`, maskImage: `url('${src}')`,
        WebkitMaskSize: "contain", maskSize: "contain",
        WebkitMaskRepeat: "no-repeat", maskRepeat: "no-repeat",
      }}
    />
  );
}

function KpiCard(props: { label: string; icon: ReactNode; value: string; sub: string; subColor?: string }) {
  return (
    <div style={kpiCardStyle}>
      <div style={kpiTopRowStyle}>
        <span style={kpiLabelStyle}>{props.label}</span>
        {props.icon}
      </div>
      <div style={kpiMainValueStyle}>{props.value}</div>
      <div style={{ ...kpiSubTextStyle, color: props.subColor ?? kpiSubTextStyle.color }}>{props.sub}</div>
    </div>
  );
}

function EmptyChart({ height, text }: { height: number; text: string }) {
  return <div style={{ height, display: "flex", alignItems: "center", justifyContent: "center", color: "#64748b", fontSize: 13, fontWeight: 600 }}>{text}</div>;
}

function TopArtifactsChart({ items }: { items: DashboardResponse["top_artifacts"] }) {
  const max = niceAxisMax(Math.max(0, ...items.map((i) => i.visit_count)), 4);
  if (items.every((i) => i.visit_count === 0)) {
    return <EmptyChart height={190} text="Chưa có lượt xem hiện vật trong khoảng thời gian này." />;
  }
  const cols = `repeat(${items.length}, 1fr)`;
  return (
    <div style={{ marginTop: 14, display: "flex", flexDirection: "column" }}>
      <div style={{ position: "relative", height: 148, paddingLeft: 44 }}>
        {[4, 3, 2, 1, 0].map((step, idx) => (
          <div key={step} style={{ ...gridLineRowStyle, top: `${idx * 25}%` }}>
            <span style={axisLabelStyle}>{fmtNum((max * step) / 4)}</span>
            <div style={{ flex: 1, height: 1, backgroundColor: step === 0 ? "#94a3b8" : "#e2e8f0" }} />
          </div>
        ))}
        <div style={{ position: "absolute", left: 44, right: 0, top: 0, bottom: 0, display: "grid", gridTemplateColumns: cols, alignItems: "end", gap: 10 }}>
          {items.map((item, index) => (
            <div key={item.artifact_id} style={{ height: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "flex-end" }} title={`${item.artifact_code} · ${item.name_vi}`}>
              <span style={{ fontSize: 12.5, fontWeight: 800, color: "#0f172a", marginBottom: 3 }}>{fmtNum(item.visit_count)}</span>
              <div
                style={{
                  width: "70%", minWidth: 28, maxWidth: 64,
                  height: `${(item.visit_count / max) * 100}%`,
                  backgroundColor: BAR_COLORS[index] ?? BAR_LIGHT,
                  borderRadius: "6px 6px 0 0",
                }}
              />
            </div>
          ))}
        </div>
      </div>
      <div style={{ paddingLeft: 44, marginTop: 8, display: "grid", gridTemplateColumns: cols, gap: 10 }}>
        {items.map((item) => {
          const [l1, l2] = splitTwoLines(item.name_vi);
          return (
            <div key={item.artifact_id} style={{ textAlign: "center", fontSize: 12, fontWeight: 600, color: "#1e293b", lineHeight: 1.25 }}>
              <div>{l1}</div>
              <div style={{ color: "#475569" }}>{l2}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function VisitsLineChart({ series }: { series: Array<{ label: string; value: number }> }) {
  if (series.every((p) => p.value === 0)) {
    return <EmptyChart height={160} text="Chưa có lượt khách trong khoảng thời gian này." />;
  }
  const W = 540;
  const H = 132;
  const max = niceAxisMax(Math.max(...series.map((p) => p.value)), 3);
  const x = (i: number) => (series.length === 1 ? W / 2 : (i * W) / (series.length - 1));
  const y = (v: number) => H - (v / max) * H;
  const line = series.map((p, i) => `${x(i)},${y(p.value)}`).join(" ");
  const tickEvery = Math.max(1, Math.ceil(series.length / 7));
  const ticks = series.filter((_, i) => i % tickEvery === 0 || i === series.length - 1);

  return (
    <div style={{ marginTop: 12 }}>
      <div style={{ position: "relative", height: H, paddingLeft: 42 }}>
        {[3, 2, 1, 0].map((step, idx) => (
          <div key={step} style={{ ...gridLineRowStyle, top: `${idx * 33.33}%` }}>
            <span style={{ ...axisLabelStyle, width: 34 }}>{fmtNum(Math.round((max * step) / 3))}</span>
            <div style={{ flex: 1, height: 1, backgroundColor: step === 0 ? "#94a3b8" : "#e2e8f0" }} />
          </div>
        ))}
        <div style={{ position: "absolute", left: 42, right: 0, top: 0, bottom: 0 }}>
          <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: "100%", height: "100%", display: "block", overflow: "visible" }}>
            <polygon points={`${line} ${x(series.length - 1)},${H} ${x(0)},${H}`} fill="rgba(155, 28, 28, 0.07)" />
            <polyline fill="none" stroke="#9b1c1c" strokeWidth="2.4" vectorEffect="non-scaling-stroke" points={line} />
            {series.length <= 31
              ? series.map((p, i) => (
                  <circle key={p.label} cx={x(i)} cy={y(p.value)} r="2.6" fill="#ffffff" stroke="#9b1c1c" strokeWidth="1.6" vectorEffect="non-scaling-stroke">
                    <title>{`${fmtDate(p.label)}: ${fmtNum(p.value)} lượt`}</title>
                  </circle>
                ))
              : null}
          </svg>
        </div>
      </div>
      <div style={{ paddingLeft: 42, marginTop: 6, display: "flex", justifyContent: "space-between", fontSize: 12, fontWeight: 600, color: "#475569" }}>
        {ticks.map((t) => (
          <span key={t.label}>{fmtDate(t.label).slice(0, 5)}</span>
        ))}
      </div>
    </div>
  );
}

function LanguageDonut({ items, total }: { items: DashboardResponse["language_distribution"]; total: number }) {
  const named = items.slice(0, 3);
  const otherCount = items.slice(3).reduce((s, i) => s + i.usage_count, 0);
  const slices = [
    ...named.map((i, idx) => ({ label: LANG_NAMES[i.language_code] ?? i.language_code, count: i.usage_count, color: LANG_COLORS[idx] })),
    ...(otherCount > 0 ? [{ label: "Khác", count: otherCount, color: LANG_OTHER }] : []),
  ];
  let offset = 25; // bắt đầu ở 12 giờ

  return (
    <div style={{ ...sectionCardStyle, display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
      <div>
        <h3 style={cardTitleStyle}>Ngôn ngữ khách sử dụng</h3>
        <p style={cardSubStyle}>Phân bổ trên {fmtNum(total)} lượt khách</p>
      </div>
      {total === 0 ? (
        <EmptyChart height={150} text="Chưa có dữ liệu ngôn ngữ." />
      ) : (
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 22, margin: "auto 0", padding: "6px 0" }}>
          <div style={{ position: "relative", width: 142, height: 142, flexShrink: 0 }}>
            <svg viewBox="0 0 42 42" style={{ width: "100%", height: "100%" }}>
              {slices.map((s) => {
                const pct = (s.count / total) * 100;
                const el = (
                  <circle key={s.label} cx="21" cy="21" r="15.915" fill="transparent" stroke={s.color} strokeWidth="6.5" strokeDasharray={`${pct} ${100 - pct}`} strokeDashoffset={offset} />
                );
                offset -= pct;
                return el;
              })}
            </svg>
            <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
              <span style={{ fontSize: 24, fontWeight: 800, color: "#0f172a", lineHeight: 1.1 }}>{items.length}</span>
              <span style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>ngôn ngữ</span>
            </div>
          </div>
          <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 10 }}>
            {slices.map((s) => (
              <div key={s.label} style={legendRowStyle}>
                <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ width: 9, height: 9, borderRadius: "50%", backgroundColor: s.color }} />
                  <span style={{ fontWeight: 600 }}>{s.label}</span>
                </span>
                <strong style={{ fontSize: 13.5, fontWeight: 700, color: "#0f172a" }}>{Math.round((s.count / total) * 100)}%</strong>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ---------- style ----------
const pageHeaderRowStyle: CSSProperties = { display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 10 };
const pageTitleStyle: CSSProperties = { fontSize: 24, fontWeight: 800, color: "#0f172a", lineHeight: 1.2 };
const pageSubtitleStyle: CSSProperties = { fontSize: 13, color: "#475569", fontWeight: 500 };
const dateRangeBoxStyle: CSSProperties = {
  height: 38, padding: "0 12px", backgroundColor: "#ffffff", border: "1px solid #cbd5e1", borderRadius: 8,
  fontSize: 13, fontWeight: 600, color: "#0f172a", display: "flex", alignItems: "center", gap: 24, cursor: "pointer",
};
const pickerPopoverStyle: CSSProperties = {
  position: "absolute", top: 44, left: 0, zIndex: 20, width: 300, padding: 14, backgroundColor: "#ffffff",
  border: "1px solid #cbd5e1", borderRadius: 10, boxShadow: "0 10px 25px rgba(15, 23, 42, 0.12)",
};
const pickerLabelStyle: CSSProperties = { display: "flex", alignItems: "center", justifyContent: "space-between", fontSize: 13, fontWeight: 600, color: "#334155", marginTop: 8 };
const dateInputStyle: CSSProperties = { height: 32, padding: "0 8px", border: "1px solid #cbd5e1", borderRadius: 6, fontSize: 13, color: "#0f172a" };
const presetBtnStyle: CSSProperties = {
  height: 30, padding: "0 10px", backgroundColor: "#ffffff", border: "1px solid #cbd5e1", borderRadius: 6,
  fontSize: 12.5, fontWeight: 600, color: "#0f172a", cursor: "pointer",
};
const excelBtnStyle: CSSProperties = {
  height: 38, padding: "0 14px", backgroundColor: "#ffffff", color: "#0f172a", border: "1px solid #cbd5e1", borderRadius: 8,
  fontSize: 13, fontWeight: 700, display: "flex", alignItems: "center", gap: 8, cursor: "pointer",
};
const pdfBtnStyle: CSSProperties = {
  height: 38, padding: "0 16px", backgroundColor: RED, color: "#ffffff", border: "none", borderRadius: 8,
  fontSize: 13, fontWeight: 700, cursor: "pointer", display: "flex", alignItems: "center", gap: 8,
};
const errorBoxStyle: CSSProperties = {
  display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, backgroundColor: "#fef2f2",
  color: "#b91c1c", border: "1px solid #fca5a5", padding: "8px 12px", borderRadius: 8, fontSize: 13, fontWeight: 600,
};
const kpiGridStyle: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 14, transition: "opacity 0.15s" };
const kpiCardStyle: CSSProperties = { backgroundColor: "#ffffff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "14px 18px" };
const kpiTopRowStyle: CSSProperties = { display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 };
const kpiLabelStyle: CSSProperties = { fontSize: 13, color: "#334155", fontWeight: 500 };
const kpiMainValueStyle: CSSProperties = { fontSize: 30, fontWeight: 800, color: "#0f172a", marginBottom: 4, letterSpacing: -0.5 };
const kpiSubTextStyle: CSSProperties = { fontSize: 12, color: "#475569", fontWeight: 500 };
const sectionCardStyle: CSSProperties = { backgroundColor: "#ffffff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "16px 20px" };
const cardHeaderRowStyle: CSSProperties = { display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: 10 };
const cardTitleStyle: CSSProperties = { fontSize: 16, fontWeight: 600, color: "#0f172a", marginBottom: 2 };
const cardSubStyle: CSSProperties = { fontSize: 12.5, color: "#475569", fontWeight: 500 };
const pinkBadgeStyle: CSSProperties = { backgroundColor: "#fde8e6", color: RED, fontSize: 12, fontWeight: 700, padding: "3px 10px", borderRadius: 6 };
const smallSelectBoxStyle: CSSProperties = {
  height: 38, padding: "0 10px", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 13, fontWeight: 500,
  color: "#0f172a", backgroundColor: "#ffffff", cursor: "pointer",
};
const gridLineRowStyle: CSSProperties = { position: "absolute", left: 0, right: 0, transform: "translateY(-50%)", display: "flex", alignItems: "center" };
const axisLabelStyle: CSSProperties = { width: 36, fontSize: 11.5, fontWeight: 500, color: "#475569", textAlign: "right", marginRight: 8 };
const legendRowStyle: CSSProperties = { display: "flex", alignItems: "center", justifyContent: "space-between", fontSize: 13, color: "#0f172a" };
const bottomMetaRowStyle: CSSProperties = { display: "flex", justifyContent: "space-between", fontSize: 11.5, fontWeight: 500, color: "#475569" };
