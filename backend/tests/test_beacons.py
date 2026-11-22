import random

import pytest

from app.auth import get_current_user
from app.main import app
from tests.test_public_db import DB_OK

needs_db = pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")
UUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"


@pytest.fixture
def staff(client):
    app.dependency_overrides[get_current_user] = lambda: {"user_id": 1, "role": "CONTENT_MANAGER", "is_active": True}
    yield client
    app.dependency_overrides.clear()


def body(**over):
    base = {"beacon_code": "BC-PYTEST", "uuid": UUID, "major": 1, "minor": 60001}
    return {**base, **over}


@pytest.mark.parametrize("over", [
    {"minor": 70000},            # BLE chỉ phát được 16 bit
    {"major": -1},
    {"uuid": "khong-phai-uuid"},
    {"trigger_distance": 0},
    {"trigger_distance": 51},
    {"tx_power": 5},             # công suất đo ở 1 m luôn âm
    {"status": "BROKEN"},
])
def test_create_beacon_rejects_invalid_values(staff, over):
    assert staff.post("/api/beacons", json=body(**over)).status_code == 422


def test_update_beacon_rejects_invalid_values(staff):
    assert staff.put("/api/beacons/1", json={"minor": 70000}).status_code == 422
    assert staff.put("/api/beacons/1", json={"uuid": "abc"}).status_code == 422


@needs_db
def test_beacon_can_be_assigned_and_unassigned(staff):
    free = [a for a in staff.get("/api/artifacts").json() if not a.get("beacon_id")]
    if not free:
        pytest.skip("Mọi hiện vật đều đã có beacon")
    aid = free[0]["artifact_id"]
    code = f"BC-PT{random.randint(100, 999)}"
    created = staff.post("/api/beacons", json=body(beacon_code=code, minor=random.randint(60000, 65000)))
    assert created.status_code == 200
    bid = created.json()["beacon_id"]

    def current():
        return next(b for b in staff.get("/api/beacons").json() if b["beacon_id"] == bid)

    try:
        assert current()["artifact_id"] is None
        assert staff.put(f"/api/beacons/{bid}", json={"artifact_id": aid}).status_code == 200
        assert current()["artifact_id"] == aid
        # không gửi artifact_id → giữ nguyên
        assert staff.put(f"/api/beacons/{bid}", json={"battery_level": 77}).status_code == 200
        assert current()["artifact_id"] == aid and current()["battery_level"] == 77
        # gửi null → gỡ gán
        assert staff.put(f"/api/beacons/{bid}", json={"artifact_id": None}).status_code == 200
        assert current()["artifact_id"] is None
    finally:
        staff.delete(f"/api/beacons/{bid}")


@needs_db
def test_duplicate_beacon_signal_is_rejected(staff):
    code = f"BC-PT{random.randint(100, 999)}"
    minor = random.randint(60000, 65000)
    first = staff.post("/api/beacons", json=body(beacon_code=code, minor=minor))
    bid = first.json()["beacon_id"]
    try:
        again = staff.post("/api/beacons", json=body(beacon_code=code + "X", minor=minor))
        assert again.status_code == 400
    finally:
        staff.delete(f"/api/beacons/{bid}")
