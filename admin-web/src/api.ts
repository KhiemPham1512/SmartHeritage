// FILE: admin-web/src/api.ts
// Đặt VITE_API_BASE trong admin-web/.env.local khi backend chạy ở máy/cổng khác.
export const API_BASE: string = import.meta.env.VITE_API_BASE ?? "http://localhost:8000";

export interface UserInfo {
  user_id: number;
  email: string;
  full_name: string;
  role: string;
  preferred_lang: string;
  auto_beacon_notify?: boolean;
  is_active?: boolean;
}

export const STAFF_ROLES = ["ADMIN", "CONTENT_MANAGER"];

// ---- Phiên đăng nhập: "Ghi nhớ" → localStorage (giữ sau khi đóng trình duyệt), ngược lại sessionStorage ----
const TOKEN_KEY = "sh_token";
const USER_KEY = "sh_user";

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || "";
}

export function getSavedUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY);
  try {
    return raw ? (JSON.parse(raw) as UserInfo) : null;
  } catch {
    return null;
  }
}

export function saveSession(token: string, user: UserInfo, remember: boolean): void {
  clearSession();
  const store = remember ? localStorage : sessionStorage;
  store.setItem(TOKEN_KEY, token);
  store.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession(): void {
  for (const store of [localStorage, sessionStorage]) {
    store.removeItem(TOKEN_KEY);
    store.removeItem(USER_KEY);
  }
}

function authHeader(): Record<string, string> {
  const token = getToken();
  return token ? { Authorization: "Bearer " + token } : {};
}

async function errorMessage(response: Response, fallback: string): Promise<string> {
  const err = await response.json().catch(() => ({ detail: fallback }));
  return typeof err.detail === "string" ? err.detail : fallback + " (HTTP " + String(response.status) + ")";
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const hadToken = Boolean(getToken());
  const response = await fetch(API_BASE + path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authHeader(),
      ...((options?.headers as Record<string, string>) || {}),
    },
  });

  if (response.status === 401 && hadToken) {
    // Token hết hạn/không hợp lệ → về W1
    clearSession();
    window.location.reload();
  }
  if (!response.ok) throw new Error(await errorMessage(response, "Lỗi kết nối Server"));
  return (await response.json()) as T;
}

// Tải file từ API cần token (vd. xuất CSV) — thẻ <a href> không gửi được header Authorization.
export async function downloadWithAuth(path: string, filename: string): Promise<void> {
  const response = await fetch(API_BASE + path, { headers: authHeader() });
  if (!response.ok) throw new Error(await errorMessage(response, "Không tải được file"));
  const url = URL.createObjectURL(await response.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

// Đường dẫn media backend trả về là tương đối (/static/...) → ghép API_BASE để <img>/<audio> tải được.
export function mediaUrl(path: string | null | undefined): string {
  if (!path) return "";
  return /^https?:\/\//.test(path) ? path : API_BASE + (path.startsWith("/") ? path : "/" + path);
}

// Upload multipart: không dùng apiFetch vì nó ép Content-Type JSON.
export async function uploadMedia(file: File): Promise<string> {
  const body = new FormData();
  body.append("file", file);
  const response = await fetch(API_BASE + "/api/upload/media", { method: "POST", body, headers: authHeader() });
  if (!response.ok) throw new Error(await errorMessage(response, "Lỗi tải file"));
  return ((await response.json()) as { url: string }).url;
}
