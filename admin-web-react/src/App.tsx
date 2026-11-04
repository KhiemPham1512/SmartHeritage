import { useState, type FormEvent } from "react";
import { Archive, MapPinned, LogOut, Menu, Plus, Landmark } from "lucide-react";
import { apiFetch } from "./api";
import W3_Artifacts from "./pages/W3_Artifacts";
import W4_ArtifactForm from "./pages/W4_ArtifactForm";

export default function App() {
  const [path, setPath] = useState(window.location.pathname);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [token, setToken] = useState(localStorage.getItem("access_token") || "");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [loggingIn, setLoggingIn] = useState(false);
  const navigate = (to: string) => { window.history.pushState({}, "", to); setPath(to); setMobileOpen(false); window.scrollTo(0, 0); };
  window.onpopstate = () => setPath(window.location.pathname);
  const editingId = new URLSearchParams(window.location.search).get("id");
  const formPage = path.endsWith("/artifact-form") || path.endsWith("/artifact-form/");
  const logout = () => { localStorage.removeItem("access_token"); localStorage.removeItem("role"); localStorage.removeItem("full_name"); localStorage.removeItem("user_id"); setToken(""); };
  const login = async (event: FormEvent) => {
    event.preventDefault(); setLoginError(""); setLoggingIn(true);
    try {
      const result = await apiFetch<{access_token: string; user: {role: string; full_name: string; user_id: number}}>("/auth/login", { method: "POST", body: { email, password } });
      if (!["ADMIN", "CONTENT_MANAGER"].includes(String(result.user?.role || "").toUpperCase())) throw new Error("Tài khoản không có quyền quản trị nội dung.");
      localStorage.setItem("access_token", result.access_token);
      localStorage.setItem("role", result.user.role);
      localStorage.setItem("full_name", result.user.full_name || email);
      localStorage.setItem("user_id", String(result.user.user_id));
      setToken(result.access_token);
    } catch (e) { setLoginError(e instanceof Error ? e.message : "Đăng nhập không thành công."); }
    finally { setLoggingIn(false); }
  };
  if (!token) return <div className="login-screen"><section className="login-visual"><div className="login-brand-mark"><Landmark size={25}/></div><div className="login-brand-name">SMART HERITAGE</div><div className="login-brand-tagline">Kết nối di sản - Lan tỏa giá trị</div><div className="login-architecture" aria-hidden="true"><Landmark size={210} strokeWidth={0.55}/></div></section><section className="login-panel"><form className="login-card" onSubmit={login}><div className="login-mini-mark"><Landmark size={22}/></div><div className="eyebrow">SMART HERITAGE</div><h1>Đăng Nhập Quản Trị</h1><p>Vui lòng đăng nhập để tiếp tục quản lý hệ thống.</p>{loginError && <div className="alert error">{loginError}</div>}<label className="field">Email<input type="email" required autoComplete="username" value={email} onChange={e => setEmail(e.target.value)} placeholder="Nhập email quản trị"/></label><label className="field">Mật khẩu<input type="password" required autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Nhập mật khẩu"/></label><label className="remember-line"><input type="checkbox"/> Ghi nhớ đăng nhập</label><button className="btn btn-primary login-submit" disabled={loggingIn}>{loggingIn ? "Đang xác thực..." : "Đăng nhập  →"}</button><small className="login-help">Nếu gặp sự cố, vui lòng liên hệ quản trị viên hệ thống.</small></form></section></div>;
  return <div className="shell">
    <aside className={`sidebar ${mobileOpen ? "open" : ""}`}>
      <div className="brand"><span className="brand-mark"><Landmark size={21}/></span><span>SMART <b>HERITAGE</b><small>HỆ THỐNG QUẢN TRỊ</small></span></div>
      <div className="nav-caption">DANH MỤC QUẢN LÝ</div>
      <button className={`nav-link ${!formPage ? "active" : ""}`} onClick={() => navigate("/artifacts")}><Archive size={18}/> Quản lý hiện vật</button>
      <button className="nav-link muted" disabled><MapPinned size={18}/> Khu di tích <small>Sắp triển khai</small></button>
      <div className="sidebar-bottom"><div className="profile"><div className="avatar">{(localStorage.getItem("full_name") || "A").slice(0,1).toUpperCase()}</div><div><b>{localStorage.getItem("full_name") || "Quản trị viên"}</b><small>Content Management</small></div></div><button className="nav-link" onClick={logout}><LogOut size={17}/> Đăng xuất</button></div>
    </aside>
    {mobileOpen && <button className="scrim" aria-label="Đóng menu" onClick={() => setMobileOpen(false)}/>}
    <main className="main">
      <header className="topbar"><button className="icon-btn mobile-menu" onClick={() => setMobileOpen(!mobileOpen)}><Menu/></button><div><div className="crumb">SMART HERITAGE / CMS</div><b>{formPage ? (editingId ? "Chỉnh sửa hiện vật" : "Thêm hiện vật") : "Quản lý hiện vật"}</b></div><button className="top-add" onClick={() => navigate("/artifact-form")}><Plus size={17}/> Thêm hiện vật</button></header>
      {formPage ? <W4_ArtifactForm artifactId={editingId} onCancel={() => navigate("/artifacts")} onSaved={() => navigate("/artifacts")}/> : <W3_Artifacts onAdd={() => navigate("/artifact-form")} onEdit={(id) => navigate(`/artifact-form?id=${id}`)}/>}
    </main>
  </div>;
}
