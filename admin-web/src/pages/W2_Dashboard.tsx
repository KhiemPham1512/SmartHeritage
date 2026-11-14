// FILE: admin-web/src/pages/W2_Dashboard.tsx
import { useState, useEffect, type CSSProperties } from "react";
import { API_BASE, apiFetch } from "../api";

const W2_ICONS: Record<string, string> = {
  kpiVisitors: "",
  kpiArtifacts: "",
  kpiBeacons: "",
  kpiRating: "",
  exportExcel: "",
  exportPdf: "",
};

interface DashboardResponse {
  kpi: {
    total_visits: number;
    total_artifacts: number;
    active_beacons: number;
    total_beacons: number;
    avg_rating: number;
    total_reviews: number;
  };
}

interface BarChartItem {
  line1: string;
  line2: string;
  views: number;
}

const FIGMA_TOP_10: BarChartItem[] = [
  { line1: "Trống đồng", line2: "Ngọc Lũ", views: 3240 },
  { line1: "Bình gốm", line2: "Chu Đậu", views: 2810 },
  { line1: "Tượng", line2: "Apsara", views: 2540 },
  { line1: "Kiếm", line2: "Đông Sơn", views: 2390 },
  { line1: "Chuông", line2: "Thanh Mai", views: 2210 },
  { line1: "Ấn vàng", line2: "Triều Nguyễn", views: 1980 },
  { line1: "Đèn gốm", line2: "Hoa Lam", views: 1820 },
  { line1: "Bia đá", line2: "Văn Miếu", views: 1610 },
  { line1: "Áo", line2: "Nhật Bình", views: 1450 },
  { line1: "Bình đồng", line2: "Đông Sơn", views: 1290 },
];

// Tọa độ các điểm nút trên biểu đồ đường để vẽ chấm tròn trực quan
const LINE_CHART_POINTS: Array<[number, number]> = [
  [0, 126], [25, 120], [48, 96], [72, 105], [95, 76], [118, 94],
  [142, 66], [165, 82], [188, 92], [212, 46], [238, 56], [262, 28],
  [286, 46], [310, 54], [336, 26], [362, 68], [390, 42], [416, 18],
  [442, 36], [468, 22], [494, 6], [518, 20], [540, 10],
];

export default function W2_Dashboard() {
  const [apiData, setApiData] = useState<DashboardResponse | null>(null);
  const [useLiveDbNumbers, setUseLiveDbNumbers] = useState<boolean>(false);

  useEffect(() => {
    apiFetch<DashboardResponse>("/api/analytics/dashboard")
      .then((res) => setApiData(res))
      .catch(() => {});
  }, []);

  const totalVisitsDisplay =
    useLiveDbNumbers && apiData ? String(apiData.kpi.total_visits) : "24.860";
  const totalArtifactsDisplay =
    useLiveDbNumbers && apiData ? String(apiData.kpi.total_artifacts) : "128";
  const activeBeaconsDisplay =
    useLiveDbNumbers && apiData
      ? String(apiData.kpi.active_beacons) + " / " + String(apiData.kpi.total_beacons)
      : "42 / 48";
  const avgRatingDisplay =
    useLiveDbNumbers && apiData ? String(apiData.kpi.avg_rating) + " / 5" : "4,8 / 5";

  const maxBarValue = 4000;
  const polylineString = LINE_CHART_POINTS.map((p) => String(p[0]) + "," + String(p[1])).join(" ");
  const polygonString = polylineString + " 540,132 0,132";

  return (
    <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 11 }}>
      {/* ================================================================= */}
      {/* HÀNG 1: TIÊU ĐỀ & NÚT XUẤT BÁO CÁO                                */}
      {/* ================================================================= */}
      <div style={pageHeaderRowStyle}>
        <div>
          <h2 style={pageTitleStyle}>Thống kê & báo cáo</h2>
          <p style={pageSubtitleStyle}>
            Theo dõi hành trình khám phá di sản qua từng điểm chạm.
          </p>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <button
            type="button"
            onClick={() => setUseLiveDbNumbers(!useLiveDbNumbers)}
            style={dateRangeBoxStyle}
            title="Nhấn để chuyển đổi giữa Số liệu Figma và Số liệu SQL Server"
          >
            <span>01/03/2026 - 30/03/2026</span>
          </button>

          <a
            href={API_BASE + "/api/analytics/export-csv"}
            target="_blank"
            rel="noreferrer"
            style={excelBtnStyle}
          >
            <span>Xuất Excel</span>
          </a>

          <button type="button" onClick={() => window.print()} style={pdfBtnStyle}>
            <span>Xuất PDF</span>
          </button>
        </div>
      </div>

      {/* ================================================================= */}
      {/* HÀNG 2: 4 THẺ KPI (CHỮ TO, ĐẬM, VIỀN SẮC NÉT)                     */}
      {/* ================================================================= */}
      <div style={kpiGridStyle}>
        <div style={kpiCardStyle}>
          <div style={kpiTopRowStyle}>
            <span style={kpiLabelStyle}>Tổng lượt khách</span>
            {W2_ICONS.kpiVisitors ? <img src={W2_ICONS.kpiVisitors} alt="" style={{ width: 18, height: 18 }} /> : null}
          </div>
          <div style={kpiMainValueStyle}>{totalVisitsDisplay}</div>
          <div style={{ fontSize: 12.5, color: "#15803d", fontWeight: 800 }}>
            +12,5% so với tháng trước
          </div>
        </div>

        <div style={kpiCardStyle}>
          <div style={kpiTopRowStyle}>
            <span style={kpiLabelStyle}>Tổng số hiện vật</span>
            {W2_ICONS.kpiArtifacts ? <img src={W2_ICONS.kpiArtifacts} alt="" style={{ width: 18, height: 18 }} /> : null}
          </div>
          <div style={kpiMainValueStyle}>{totalArtifactsDisplay}</div>
          <div style={kpiSubTextStyle}>116 hiện vật đang trưng bày</div>
        </div>

        <div style={kpiCardStyle}>
          <div style={kpiTopRowStyle}>
            <span style={kpiLabelStyle}>iBeacon đang hoạt động</span>
            {W2_ICONS.kpiBeacons ? <img src={W2_ICONS.kpiBeacons} alt="" style={{ width: 18, height: 18 }} /> : null}
          </div>
          <div style={kpiMainValueStyle}>{activeBeaconsDisplay}</div>
          <div style={kpiSubTextStyle}>4 pin yếu • 2 mất tín hiệu</div>
        </div>

        <div style={kpiCardStyle}>
          <div style={kpiTopRowStyle}>
            <span style={kpiLabelStyle}>Điểm đánh giá trung bình</span>
            {W2_ICONS.kpiRating ? <img src={W2_ICONS.kpiRating} alt="" style={{ width: 18, height: 18 }} /> : null}
          </div>
          <div style={kpiMainValueStyle}>{avgRatingDisplay}</div>
          <div style={kpiSubTextStyle}>Từ 1.284 lượt đánh giá</div>
        </div>
      </div>

      {/* ================================================================= */}
      {/* HÀNG 3: BIỂU ĐỒ 10 CỘT (CỘT TO BẢN, MÀU ĐẬM NÉT)                  */}
      {/* ================================================================= */}
      <div style={sectionCardStyle}>
        <div style={cardHeaderRowStyle}>
          <div>
            <h3 style={cardTitleStyle}>Top 10 hiện vật được xem nhiều nhất</h3>
            <p style={cardSubStyle}>
              Tổng lượt tương tác với hiện vật trong tháng 03/2026 • Đơn vị: lượt xem
            </p>
          </div>
          <span style={pinkBadgeStyle}>Theo lượt xem</span>
        </div>

        <div style={{ marginTop: 14, display: "flex", flexDirection: "column" }}>
          <div style={{ position: "relative", height: 148, paddingLeft: 44 }}>
            {[4000, 3000, 2000, 1000, 0].map((val, idx) => {
              const topPercent = idx * 25;
              return (
                <div
                  key={val}
                  style={{
                    position: "absolute",
                    left: 0,
                    right: 0,
                    top: String(topPercent) + "%",
                    transform: "translateY(-50%)",
                    display: "flex",
                    alignItems: "center",
                  }}
                >
                  <span style={{ width: 36, fontSize: 12, fontWeight: 700, color: "#334155", textAlign: "right", marginRight: 8 }}>
                    {val === 0 ? "0" : val.toLocaleString("vi-VN")}
                  </span>
                  <div style={{ flex: 1, height: 1, backgroundColor: val === 0 ? "#94a3b8" : "#cbd5e1" }} />
                </div>
              );
            })}

            <div
              style={{
                position: "absolute",
                left: 44,
                right: 0,
                top: 0,
                bottom: 0,
                display: "grid",
                gridTemplateColumns: "repeat(10, 1fr)",
                alignItems: "end",
                gap: 10,
              }}
            >
              {FIGMA_TOP_10.map((item, index) => {
                const heightPercent = Math.round((item.views / maxBarValue) * 100);
                // Màu cột đậm và rõ nét hơn hẳn
                const barColor =
                  index === 0 ? "#7f1510" : index <= 2 ? "#a8382f" : "#c97a70";

                return (
                  <div
                    key={item.line1 + item.line2}
                    style={{
                      height: "100%",
                      display: "flex",
                      flexDirection: "column",
                      alignItems: "center",
                      justifyContent: "flex-end",
                    }}
                  >
                    <span style={{ fontSize: 12.5, fontWeight: 800, color: "#0f172a", marginBottom: 3 }}>
                      {item.views.toLocaleString("vi-VN")}
                    </span>
                    <div
                      style={{
                        width: "70%",
                        minWidth: 36,
                        maxWidth: 64,
                        height: String(heightPercent) + "%",
                        backgroundColor: barColor,
                        borderRadius: "6px 6px 0 0",
                        boxShadow: "0 2px 4px rgba(0,0,0,0.08)",
                      }}
                    />
                  </div>
                );
              })}
            </div>
          </div>

          <div
            style={{
              paddingLeft: 44,
              marginTop: 8,
              display: "grid",
              gridTemplateColumns: "repeat(10, 1fr)",
              gap: 10,
            }}
          >
            {FIGMA_TOP_10.map((item) => (
              <div
                key={item.line1 + item.line2}
                style={{
                  textAlign: "center",
                  fontSize: 12,
                  fontWeight: 700,
                  color: "#1e293b",
                  lineHeight: 1.25,
                }}
              >
                <div>{item.line1}</div>
                <div>{item.line2}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ================================================================= */}
      {/* HÀNG 4: BIỂU ĐỒ ĐƯỜNG & BIỂU ĐỒ TRÒN DONUT                        */}
      {/* ================================================================= */}
      <div style={{ display: "grid", gridTemplateColumns: "1.62fr 1fr", gap: 14 }}>
        {/* Lượt khách tương tác theo ngày */}
        <div style={sectionCardStyle}>
          <div style={cardHeaderRowStyle}>
            <div>
              <h3 style={cardTitleStyle}>Lượt khách tương tác theo ngày</h3>
              <p style={cardSubStyle}>Tháng 03/2026 • Đơn vị: lượt khách</p>
            </div>
            <div style={smallSelectBoxStyle}>
              <span>Theo ngày ▾</span>
            </div>
          </div>

          <div style={{ marginTop: 12 }}>
            <div style={{ position: "relative", height: 132, paddingLeft: 42 }}>
              {["1.200", "800", "400", "0"].map((label, idx) => {
                const topPercent = idx * 33.33;
                return (
                  <div
                    key={label}
                    style={{
                      position: "absolute",
                      left: 0,
                      right: 0,
                      top: String(topPercent) + "%",
                      transform: "translateY(-50%)",
                      display: "flex",
                      alignItems: "center",
                    }}
                  >
                    <span style={{ width: 34, fontSize: 12, fontWeight: 700, color: "#334155", textAlign: "right", marginRight: 8 }}>
                      {label}
                    </span>
                    <div style={{ flex: 1, height: 1, backgroundColor: label === "0" ? "#94a3b8" : "#cbd5e1" }} />
                  </div>
                );
              })}

              <div style={{ position: "absolute", left: 42, right: 0, top: 0, bottom: 0 }}>
                <svg
                  viewBox="0 0 540 132"
                  preserveAspectRatio="none"
                  style={{ width: "100%", height: "100%", display: "block", overflow: "visible" }}
                >
                  <polygon points={polygonString} fill="rgba(127, 21, 16, 0.15)" />
                  <polyline
                    fill="none"
                    stroke="#7f1510"
                    strokeWidth="2.8"
                    vectorEffect="non-scaling-stroke"
                    points={polylineString}
                  />
                  {LINE_CHART_POINTS.filter((_, i) => i % 2 === 0).map((pt, idx) => (
                    <circle
                      key={idx}
                      cx={pt[0]}
                      cy={pt[1]}
                      r="3.2"
                      fill="#ffffff"
                      stroke="#7f1510"
                      strokeWidth="2"
                    />
                  ))}
                </svg>
              </div>
            </div>

            <div
              style={{
                paddingLeft: 42,
                marginTop: 6,
                display: "flex",
                justifyContent: "space-between",
                fontSize: 12,
                fontWeight: 700,
                color: "#334155",
              }}
            >
              <span>01/03</span>
              <span>05/03</span>
              <span>10/03</span>
              <span>15/03</span>
              <span>20/03</span>
              <span>25/03</span>
              <span>30/03</span>
            </div>
          </div>
        </div>

        {/* Ngôn ngữ khách sử dụng */}
        <div style={{ ...sectionCardStyle, display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
          <div>
            <h3 style={cardTitleStyle}>Ngôn ngữ khách sử dụng</h3>
            <p style={cardSubStyle}>Phân bổ trên 24.860 lượt khách</p>
          </div>

          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 22, margin: "auto 0", padding: "6px 0" }}>
            <div style={{ position: "relative", width: 142, height: 142, flexShrink: 0 }}>
              <svg viewBox="0 0 42 42" style={{ width: "100%", height: "100%" }}>
                <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#94a3b8" strokeWidth="6.5" />
                <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#7f1510" strokeWidth="6.5" strokeDasharray="52 48" strokeDashoffset="25" />
                <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#c85a17" strokeWidth="6.5" strokeDasharray="24 76" strokeDashoffset="73" />
                <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="#2b547e" strokeWidth="6.5" strokeDasharray="13 87" strokeDashoffset="49" />
              </svg>
              <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
                <span style={{ fontSize: 24, fontWeight: 800, color: "#0f172a", lineHeight: 1.1 }}>4</span>
                <span style={{ fontSize: 12, fontWeight: 700, color: "#334155" }}>ngôn ngữ</span>
              </div>
            </div>

            <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 10 }}>
              <div style={legendRowStyle}>
                <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ width: 11, height: 11, borderRadius: "50%", backgroundColor: "#7f1510" }} />
                  <span style={{ fontWeight: 700 }}>Tiếng Việt</span>
                </span>
                <strong style={{ fontSize: 14.5, fontWeight: 800, color: "#0f172a" }}>52%</strong>
              </div>

              <div style={legendRowStyle}>
                <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ width: 11, height: 11, borderRadius: "50%", backgroundColor: "#c85a17" }} />
                  <span style={{ fontWeight: 700 }}>English</span>
                </span>
                <strong style={{ fontSize: 14.5, fontWeight: 800, color: "#0f172a" }}>24%</strong>
              </div>

              <div style={legendRowStyle}>
                <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ width: 11, height: 11, borderRadius: "50%", backgroundColor: "#2b547e" }} />
                  <span style={{ fontWeight: 700 }}>한국어</span>
                </span>
                <strong style={{ fontSize: 14.5, fontWeight: 800, color: "#0f172a" }}>13%</strong>
              </div>

              <div style={legendRowStyle}>
                <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ width: 11, height: 11, borderRadius: "50%", backgroundColor: "#94a3b8" }} />
                  <span style={{ fontWeight: 700 }}>Khác</span>
                </span>
                <strong style={{ fontSize: 14.5, fontWeight: 800, color: "#0f172a" }}>11%</strong>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* DÒNG THÔNG TIN CUỐI TRANG */}
      <div style={bottomMetaRowStyle}>
        <span>Cập nhật lần cuối: 30/03/2026, 17:30 • Múi giờ GMT+7</span>
        <span>Số liệu minh họa phục vụ thiết kế</span>
      </div>
    </div>
  );
}

const pageHeaderRowStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  flexWrap: "wrap",
  gap: 10,
};

const pageTitleStyle: CSSProperties = {
  fontSize: 21,
  fontWeight: 800,
  color: "#0f172a",
  lineHeight: 1.2,
};

const pageSubtitleStyle: CSSProperties = {
  fontSize: 13,
  color: "#334155",
  fontWeight: 600,
};

const dateRangeBoxStyle: CSSProperties = {
  height: 34,
  padding: "0 12px",
  backgroundColor: "#ffffff",
  border: "1.5px solid #94a3b8",
  borderRadius: 8,
  fontSize: 12.5,
  fontWeight: 700,
  color: "#0f172a",
  display: "flex",
  alignItems: "center",
  cursor: "pointer",
};

const excelBtnStyle: CSSProperties = {
  height: 34,
  padding: "0 14px",
  backgroundColor: "#ffffff",
  color: "#0f172a",
  border: "1.5px solid #94a3b8",
  borderRadius: 8,
  fontSize: 12.5,
  fontWeight: 800,
  textDecoration: "none",
  display: "flex",
  alignItems: "center",
};

const pdfBtnStyle: CSSProperties = {
  height: 34,
  padding: "0 16px",
  backgroundColor: "#80140e",
  color: "#ffffff",
  border: "none",
  borderRadius: 8,
  fontSize: 12.5,
  fontWeight: 800,
  cursor: "pointer",
  display: "flex",
  alignItems: "center",
};

const kpiGridStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(4, 1fr)",
  gap: 14,
};

const kpiCardStyle: CSSProperties = {
  backgroundColor: "#ffffff",
  border: "1.5px solid #d4cdc3",
  borderRadius: 11,
  padding: "12px 16px",
  boxShadow: "0 2px 5px rgba(15, 23, 42, 0.04)",
};

const kpiTopRowStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  marginBottom: 4,
};

const kpiLabelStyle: CSSProperties = {
  fontSize: 13,
  color: "#1e293b",
  fontWeight: 700,
};

const kpiMainValueStyle: CSSProperties = {
  fontSize: 26,
  fontWeight: 800,
  color: "#0f172a",
  marginBottom: 2,
  letterSpacing: -0.5,
};

const kpiSubTextStyle: CSSProperties = {
  fontSize: 12,
  color: "#334155",
  fontWeight: 600,
};

const sectionCardStyle: CSSProperties = {
  backgroundColor: "#ffffff",
  border: "1.5px solid #d4cdc3",
  borderRadius: 11,
  padding: "14px 18px",
  boxShadow: "0 2px 5px rgba(15, 23, 42, 0.04)",
};

const cardHeaderRowStyle: CSSProperties = {
  display: "flex",
  alignItems: "flex-start",
  justifyContent: "space-between",
  gap: 10,
};

const cardTitleStyle: CSSProperties = {
  fontSize: 15.5,
  fontWeight: 800,
  color: "#0f172a",
  marginBottom: 2,
};

const cardSubStyle: CSSProperties = {
  fontSize: 12.5,
  color: "#334155",
  fontWeight: 600,
};

const pinkBadgeStyle: CSSProperties = {
  backgroundColor: "#fde8e6",
  border: "1.5px solid #eca9a4",
  color: "#80140e",
  fontSize: 12,
  fontWeight: 800,
  padding: "3px 10px",
  borderRadius: 6,
};

const smallSelectBoxStyle: CSSProperties = {
  padding: "4px 10px",
  border: "1.5px solid #94a3b8",
  borderRadius: 6,
  fontSize: 12,
  fontWeight: 700,
  color: "#0f172a",
};

const legendRowStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  fontSize: 13.5,
  color: "#0f172a",
};

const bottomMetaRowStyle: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  fontSize: 11.5,
  fontWeight: 600,
  color: "#334155",
};