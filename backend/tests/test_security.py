import hashlib
import os

import pytest

from app.auth import get_current_user, hash_password, verify_password
from app.main import app

STAFF_ONLY = [
    ("post", "/api/artifacts"),
    ("put", "/api/artifacts/1"),
    ("delete", "/api/artifacts/1"),
    ("get", "/api/artifacts/stats"),
    ("post", "/api/upload/media"),
    ("get", "/api/beacons"),
    ("get", "/api/beacons/stats"),
    ("post", "/api/beacons"),
    ("put", "/api/beacons/1"),
    ("put", "/api/beacons/1/coordinates"),
    ("delete", "/api/beacons/1"),
    ("put", "/api/sites/1/map"),
    ("get", "/api/feedbacks"),
    ("put", "/api/feedbacks/1/visibility"),
    ("delete", "/api/feedbacks/1"),
    ("get", "/api/analytics/dashboard"),
    ("get", "/api/analytics/export-csv"),
]
ADMIN_ONLY = [("get", "/api/users"), ("post", "/api/users"), ("put", "/api/users/1")]


def as_role(role):
    app.dependency_overrides[get_current_user] = lambda: {"user_id": 99, "role": role, "is_active": True}


@pytest.fixture(autouse=True)
def clear_overrides():
    yield
    app.dependency_overrides.clear()


@pytest.mark.parametrize("method,path", STAFF_ONLY + ADMIN_ONLY)
def test_admin_endpoints_require_token(client, method, path):
    assert getattr(client, method)(path).status_code == 401


@pytest.mark.parametrize("method,path", STAFF_ONLY + ADMIN_ONLY)
def test_visitor_is_forbidden(client, method, path):
    as_role("VISITOR")
    assert getattr(client, method)(path).status_code == 403


@pytest.mark.parametrize("method,path", ADMIN_ONLY)
def test_content_manager_cannot_manage_users(client, method, path):
    as_role("CONTENT_MANAGER")
    assert getattr(client, method)(path).status_code == 403


def test_password_hash_is_salted_and_verifiable():
    h1, h2 = hash_password("123456"), hash_password("123456")
    assert h1 != h2 and h1.startswith("pbkdf2_sha256$")
    assert verify_password("123456", h1)
    assert not verify_password("sai-mat-khau", h1)


def test_plaintext_stored_password_is_rejected():
    assert not verify_password("123456", "123456")


def test_legacy_sha256_hash_still_verifies():
    legacy = hashlib.sha256(b"123456").hexdigest()
    assert verify_password("123456", legacy)
    assert not verify_password("654321", legacy)


def test_upload_rejects_bad_extension(client):
    as_role("ADMIN")
    r = client.post("/api/upload/media", files={"file": ("x.exe", b"MZ", "application/octet-stream")})
    assert r.status_code == 400


def test_upload_rejects_oversize_image(client):
    as_role("ADMIN")
    big = b"0" * (10 * 1024 * 1024 + 1)
    r = client.post("/api/upload/media", files={"file": ("big.jpg", big, "image/jpeg")})
    assert r.status_code == 413


def test_upload_uses_unique_names(client):
    as_role("CONTENT_MANAGER")
    urls = [client.post("/api/upload/media", files={"file": ("a.png", b"png", "image/png")}).json()["url"] for _ in range(2)]
    try:
        assert urls[0] != urls[1] and all(u.startswith("/static/images/") for u in urls)
    finally:
        for u in urls:
            os.remove(u.lstrip("/"))
