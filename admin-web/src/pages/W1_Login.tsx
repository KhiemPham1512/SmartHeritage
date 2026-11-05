// FILE: admin-web/src/pages/W1_Login.tsx
import { useState, type FormEvent, type CSSProperties } from "react";
import { apiFetch, type UserInfo } from "../api";

const W1_ASSETS = {
  leftCircleLogo: "/logo_cong_dinh_tron.svg",
  rightSmallBadgeLogo: "/logo_cong_dinh_tron.svg",
  // Hỗ trợ tự động nhận diện cả đuôi .png và .jpg
  leftTempleBgMulti:
    "url('/smart-heritage-background.png'), url('/smart-heritage-background.jpg')",
};

interface W1LoginProps {
  onLoginSuccess: (token: string, user: UserInfo) => void;
}

interface LoginResponse {
  access_token: string;
  token_type: string;
  user: UserInfo;
}

export default function W1_Login({ onLoginSuccess }: W1LoginProps) {
  const [email, setEmail] = useState<string>("ngocanh@smartheritage.vn");
  const [password, setPassword] = useState<string>("123456");
  const [rememberMe, setRememberMe] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string>("");

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMsg("");
    setLoading(true);
    try {
      const data = await apiFetch<LoginResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      localStorage.setItem("sh_token", data.access_token);
      localStorage.setItem("sh_user", JSON.stringify(data.user));
      onLoginSuccess(data.access_token, data.user);
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : "Đăng nhập thất bại");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={splitScreenStyle}>
      {/* ================================================================= */}
      {/* CỘT TRÁI: ẢNH NỀN ĐƯỢC CĂN CHỈNH VỪA VẶN, KHÔNG BỊ PHÓNG TO QUÁ      */}
      {/* ================================================================= */}
      <div
        style={{
          ...leftPanelStyle,
          backgroundImage: W1_ASSETS.leftTempleBgMulti,
        }}
      >
        <div style={leftBrandContentStyle}>
          <img
            src={W1_ASSETS.leftCircleLogo}
            alt="Smart Heritage Logo"
            style={leftLogoImgStyle}
          />

          <h1 style={brandTitleStyle}>SMART HERITAGE</h1>

          <div style={dividerRowStyle}>
            <div style={dividerLineStyle} />
            <div style={dividerDiamondStyle} />
            <div style={dividerLineStyle} />
          </div>

          <p style={brandSubtitleStyle}>Kết nối di sản – Lan tỏa giá trị</p>
        </div>
      </div>

      {/* ================================================================= */}
      {/* CỘT PHẢI: FORM ĐĂNG NHẬP QUẢN TRỊ                                 */}
      {/* ================================================================= */}
      <div style={rightPanelStyle}>
        <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", width: "100%" }}>
          <form onSubmit={handleSubmit} style={formContainerStyle}>
            <div style={topBadgeWrapperStyle}>
              <div style={topBadgeLineStyle} />
              <img
                src={W1_ASSETS.rightSmallBadgeLogo}
                alt="Smart Heritage Badge"
                style={{ width: 32, height: 32, objectFit: "contain", opacity: 0.88 }}
              />
              <div style={topBadgeLineStyle} />
            </div>

            <h2 style={formHeadingStyle}>Đăng Nhập Quản Trị</h2>
            <p style={formSubHeadingStyle}>Vui lòng nhập thông tin để tiếp tục</p>

            {errorMsg ? <div style={errorBoxStyle}>{errorMsg}</div> : null}

            <div style={{ marginBottom: 18 }}>
              <label style={inputLabelStyle}>Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                style={textInputStyle}
                required
              />
            </div>

            <div style={{ marginBottom: 14 }}>
              <label style={inputLabelStyle}>Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={textInputStyle}
                required
              />
            </div>

            <div style={rememberRowStyle}>
              <label style={{ display: "flex", alignItems: "center", gap: 8, cursor: "pointer", fontSize: 13, fontWeight: 500, color: "#475569" }}>
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  style={{ width: 15, height: 15, accentColor: "#80140e" }}
                />
                <span>Ghi nhớ đăng nhập</span>
              </label>

              <span style={{ fontSize: 13, color: "#d97706", fontWeight: 600, cursor: "pointer" }}>
                Quên mật khẩu?
              </span>
            </div>

            <button type="submit" disabled={loading} style={loginButtonStyle}>
              <span>{loading ? "Đang xử lý..." : "Đăng Nhập"}</span>
              <span style={{ fontSize: 16, lineHeight: 1 }}>→</span>
            </button>

            <div style={systemNoteStyle}>Hệ thống quản lý di sản thông minh</div>
          </form>
        </div>

        <footer style={footerCopyStyle}>© 2026 Smart Heritage</footer>
      </div>
    </div>
  );
}

const splitScreenStyle: CSSProperties = {
  display: "flex",
  width: "100%",
  height: "100vh",
  backgroundColor: "#ffffff",
  overflow: "hidden",
};

const leftPanelStyle: CSSProperties = {
  width: "40%",
  height: "100vh",
  backgroundColor: "#6e1510",
  backgroundRepeat: "no-repeat",
  // Thay đổi từ cover sang contain để ảnh hiển thị trọn vẹn, không bị phóng to tràn viền
  backgroundSize: "100% 100%",
  backgroundPosition: "center center",
  position: "relative",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "flex-start",
  paddingTop: "10vh",
  color: "#f3d79b",
};

const leftBrandContentStyle: CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  zIndex: 2,
  padding: "0 24px",
};

const leftLogoImgStyle: CSSProperties = {
  width: 86,
  height: 86,
  objectFit: "contain",
  marginBottom: 16,
  filter: "brightness(1.2) saturate(1.05)",
};

const brandTitleStyle: CSSProperties = {
  fontSize: 28,
  fontWeight: 700,
  letterSpacing: 2.5,
  color: "#edd096",
  margin: "0 0 12px 0",
  textAlign: "center",
  fontFamily: "Georgia, 'Times New Roman', serif",
};

const dividerRowStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 10,
  width: 250,
  marginBottom: 12,
};

const dividerLineStyle: CSSProperties = {
  flex: 1,
  height: 1,
  backgroundColor: "rgba(237, 208, 150, 0.55)",
};

const dividerDiamondStyle: CSSProperties = {
  width: 7,
  height: 7,
  border: "1.2px solid #edd096",
  transform: "rotate(45deg)",
};

const brandSubtitleStyle: CSSProperties = {
  fontSize: 15,
  color: "#f8ebd0",
  fontWeight: 400,
  letterSpacing: 0.4,
};

const rightPanelStyle: CSSProperties = {
  width: "60%",
  height: "100vh",
  backgroundColor: "#ffffff",
  display: "flex",
  flexDirection: "column",
  justifyContent: "space-between",
  padding: "28px 48px",
};

const formContainerStyle: CSSProperties = {
  width: "100%",
  maxWidth: 390,
  display: "flex",
  flexDirection: "column",
};

const topBadgeWrapperStyle: CSSProperties = {
  alignSelf: "center",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 12,
  backgroundColor: "#fbf9f5",
  border: "none",
  padding: "9px 28px",
  borderRadius: 6,
  marginBottom: 16,
};

const topBadgeLineStyle: CSSProperties = {
  width: 34,
  height: 1,
  backgroundColor: "#d8c7aa",
};

const formHeadingStyle: CSSProperties = {
  fontSize: 24,
  fontWeight: 700,
  color: "#1e293b",
  textAlign: "center",
  marginBottom: 6,
};

const formSubHeadingStyle: CSSProperties = {
  fontSize: 13,
  color: "#64748b",
  fontWeight: 400,
  textAlign: "center",
  marginBottom: 24,
};

const inputLabelStyle: CSSProperties = {
  display: "block",
  fontSize: 13,
  fontWeight: 600,
  color: "#334155",
  marginBottom: 6,
};

const textInputStyle: CSSProperties = {
  width: "100%",
  height: 42,
  padding: "0 14px",
  backgroundColor: "#ffffff",
  color: "#0f172a",
  border: "1px solid #cbd5e1",
  borderRadius: 6,
  fontSize: 14,
  outline: "none",
};

const rememberRowStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  marginBottom: 22,
  marginTop: 4,
};

const loginButtonStyle: CSSProperties = {
  width: "100%",
  height: 44,
  backgroundColor: "#8b1e18",
  color: "#ffffff",
  border: "none",
  borderRadius: 6,
  fontSize: 14,
  fontWeight: 700,
  cursor: "pointer",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 8,
  boxShadow: "0 4px 12px rgba(139, 30, 24, 0.22)",
};

const systemNoteStyle: CSSProperties = {
  textAlign: "center",
  fontSize: 12,
  fontWeight: 400,
  color: "#94a3b8",
  marginTop: 48,
};

const footerCopyStyle: CSSProperties = {
  textAlign: "center",
  fontSize: 12,
  fontWeight: 400,
  color: "#94a3b8",
};

const errorBoxStyle: CSSProperties = {
  backgroundColor: "#fef2f2",
  color: "#b91c1c",
  border: "1px solid #f87171",
  padding: "10px 12px",
  borderRadius: 6,
  fontSize: 13,
  fontWeight: 600,
  marginBottom: 16,
};