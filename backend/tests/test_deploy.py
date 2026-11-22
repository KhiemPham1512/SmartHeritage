"""Cấu hình triển khai: bí mật JWT, CORS, phục vụ web build sẵn, healthcheck."""
import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.auth import DEV_SECRET, resolve_secret
from app.main import mount_web, parse_origins


def test_dev_falls_back_to_default_secret():
    assert resolve_secret({}) == DEV_SECRET


def test_env_secret_is_used():
    assert resolve_secret({"JWT_SECRET": "x" * 40}) == "x" * 40


@pytest.mark.parametrize("env", [
    {"APP_ENV": "production"},
    {"APP_ENV": "production", "JWT_SECRET": DEV_SECRET},
    {"APP_ENV": "production", "JWT_SECRET": "ngan"},
])
def test_production_refuses_missing_or_weak_secret(env):
    with pytest.raises(RuntimeError):
        resolve_secret(env)


def test_production_accepts_strong_secret():
    assert resolve_secret({"APP_ENV": "production", "JWT_SECRET": "a" * 40}) == "a" * 40


def test_cors_origins():
    assert parse_origins(None, production=False) == ["*"]           # dev: mở để web :5173 gọi được
    assert parse_origins(None, production=True) == []               # production: cùng gốc, không cần CORS
    assert parse_origins(" https://a.vn , https://b.vn ", production=True) == ["https://a.vn", "https://b.vn"]


def test_healthz_does_not_need_database(client):
    r = client.get("/healthz")
    assert r.status_code == 200 and r.json() == {"status": "ok"}


def test_mount_web_serves_index_and_keeps_api_routes(tmp_path):
    (tmp_path / "index.html").write_text("<h1>Smart Heritage</h1>", encoding="utf-8")
    app = FastAPI()

    @app.get("/api/ping")
    def ping():
        return {"ok": True}

    assert mount_web(app, str(tmp_path)) is True
    c = TestClient(app)
    assert "Smart Heritage" in c.get("/").text
    assert c.get("/api/ping").json() == {"ok": True}      # API vẫn ưu tiên hơn file tĩnh
    assert c.get("/api/khong-co").status_code == 404      # đường dẫn API sai không rơi về trang chủ


def test_mount_web_without_build_is_a_noop(tmp_path):
    assert mount_web(FastAPI(), str(tmp_path / "chua-build")) is False
