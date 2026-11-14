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

  if (!response.ok) {
    const err = await response.json().catch(() => ({ detail: "Lỗi kết nối Server" }));
    throw new Error(err.detail || "HTTP Error " + String(response.status));
  }
  return (await response.json()) as T;
}