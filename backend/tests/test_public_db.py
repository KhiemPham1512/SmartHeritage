"""Test đọc DB thật (chỉ SELECT + cập nhật last_seen_at của resolve). Tự bỏ qua khi không kết nối được SQL Server."""
import pytest
from sqlalchemy import text

from app.database import engine

try:
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))
    DB_OK = True
except Exception:
    DB_OK = False

pytestmark = pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")


def test_public_artifact_list_only_published(client):
    items = client.get("/api/artifacts", params={"lang": "vi", "status": "DRAFT"}).json()
    assert all(i["status"] == "PUBLISHED" for i in items)


def test_public_beacon_list_for_simulator(client):
    items = client.get("/api/beacons/public").json()
    assert items and {"beacon_code", "uuid", "major", "minor", "tx_power"} <= set(items[0])
    assert "battery_level" not in items[0]


def test_resolve_checks_uuid(client):
    # DB của từng máy có thể đã sửa dữ liệu mẫu → chọn beacon ACTIVE đang gắn hiện vật
    live = [b for b in client.get("/api/beacons/public").json() if b["status"] == "ACTIVE" and b["artifact_code"]]
    if not live:
        pytest.skip("Không có beacon ACTIVE gắn hiện vật trong DB")
    b = live[0]
    base = {"major": b["major"], "minor": b["minor"]}
    assert client.get("/api/beacons/resolve", params=base).status_code == 200
    assert client.get("/api/beacons/resolve", params={**base, "uuid": b["uuid"].lower()}).status_code == 200
    wrong = {**base, "uuid": "00000000-0000-0000-0000-000000000000"}
    assert client.get("/api/beacons/resolve", params=wrong).status_code == 404
