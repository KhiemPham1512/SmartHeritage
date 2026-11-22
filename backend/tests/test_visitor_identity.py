"""Danh tính người xem phải lấy từ token, không tin user_id do client gửi (tránh đọc/ghi lịch sử của người khác)."""
import pytest
from sqlalchemy import text

from app.database import engine
from tests.test_public_db import DB_OK

needs_db = pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")
DEVICE = "pytest-identity"


@pytest.fixture
def cleanup():
    yield
    with engine.begin() as c:
        c.execute(text("DELETE FROM dbo.VisitLogs WHERE device_id = :d"), {"d": DEVICE})
        c.execute(text("DELETE FROM dbo.Feedbacks WHERE visitor_name = 'PytestIdentity'"))


def login(client, email="nguyenvana@email.com"):
    r = client.post("/api/auth/login", json={"email": email, "password": "123456"})
    if r.status_code != 200:
        pytest.skip("Tài khoản mẫu không đăng nhập được (đã đổi mật khẩu?)")
    return r.json()["user"]["user_id"], {"Authorization": "Bearer " + r.json()["access_token"]}


def artifact_id(client):
    return client.get("/api/artifacts").json()[0]["artifact_id"]


@needs_db
def test_visit_log_uses_user_id_from_token_not_body(client, cleanup):
    uid, auth = login(client)
    aid = artifact_id(client)
    body = {"user_id": 999999, "device_id": DEVICE, "artifact_id": aid, "duration_seconds": 5}
    assert client.post("/api/visit-logs", json=body, headers=auth).status_code == 200
    with engine.connect() as c:
        row = c.execute(text("SELECT user_id FROM dbo.VisitLogs WHERE device_id = :d"), {"d": DEVICE}).first()
    assert row.user_id == uid  # không phải 999999


@needs_db
def test_user_id_without_token_cannot_read_someone_elses_history(client, cleanup):
    uid, auth = login(client)
    aid = artifact_id(client)
    client.post("/api/visit-logs", json={"device_id": DEVICE, "artifact_id": aid, "duration_seconds": 5}, headers=auth)

    spoof = client.get("/api/visit-logs/me", params={"user_id": uid, "device_id": "pytest-someone-else"})
    assert spoof.status_code == 200 and spoof.json()["history"] == []

    mine = client.get("/api/visit-logs/me", headers=auth)
    assert any(h["artifact_id"] == aid for h in mine.json()["history"])


@needs_db
def test_guest_still_works_by_device_id(client, cleanup):
    aid = artifact_id(client)
    assert client.post("/api/visit-logs", json={"device_id": DEVICE, "artifact_id": aid, "duration_seconds": 7}).status_code == 200
    me = client.get("/api/visit-logs/me", params={"device_id": DEVICE}).json()
    assert [h["artifact_id"] for h in me["history"]] == [aid]


@needs_db
def test_favorite_toggle_follows_token_identity(client, cleanup):
    uid, auth = login(client)
    aid = artifact_id(client)

    def favorites():
        return {f["artifact_id"] for f in client.get("/api/visit-logs/me", headers=auth).json()["favorites"]}

    was_favorite = aid in favorites()  # dữ liệu mẫu có thể đã yêu thích sẵn
    flipped = client.post("/api/favorites/toggle", json={"user_id": 999999, "device_id": DEVICE, "artifact_id": aid}, headers=auth)
    try:
        assert flipped.status_code == 200 and flipped.json()["is_favorite"] is (not was_favorite)
        assert (aid in favorites()) is (not was_favorite)  # ghi theo user trong token, không theo user_id 999999
    finally:
        restored = client.post("/api/favorites/toggle", json={"device_id": DEVICE, "artifact_id": aid}, headers=auth)
        assert restored.json()["is_favorite"] is was_favorite
        assert (aid in favorites()) is was_favorite


@needs_db
def test_feedback_user_id_comes_from_token(client, cleanup):
    uid, auth = login(client)
    aid = artifact_id(client)
    r = client.post("/api/feedbacks", json={"user_id": 999999, "visitor_name": "PytestIdentity", "artifact_id": aid, "rating": 4}, headers=auth)
    assert r.status_code == 200
    with engine.connect() as c:
        row = c.execute(text("SELECT user_id FROM dbo.Feedbacks WHERE feedback_id = :i"), {"i": r.json()["feedback_id"]}).first()
    assert row.user_id == uid
