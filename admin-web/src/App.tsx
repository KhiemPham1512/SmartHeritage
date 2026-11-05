// FILE: admin-web/src/App.tsx
import { useState, type CSSProperties } from "react";
import { type UserInfo } from "./api";
import W1_Login from "./pages/W1_Login";
import W2_Dashboard from "./pages/W2_Dashboard";
import W3_Artifacts from "./pages/W3_Artifacts";
import W4_ArtifactForm from "./pages/W4_ArtifactForm";
import W5_Beacons from "./pages/W5_Beacons";
import W6_FloorMap from "./pages/W6_FloorMap";
import W7_Feedbacks from "./pages/W7_Feedbacks";
import W8_Users from "./pages/W8_Users";

const LAYOUT_ICONS = {
  sidebarLogo: "/logo_cong_dinh_tron.svg",
  museumCardIcon: "/logo_bao_tang.svg",
  menuW2: "/thong_ke.svg",
  menuW3: "/quan_ly_hien_vat.svg",
  menuW5: "/thiet_bi_ibeacon.svg",
  menuW6: "/ban_do.svg",
  menuW7: "/danh_gia_phan_hoi.svg",
  menuW8: "/ngon_ngu.svg",
};

const MENU_ITEMS: Array<{ key: string; label: string; iconUrl: string }> = [
  { key: "W2", label: "Thống kê & báo cáo", iconUrl: LAYOUT_ICONS.menuW2 },
  { key: "W3", label: "Quản lý hiện vật", iconUrl: LAYOUT_ICONS.menuW3 },
  { key: "W5", label: "Thiết bị iBeacon", iconUrl: LAYOUT_ICONS.menuW5 },
  { key: "W6", label: "Bản đồ khu di tích", iconUrl: LAYOUT_ICONS.menuW6 },
  { key: "W7", label: "Đánh giá & phản hồi", iconUrl: LAYOUT_ICONS.menuW7 },
  { key: "W8", label: "Ngôn ngữ & tài khoản", iconUrl: LAYOUT_ICONS.menuW8 },
];

export default function App() {
  const [token, setToken] = useState<string>(localStorage.getItem("sh_token") || "");
  const [user, setUser] = useState<UserInfo | null>(() => {
    const saved = localStorage.getItem("sh_user");
    return saved ? (JSON.parse(saved) as UserInfo) : null;
  });
  const [activeScreen, setActiveScreen] = useState<string>("W2");

  const handleLoginSuccess = (newToken: string, userInfo: UserInfo) => {
    setToken(newToken);
    setUser(userInfo);
    setActiveScreen("W2");
  };

  const handleLogout = () => {
    localStorage.removeItem("sh_token");
    localStorage.removeItem("sh_user");
    setToken("");
    setUser(null);
  };

  if (!token) {
    return <W1_Login onLoginSuccess={handleLoginSuccess} />;
  }

  const currentMenuLabel =
    activeScreen === "W4"
      ? "Thêm / Sửa hiện vật"
      : MENU_ITEMS.find((m) => m.key === activeScreen)?.label || "Thống kê & báo cáo";

  return (
    <div style={appLayoutStyle}>
      {/* ================================================================= */}
      {/* SIDEBAR TRÁI (GỌN CHIỀU DỌC ĐỂ VỪA KHÍT MÀN HÌNH 100%)            */}
      {/* ================================================================= */}
      <aside style={sidebarStyle}>
        <div>
          <div style={sidebarBrandCardStyle}>
            <div style={sidebarLogoLineRowStyle}>
              <div style={goldLineStyle} />
              <img
                src={LAYOUT_ICONS.sidebarLogo}
                alt="Logo"
                style={{ width: 32, height: 32, objectFit: "contain" }}
              />
              <div style={goldLineStyle} />
            </div>
            <div style={sidebarBrandTitleStyle}>SMART HERITAGE</div>
            <div style={sidebarBrandSubStyle}>Kết nối di sản - Lan tỏa giá trị</div>
          </div>

          <div style={menuSectionTitleStyle}>KHÔNG GIAN QUẢN TRỊ</div>

          <nav style={{ padding: "0 12px" }}>
            {MENU_ITEMS.map((item) => {
              const isActive =
                activeScreen === item.key || (item.key === "W3" && activeScreen === "W4");
              const iconColor = isActive ? "#80140e" : "#334155";

              return (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => setActiveScreen(item.key)}
                  style={{
                    width: "100%",
                    display: "flex",
                    alignItems: "center",
                    gap: 11,
                    padding: "9px 12px",
                    marginBottom: 4,
                    borderRadius: 8,
                    border: isActive ? "1.5px solid #eca9a4" : "1.5px solid transparent",
                    cursor: "pointer",
                    fontSize: 13.5,
                    fontWeight: isActive ? 800 : 600,
                    backgroundColor: isActive ? "#fde8e6" : "transparent",
                    color: isActive ? "#80140e" : "#1e293b",
                    textAlign: "left",
                    transition: "all 0.15s ease",
                  }}
                >
                  <span
                    style={{
                      width: 18,
                      height: 18,
                      display: "inline-block",
                      flexShrink: 0,
                      backgroundColor: iconColor,
                      WebkitMaskImage: "url('" + item.iconUrl + "')",
                      maskImage: "url('" + item.iconUrl + "')",
                      WebkitMaskSize: "contain",
                      maskSize: "contain",
                      WebkitMaskRepeat: "no-repeat",
                      maskRepeat: "no-repeat",
                      WebkitMaskPosition: "center",
                      maskPosition: "center",
                    }}
                  />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* Khối Bảo tàng Di sản Việt ở đáy Sidebar */}
        <div style={{ padding: "0 12px 12px 12px" }}>
          <div style={museumInfoBoxStyle}>
            <img
              src={LAYOUT_ICONS.museumCardIcon}
              alt="Bảo tàng"
              style={{ width: 20, height: 20, objectFit: "contain", marginBottom: 6, display: "block" }}
            />
            <div style={{ fontWeight: 800, fontSize: 13, color: "#0f172a", marginBottom: 3 }}>
              Bảo tàng Di sản Việt
            </div>
            <div style={{ fontSize: 11.5, color: "#334155", fontWeight: 500, lineHeight: 1.35, marginBottom: 8 }}>
              Hệ thống quản lý di sản thông minh
            </div>
            <span style={illustrationBadgeStyle}>Không gian minh họa</span>
          </div>

          <div style={helpRowStyle}>
            <span>Trung tâm trợ giúp</span>
          </div>
          <div style={{ fontSize: 11, color: "#475569", paddingLeft: 6, marginTop: 4, fontWeight: 600 }}>
            © 2026 Smart Heritage • v1.0
          </div>
        </div>
      </aside>

      {/* ================================================================= */}
      {/* CỘT PHẢI: TOP HEADER BAR + NỘI DUNG MÀN HÌNH                      */}
      {/* ================================================================= */}
      <div style={rightContentWrapperStyle}>
        <header style={topHeaderStyle}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13.5 }}>
            <span style={{ color: "#475569", fontWeight: 600 }}>Web Admin</span>
            <span style={{ color: "#64748b", fontWeight: 700 }}>›</span>
            <strong style={{ color: "#0f172a", fontWeight: 800 }}>{currentMenuLabel}</strong>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <span style={demoDataPillStyle}>Dữ liệu minh họa</span>

            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div style={avatarCircleStyle}>NA</div>
              <div style={{ lineHeight: 1.2 }}>
                <div style={{ fontSize: 13.5, fontWeight: 800, color: "#0f172a" }}>
                  {user ? user.full_name : "Ngọc Anh"}
                </div>
                <div style={{ fontSize: 11.5, color: "#334155", fontWeight: 600 }}>Quản trị viên</div>
              </div>
              <button type="button" onClick={handleLogout} style={logoutSmallBtnStyle}>
                Đăng xuất
              </button>
            </div>
          </div>
        </header>

        <main style={mainScrollAreaStyle}>
          {activeScreen === "W2" && <W2_Dashboard />}
          {activeScreen === "W3" && <W3_Artifacts onNavigateW4={() => setActiveScreen("W4")} />}
          {activeScreen === "W4" && <W4_ArtifactForm />}
          {activeScreen === "W5" && <W5_Beacons />}
          {activeScreen === "W6" && <W6_FloorMap />}
          {activeScreen === "W7" && <W7_Feedbacks />}
          {activeScreen === "W8" && <W8_Users />}
        </main>
      </div>
    </div>
  );
}

const appLayoutStyle: CSSProperties = {
  display: "flex",
  width: "100%",
  height: "100vh",
  backgroundColor: "#f1ede4",
  overflow: "hidden",
};

const sidebarStyle: CSSProperties = {
  width: 224,
  height: "100vh",
  backgroundColor: "#ffffff",
  borderRight: "1.5px solid #d4cdc3",
  display: "flex",
  flexDirection: "column",
  justifyContent: "space-between",
  flexShrink: 0,
};

const sidebarBrandCardStyle: CSSProperties = {
  margin: "10px 12px 6px 12px",
  padding: "12px 10px",
  backgroundColor: "#f9f4eb",
  border: "1.5px solid #e2d5c1",
  borderRadius: 10,
  textAlign: "center",
};

const sidebarLogoLineRowStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 8,
  marginBottom: 6,
};

const goldLineStyle: CSSProperties = {
  width: 26,
  height: 1.5,
  backgroundColor: "#b88942",
};

const sidebarBrandTitleStyle: CSSProperties = {
  fontSize: 14.5,
  fontWeight: 800,
  color: "#80140e",
  letterSpacing: 0.8,
  fontFamily: "Georgia, serif",
};

const sidebarBrandSubStyle: CSSProperties = {
  fontSize: 11,
  color: "#6b4718",
  fontWeight: 600,
  marginTop: 2,
};

const menuSectionTitleStyle: CSSProperties = {
  fontSize: 11,
  fontWeight: 800,
  color: "#475569",
  padding: "6px 18px",
  letterSpacing: 0.7,
};

const museumInfoBoxStyle: CSSProperties = {
  backgroundColor: "#f9f4eb",
  border: "1.5px solid #e2d5c1",
  borderRadius: 10,
  padding: 11,
  marginBottom: 8,
};

const illustrationBadgeStyle: CSSProperties = {
  display: "inline-block",
  backgroundColor: "#fcd34d",
  color: "#78350f",
  fontSize: 11,
  fontWeight: 800,
  padding: "3px 8px",
  borderRadius: 6,
};

const helpRowStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  fontSize: 12.5,
  fontWeight: 700,
  color: "#334155",
  padding: "6px 6px",
  borderTop: "1px solid #cbd5e1",
  cursor: "pointer",
};

const rightContentWrapperStyle: CSSProperties = {
  flex: 1,
  display: "flex",
  flexDirection: "column",
  minWidth: 0,
  height: "100vh",
};

const topHeaderStyle: CSSProperties = {
  height: 48,
  backgroundColor: "#ffffff",
  borderBottom: "1.5px solid #d4cdc3",
  padding: "0 22px",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  flexShrink: 0,
};

const demoDataPillStyle: CSSProperties = {
  backgroundColor: "#fde68a",
  border: "1px solid #f59e0b",
  color: "#78350f",
  fontSize: 12,
  fontWeight: 800,
  padding: "3px 10px",
  borderRadius: 999,
};

const avatarCircleStyle: CSSProperties = {
  width: 32,
  height: 32,
  borderRadius: "50%",
  backgroundColor: "#fde8e6",
  border: "1.5px solid #eca9a4",
  color: "#80140e",
  fontWeight: 800,
  fontSize: 12,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
};

const logoutSmallBtnStyle: CSSProperties = {
  marginLeft: 6,
  padding: "5px 10px",
  backgroundColor: "#ffffff",
  color: "#0f172a",
  border: "1.5px solid #94a3b8",
  borderRadius: 6,
  fontSize: 12,
  cursor: "pointer",
  fontWeight: 700,
};

const mainScrollAreaStyle: CSSProperties = {
  flex: 1,
  padding: "12px 22px",
  overflowY: "auto",
};