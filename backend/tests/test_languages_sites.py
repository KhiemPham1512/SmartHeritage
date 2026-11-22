import pytest

from app.auth import get_current_user
from app.main import app
from tests.test_public_db import DB_OK

needs_db = pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")


def as_role(role):
    app.dependency_overrides[get_current_user] = lambda: {"user_id": 1, "role": role, "is_active": True}


@pytest.fixture(autouse=True)
def clear_overrides():
    yield
    app.dependency_overrides.clear()


@pytest.mark.parametrize("method,path,role", [
    ("put", "/api/languages/ko", None),
    ("put", "/api/languages/ko", "CONTENT_MANAGER"),
    ("get", "/api/artifacts/1/translations", None),
    ("put", "/api/artifacts/1/translations/ko", "VISITOR"),
    ("delete", "/api/artifacts/1/translations/ko", None),
    ("post", "/api/sites", "VISITOR"),
    ("put", "/api/sites/1", None),
])
def test_language_and_site_writes_are_protected(client, method, path, role):
    if role:
        as_role(role)
    assert getattr(client, method)(path).status_code in (401, 403)


@needs_db
def test_languages_list_has_default_vi(client):
    langs = client.get("/api/languages").json()
    vi = next(lang for lang in langs if lang["lang_code"] == "vi")
    assert vi["is_default"] and vi["is_enabled"]
    assert vi["translated_count"] == vi["total_artifacts"]


@needs_db
def test_cannot_disable_default_language(client):
    as_role("ADMIN")
    assert client.put("/api/languages/vi", json={"is_enabled": False}).status_code == 400


@needs_db
def test_toggle_language_roundtrip(client):
    as_role("ADMIN")
    before = next(x for x in client.get("/api/languages").json() if x["lang_code"] == "ja")["is_enabled"]
    try:
        assert client.put("/api/languages/ja", json={"is_enabled": not before}).status_code == 200
        now = next(x for x in client.get("/api/languages").json() if x["lang_code"] == "ja")["is_enabled"]
        assert now == (not before)
    finally:
        client.put("/api/languages/ja", json={"is_enabled": before})


@needs_db
def test_translation_upsert_drives_localized_content(client):
    as_role("CONTENT_MANAGER")
    art = client.get("/api/artifacts", params={"lang": "vi"}).json()[0]
    aid = art["artifact_id"]
    body = {"name": "Nom FR test", "summary": "Résumé", "description": "Description FR", "audio_url": None}
    try:
        assert client.put(f"/api/artifacts/{aid}/translations/fr", json=body).status_code == 200
        assert client.get(f"/api/artifacts/{aid}", params={"lang": "fr"}).json()["name"] == "Nom FR test"
        rows = client.get(f"/api/artifacts/{aid}/translations").json()
        assert any(r["lang_code"] == "fr" for r in rows)
        assert "FR" in client.get(f"/api/artifacts/{aid}").json()["translated_langs"]
    finally:
        client.delete(f"/api/artifacts/{aid}/translations/fr")
    detail = client.get(f"/api/artifacts/{aid}", params={"lang": "fr"}).json()
    assert detail["name"] == detail["name_en"]  # chưa dịch → lùi về tiếng Anh
    assert "FR" not in detail["translated_langs"]


@needs_db
def test_translation_rejects_vi_en_and_unknown_lang(client):
    as_role("ADMIN")
    aid = client.get("/api/artifacts", params={"lang": "vi"}).json()[0]["artifact_id"]
    body = {"name": "x", "summary": "x", "description": "x"}
    assert client.put(f"/api/artifacts/{aid}/translations/vi", json=body).status_code == 400
    assert client.put(f"/api/artifacts/{aid}/translations/xx", json=body).status_code == 404
    assert client.put("/api/artifacts/999999/translations/ko", json=body).status_code == 404


@needs_db
def test_coordinates_go_through_updlock_sp(client):
    # Gửi lại đúng tọa độ hiện có → không đổi dữ liệu, nhưng chạy qua sp_AssignBeaconSafe_UpdLock
    as_role("ADMIN")
    b = next((x for x in client.get("/api/beacons").json() if x["artifact_id"]), None)
    if not b:
        pytest.skip("Không có beacon gắn hiện vật")
    body = {"map_x": b["map_x"], "map_y": b["map_y"]}
    assert client.put(f"/api/beacons/{b['beacon_id']}/coordinates", json=body).status_code == 200
    art = client.get(f"/api/artifacts/{b['artifact_id']}").json()
    assert (art["map_x"], art["map_y"]) == (b["map_x"], b["map_y"])
    assert client.put("/api/beacons/999999/coordinates", json=body).status_code == 404


@needs_db
def test_next_artifact_code_fills_gap(client):
    as_role("CONTENT_MANAGER")
    code = client.get("/api/artifacts/next-code").json()["next_code"]
    assert code.startswith("HV-") and len(code) == 6
    existing = {a["artifact_code"] for a in client.get("/api/artifacts").json()}
    assert code not in existing
    # SP lấp chỗ trống: mọi mã nhỏ hơn đều đã tồn tại
    assert all(f"HV-{i:03d}" in existing for i in range(1, int(code[3:])))


def test_next_artifact_code_requires_staff(client):
    assert client.get("/api/artifacts/next-code").status_code == 401


@needs_db
def test_sites_public_list(client):
    sites = client.get("/api/sites").json()
    assert sites and {"site_id", "name_vi", "name_en", "floor_name"} <= set(sites[0])
