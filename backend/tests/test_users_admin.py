import uuid

import pytest
from sqlalchemy import text

from app.auth import get_current_user
from app.database import engine
from app.main import app
from tests.test_public_db import DB_OK

needs_db = pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")
ADMIN_ID = 777


@pytest.fixture
def admin(client):
    app.dependency_overrides[get_current_user] = lambda: {"user_id": ADMIN_ID, "role": "ADMIN", "is_active": True}
    yield client
    app.dependency_overrides.clear()


def test_admin_cannot_lock_or_demote_self(admin):
    assert admin.put(f"/api/users/{ADMIN_ID}", json={"is_active": False}).status_code == 400
    assert admin.put(f"/api/users/{ADMIN_ID}", json={"role": "VISITOR"}).status_code == 400


def test_create_rejects_unknown_role(admin):
    body = {"email": "x@example.test", "full_name": "X", "role": "SUPERUSER"}
    assert admin.post("/api/users", json=body).status_code == 400


def test_update_rejects_unknown_role(admin):
    assert admin.put("/api/users/1", json={"role": "SUPERUSER"}).status_code == 400


def test_password_must_be_at_least_6_chars(admin):
    assert admin.put("/api/users/1", json={"password": "123"}).status_code == 422


@needs_db
def test_update_missing_user_is_404(admin):
    assert admin.put("/api/users/999999", json={"full_name": "Ghost"}).status_code == 404


@needs_db
def test_create_rejects_unknown_language(admin):
    body = {"email": "lang@example.test", "full_name": "L", "role": "VISITOR", "preferred_lang": "xx"}
    assert admin.post("/api/users", json=body).status_code == 400


@needs_db
def test_admin_can_reset_password_and_user_logs_in_with_new_one(admin):
    email = f"pytest-{uuid.uuid4().hex[:8]}@example.test"
    try:
        created = admin.post("/api/users", json={"email": email, "password": "oldpass1", "full_name": "Pytest", "role": "VISITOR"})
        assert created.status_code == 200
        uid = created.json()["user_id"]
        assert admin.post("/api/auth/login", json={"email": email, "password": "oldpass1"}).status_code == 200

        assert admin.put(f"/api/users/{uid}", json={"password": "newpass2"}).status_code == 200
        assert admin.post("/api/auth/login", json={"email": email, "password": "oldpass1"}).status_code == 401
        assert admin.post("/api/auth/login", json={"email": email, "password": "newpass2"}).status_code == 200

        assert admin.put(f"/api/users/{uid}", json={"is_active": False}).status_code == 200
        assert admin.post("/api/auth/login", json={"email": email, "password": "newpass2"}).status_code == 403
    finally:
        with engine.begin() as conn:
            conn.execute(text("DELETE FROM dbo.Users WHERE email = :e"), {"e": email})
