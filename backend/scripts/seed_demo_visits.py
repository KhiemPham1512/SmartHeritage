"""Sinh lượt tham quan demo cho dashboard W2 (30 ngày gần nhất). Mọi dòng có device_id 'demo-…' để xóa lại được.

Chạy từ thư mục backend:
  .\\.venv\\Scripts\\python scripts\\seed_demo_visits.py           # thêm dữ liệu demo
  .\\.venv\\Scripts\\python scripts\\seed_demo_visits.py --clear   # xóa toàn bộ dữ liệu demo
"""
import os
import random
import sys
from datetime import datetime, timedelta

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sqlalchemy import text  # noqa: E402

from app.database import engine  # noqa: E402

DAYS = 30
LANGS = (["vi"] * 60) + (["en"] * 24) + (["ko"] * 10) + (["ja"] * 4) + (["zh"] * 2)


def main() -> None:
    with engine.begin() as conn:
        removed = conn.execute(text("DELETE FROM dbo.VisitLogs WHERE device_id LIKE 'demo-%'")).rowcount
        print(f"Đã xóa {removed} lượt demo cũ.")
        if "--clear" in sys.argv:
            return

        artifacts = conn.execute(text("""
            SELECT a.artifact_id, b.beacon_id
            FROM dbo.Artifacts a LEFT JOIN dbo.Beacons b ON b.artifact_id = a.artifact_id
            WHERE a.status = 'PUBLISHED'
        """)).mappings().all()
        if not artifacts:
            print("Không có hiện vật PUBLISHED để sinh lượt xem.")
            return

        rng = random.Random(2026)  # cố định để lần chạy nào cũng ra cùng hình dạng biểu đồ
        weights = [len(artifacts) - i for i in range(len(artifacts))]  # hiện vật đầu phổ biến hơn
        today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
        rows = []
        for d in range(DAYS):
            day = today - timedelta(days=DAYS - 1 - d)
            for _ in range(rng.randint(15, 40) + d):  # xu hướng tăng dần như Figma
                a = rng.choices(artifacts, weights=weights)[0]
                rows.append({
                    "device_id": f"demo-{rng.randint(1, 400):03d}",
                    "artifact_id": a["artifact_id"],
                    "beacon_id": a["beacon_id"],
                    "lang": rng.choice(LANGS),
                    "dur": rng.randint(30, 300),
                    "at": day + timedelta(minutes=rng.randint(8 * 60, 17 * 60)),
                })
        conn.execute(text("""
            INSERT INTO dbo.VisitLogs (device_id, artifact_id, beacon_id, lang_used, duration_seconds, is_favorite, visited_at)
            VALUES (:device_id, :artifact_id, :beacon_id, :lang, :dur, 0, :at)
        """), rows)
        print(f"Đã thêm {len(rows)} lượt tham quan demo trong {DAYS} ngày.")


if __name__ == "__main__":
    main()
