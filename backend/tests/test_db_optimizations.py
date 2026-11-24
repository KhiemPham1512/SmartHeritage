"""Tối ưu CSDL theo kiến thức môn HQTCSDL (chỉ mục, UPDLOCK/HOLDLOCK, đa phiên bản SNAPSHOT)."""
import threading
from concurrent.futures import ThreadPoolExecutor

import pytest
from sqlalchemy import text

from app.auth import get_current_user
from app.database import engine
from app.main import app
from tests.test_public_db import DB_OK

needs_db = pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")
DEVICE = "pytest-optimize"


@pytest.fixture
def cleanup():
    yield
    with engine.begin() as c:
        c.execute(text("DELETE FROM dbo.VisitLogs WHERE device_id = :d"), {"d": DEVICE})


@pytest.fixture
def admin_client(client):
    app.dependency_overrides[get_current_user] = lambda: {"user_id": 1, "role": "ADMIN", "is_active": True}
    yield client
    app.dependency_overrides.clear()


def artifact_id(client):
    return client.get("/api/artifacts").json()[0]["artifact_id"]


@needs_db
def test_snapshot_isolation_is_allowed():
    with engine.connect() as c:
        state = c.execute(text("SELECT snapshot_isolation_state FROM sys.databases WHERE name = DB_NAME()")).scalar()
    assert state == 1, "Chạy database/migrations/2026-11-24_04_snapshot_isolation.sql"


@needs_db
def test_concurrent_favorite_toggles_never_duplicate_or_lose_updates(client, cleanup):
    # Mất cập nhật + bóng ma: nhiều lần bấm tim cùng lúc phải như chạy tuần tự (1 dòng, trạng thái = số lần lẻ/chẵn)
    aid = artifact_id(client)
    body = {"device_id": DEVICE, "artifact_id": aid}
    for burst in (8, 7):
        with ThreadPoolExecutor(burst) as ex:
            codes = list(ex.map(lambda _: client.post("/api/favorites/toggle", json=body).status_code, range(burst)))
        assert codes == [200] * burst
    with engine.connect() as c:
        rows = c.execute(text("SELECT is_favorite FROM dbo.VisitLogs WHERE device_id = :d AND artifact_id = :a"),
                         {"d": DEVICE, "a": aid}).all()
    assert len(rows) == 1, f"Sinh {len(rows)} dòng (bóng ma: 2 giao tác cùng thấy 'chưa có' rồi cùng INSERT)"
    assert rows[0].is_favorite == 1  # 15 lần bấm (lẻ) → đang thích


@needs_db
def test_dashboard_reads_committed_snapshot_without_waiting_for_writers(admin_client, cleanup):
    # Đa phiên bản: một giao tác ghi VisitLogs đang dở (giữ khóa X) không được làm treo báo cáo W2
    aid = artifact_id(admin_client)
    writer = engine.connect()
    tx = writer.begin()
    writer.execute(text("INSERT INTO dbo.VisitLogs (device_id, artifact_id, lang_used) VALUES (:d, :a, 'vi')"),
                   {"d": DEVICE, "a": aid})
    result = {}
    t = threading.Thread(target=lambda: result.update(r=admin_client.get("/api/analytics/dashboard")))
    t.start()
    t.join(timeout=8)
    blocked = t.is_alive()
    tx.rollback()  # nhả khóa để luồng kia (nếu đang chờ) kết thúc
    t.join()
    writer.close()
    assert not blocked, "Báo cáo phải chờ giao tác ghi nhả khóa → chưa dùng SNAPSHOT"
    assert result["r"].status_code == 200


@needs_db
def test_many_unassigned_beacons_but_one_beacon_per_artifact(admin_client):
    # UNIQUE thường coi mọi NULL là trùng → chỉ được 1 beacon chưa gắn. Chỉ mục UNIQUE có lọc
    # (WHERE artifact_id IS NOT NULL) cho nhập kho nhiều beacon, vẫn giữ "1 hiện vật – tối đa 1 beacon".
    codes = ["BC-PT-NULL1", "BC-PT-NULL2"]
    try:
        for i, code in enumerate(codes):
            r = admin_client.post("/api/beacons", json={"beacon_code": code, "minor": 64100 + i})
            assert r.status_code == 200, r.text
        with engine.begin() as c, pytest.raises(Exception):
            taken = c.execute(text("SELECT TOP 1 artifact_id FROM dbo.Beacons WHERE artifact_id IS NOT NULL")).scalar()
            c.execute(text("UPDATE dbo.Beacons SET artifact_id = :a WHERE beacon_code = :c"), {"a": taken, "c": codes[0]})
    finally:
        with engine.begin() as c:
            c.execute(text("DELETE FROM dbo.Beacons WHERE beacon_code IN ('BC-PT-NULL1', 'BC-PT-NULL2')"))
