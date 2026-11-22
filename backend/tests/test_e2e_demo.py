"""Kịch bản demo đầu-cuối trên SQL Server thật (cũng là bằng chứng cho báo cáo kiểm thử).

Admin tạo hiện vật + bản dịch + gán beacon → khách quét beacon → xem, yêu thích, đánh giá
→ Admin ẩn bình luận → số liệu Dashboard/CSV đổi đúng → hiện vật nháp biến mất với khách.
Mọi dữ liệu tạo ra đều bị xóa ở cuối (kể cả khi test lỗi).
"""
import random
from datetime import date

import pytest
from sqlalchemy import text

from app.database import engine
from tests.test_public_db import DB_OK

pytestmark = pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")
UUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
DEVICE = "pytest-e2e-device"


def test_demo_flow_end_to_end(client):
    login = client.post("/api/auth/login", json={"email": "ngocanh@smartheritage.vn", "password": "123456"})
    if login.status_code != 200:
        pytest.skip("Tài khoản admin mẫu không đăng nhập được (đã đổi mật khẩu?)")
    admin = {"Authorization": "Bearer " + login.json()["access_token"]}
    today = date.today().isoformat()

    def dashboard():
        return client.get("/api/analytics/dashboard", params={"from": today, "to": today}, headers=admin).json()

    artifact_id = beacon_id = None
    minor = random.randint(60000, 65000)
    try:
        # 1. Admin: mã mới (SP lấp chỗ trống) → tạo hiện vật
        code = client.get("/api/artifacts/next-code", headers=admin).json()["next_code"]
        created = client.post("/api/artifacts", headers=admin, json={
            "artifact_code": code, "zone_name": "Khu E2E", "status": "PUBLISHED",
            "name_vi": "Hiện vật E2E", "name_en": "E2E Artifact",
            "summary_vi": "Tóm tắt E2E", "summary_en": "E2E summary",
            "description_vi": "Mô tả E2E", "description_en": "E2E description",
        })
        assert created.status_code == 200, created.text
        artifact_id = created.json()["artifact_id"]

        # 2. Admin: bản dịch tiếng Hàn → nhãn ngôn ngữ cập nhật
        ko = {"name": "E2E 유물", "summary": "요약", "description": "설명"}
        assert client.put(f"/api/artifacts/{artifact_id}/translations/ko", headers=admin, json=ko).status_code == 200
        detail = client.get(f"/api/artifacts/{artifact_id}", headers=admin).json()
        assert "KO" in detail["translated_langs"]

        # 3. Admin: gán beacon cho hiện vật
        beacon = client.post("/api/beacons", headers=admin, json={
            "beacon_code": f"BC-E{minor}", "uuid": UUID, "major": 1, "minor": minor,
            "artifact_id": artifact_id, "trigger_distance": 3.0,
        })
        assert beacon.status_code == 200, beacon.text
        beacon_id = beacon.json()["beacon_id"]

        # 4. Khách (không token) quét beacon: nhận đúng nội dung theo ngôn ngữ + bán kính do Admin đặt
        vi = client.get("/api/beacons/resolve", params={"major": 1, "minor": minor, "uuid": UUID, "lang": "vi"}).json()
        assert vi["name"] == "Hiện vật E2E" and vi["trigger_distance"] == 3.0 and vi["artifact_id"] == artifact_id
        assert client.get("/api/beacons/resolve", params={"major": 1, "minor": minor, "lang": "ko"}).json()["name"] == "E2E 유물"
        assert client.get("/api/beacons/resolve", params={"major": 1, "minor": minor, "lang": "fr"}).json()["name"] == "E2E Artifact"  # chưa dịch → tiếng Anh

        # 5. Khách: xem, yêu thích, đánh giá
        before = dashboard()["kpi"]["total_visits"]
        assert client.post("/api/visit-logs", json={"device_id": DEVICE, "artifact_id": artifact_id, "beacon_id": beacon_id, "lang_used": "vi", "duration_seconds": 90}).status_code == 200
        assert client.post("/api/favorites/toggle", json={"device_id": DEVICE, "artifact_id": artifact_id}).json()["is_favorite"] is True
        mine = client.get("/api/visit-logs/me", params={"device_id": DEVICE}).json()
        assert [f["artifact_id"] for f in mine["favorites"]] == [artifact_id]
        avg_before = client.get("/api/feedbacks", headers=admin).json()["stats"]["avg_rating"]
        fb = client.post("/api/feedbacks", json={"artifact_id": artifact_id, "rating": 1, "visitor_name": "Khách E2E", "comment": "Quảng cáo spam"}).json()
        assert any(f["feedback_id"] == fb["feedback_id"] for f in client.get(f"/api/artifacts/{artifact_id}").json()["feedbacks"])

        # 6. Dashboard + CSV phản ánh lượt xem vừa tạo
        assert dashboard()["kpi"]["total_visits"] == before + 1
        csv = client.get("/api/analytics/export-csv", params={"from": today, "to": today}, headers=admin)
        assert csv.status_code == 200 and code in csv.content.decode("utf-8-sig")

        # 7. Admin ẩn bình luận kèm lý do → khách không còn thấy, điểm TB không tính
        assert client.put(f"/api/feedbacks/{fb['feedback_id']}/visibility", headers=admin, json={"is_visible": False, "hide_reason": "Quảng cáo / Spam"}).status_code == 200
        assert client.get(f"/api/artifacts/{artifact_id}").json()["feedbacks"] == []
        assert client.get("/api/feedbacks", headers=admin).json()["stats"]["avg_rating"] == avg_before

        # 8. Admin chuyển hiện vật sang NHÁP → khách không còn thấy / không quét ra được
        assert client.put(f"/api/artifacts/{artifact_id}", headers=admin, json={"status": "DRAFT"}).status_code == 200
        assert client.get("/api/beacons/resolve", params={"major": 1, "minor": minor}).status_code == 404
        assert client.get(f"/api/artifacts/{artifact_id}").status_code == 404
        assert all(a["artifact_id"] != artifact_id for a in client.get("/api/artifacts").json())
        assert any(a["artifact_id"] == artifact_id for a in client.get("/api/artifacts", headers=admin).json())  # Admin vẫn thấy nháp
    finally:
        with engine.begin() as conn:
            conn.execute(text("DELETE FROM dbo.VisitLogs WHERE device_id = :d"), {"d": DEVICE})
            if beacon_id:
                conn.execute(text("DELETE FROM dbo.Beacons WHERE beacon_id = :b"), {"b": beacon_id})
            if artifact_id:  # xóa dây chuyền: bản dịch, đánh giá
                conn.execute(text("DELETE FROM dbo.Artifacts WHERE artifact_id = :a"), {"a": artifact_id})
