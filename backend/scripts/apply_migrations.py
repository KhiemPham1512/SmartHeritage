"""Nâng cấp DB đã có sẵn: chạy lần lượt database/migrations/*.sql (idempotent, không xóa dữ liệu).
Tương đương mở từng file trong SSMS và bấm F5.

Chạy từ thư mục backend:  .\\.venv\\Scripts\\python scripts\\apply_migrations.py
"""
import glob
import os
import re
import sys

BACKEND = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, BACKEND)

from app.database import engine  # noqa: E402

MIGRATIONS = os.path.join(BACKEND, "..", "database", "migrations", "*.sql")


def main() -> None:
    raw = engine.raw_connection()
    raw.driver_connection.autocommit = True  # đặt trên kết nối pyodbc thật; proxy của pool không nhận
    cur = raw.driver_connection.cursor()
    try:
        for path in sorted(glob.glob(MIGRATIONS)):
            with open(path, encoding="utf-8") as f:
                batches = [b for b in re.split(r"^\s*GO\s*$", f.read(), flags=re.M | re.I) if b.strip()]
            for batch in batches:
                cur.execute(batch)
                while cur.nextset():
                    pass
            print(f"Đã chạy {os.path.basename(path)} ({len(batches)} lô lệnh)")
    finally:
        raw.close()


if __name__ == "__main__":
    main()
