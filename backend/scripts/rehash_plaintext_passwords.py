"""Nâng cấp DB đã tạo trước 2026-11-22: băm PBKDF2 các mật khẩu đang lưu dạng thô (vd. '123456' của dữ liệu mẫu).
Mật khẩu đăng nhập không đổi. Không đụng tới hash PBKDF2 hoặc SHA-256 cũ.

Chạy từ thư mục backend:  .\\.venv\\Scripts\\python scripts\\rehash_plaintext_passwords.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sqlalchemy import text  # noqa: E402

from app.auth import hash_password  # noqa: E402
from app.database import engine  # noqa: E402


def is_hashed(value: str) -> bool:
    return value.startswith("pbkdf2_sha256$") or len(value) == 64


def main() -> None:
    with engine.begin() as conn:
        rows = conn.execute(text("SELECT user_id, email, password_hash FROM dbo.Users")).mappings().all()
        todo = [r for r in rows if not is_hashed(r["password_hash"])]
        for r in todo:
            conn.execute(text("UPDATE dbo.Users SET password_hash = :h WHERE user_id = :id"),
                         {"h": hash_password(r["password_hash"]), "id": r["user_id"]})
            print(f"Đã băm mật khẩu: {r['email']}")
    print(f"Xong: {len(todo)}/{len(rows)} tài khoản được nâng cấp.")


if __name__ == "__main__":
    main()
