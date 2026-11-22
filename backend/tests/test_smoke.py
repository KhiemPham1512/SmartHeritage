from app.auth import create_access_token, decode_access_token


def test_openapi_lists_core_routes(client):
    paths = client.get("/openapi.json").json()["paths"]
    for route in ["/api/auth/login", "/api/artifacts", "/api/beacons/resolve", "/api/analytics/dashboard"]:
        assert route in paths


def test_token_roundtrip():
    payload = decode_access_token(create_access_token(7, "a@b.vn", "ADMIN"))
    assert payload["sub"] == "7" and payload["role"] == "ADMIN"
