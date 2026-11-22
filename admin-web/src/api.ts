// FILE: admin-web/src/api.ts
export const API_BASE = "http://localhost:8000";

export interface UserInfo {
  user_id: number;
  email: string;
  full_name: string;
  role: string;
  preferred_lang: string;
  auto_beacon_notify?: boolean;
  is_active?: boolean;
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = localStorage.getItem("sh_token");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((options?.headers as Record<string, string>) || {}),
  };
  if (token) {
    headers["Authorization"] = "Bearer " + token;
  }

  const response = await fetch(API_BASE + path, {
    ...options,
    headers,
  });

  if (response.status === 401 && token) {
    // Token hết hạn/không hợp lệ → về W1
    localStorage.removeItem("sh_token");
    localStorage.removeItem("sh_user");
    window.location.reload();
  }
  if (!response.ok) {
    const err = await response.json().catch(() => ({ detail: "Lỗi kết nối Server" }));
    throw new Error(err.detail || "HTTP Error " + String(response.status));
  }
  return (await response.json()) as T;
}

export const STAFF_ROLES = ["ADMIN", "CONTENT_MANAGER"];

// Tải file từ API cần token (vd. xuất CSV) — thẻ <a href> không gửi được header Authorization.
export async function downloadWithAuth(path: string, filename: string): Promise<void> {
  const token = localStorage.getItem("sh_token");
  const response = await fetch(API_BASE + path, {
    headers: token ? { Authorization: "Bearer " + token } : undefined,
  });
  if (!response.ok) throw new Error("Không tải được file (HTTP " + String(response.status) + ")");
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
  const token = localStorage.getItem("sh_token");
  const response = await fetch(API_BASE + "/api/upload/media", {
    method: "POST",
    body,
    headers: token ? { Authorization: "Bearer " + token } : undefined,
  });
  if (!response.ok) {
    const err = await response.json().catch(() => ({ detail: "Lỗi tải file" }));
    throw new Error(err.detail || "HTTP Error " + String(response.status));
  }
  return ((await response.json()) as { url: string }).url;
}