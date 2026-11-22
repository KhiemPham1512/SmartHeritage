import pytest

from app.auth import get_current_user
from app.main import app
from app.routers.analytics_router import percent_change
from tests.test_public_db import DB_OK


def test_percent_change():
    assert percent_change(120, 100) == 20.0
    assert percent_change(50, 100) == -50.0
    assert percent_change(5, 0) is None


@pytest.fixture
def admin_client(client):
    app.dependency_overrides[get_current_user] = lambda: {"user_id": 1, "role": "ADMIN", "is_active": True}
    yield client
    app.dependency_overrides.clear()


def test_dashboard_rejects_inverted_range(admin_client):
    r = admin_client.get("/api/analytics/dashboard", params={"from": "2026-10-10", "to": "2026-10-01"})
    assert r.status_code == 400


@pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")
def test_dashboard_all_time_is_consistent(admin_client):
    d = admin_client.get("/api/analytics/dashboard", params={"from": "2000-01-01", "to": "2100-01-01"}).json()
    k = d["kpi"]
    for key in ["total_visits", "prev_visits", "visits_change_pct", "total_artifacts", "published_artifacts",
                "active_beacons", "total_beacons", "low_battery_count", "lost_signal_count",
                "avg_rating", "total_reviews", "avg_duration_sec"]:
        assert key in k
    assert sum(x["visit_count"] for x in d["daily_visits"]) == k["total_visits"]
    assert sum(x["usage_count"] for x in d["language_distribution"]) == k["total_visits"]
    assert len(d["top_artifacts"]) <= 10
    assert d["range"] == {"from": "2000-01-01", "to": "2100-01-01"}


@pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")
def test_dashboard_empty_range(admin_client):
    d = admin_client.get("/api/analytics/dashboard", params={"from": "1990-01-01", "to": "1990-01-02"}).json()
    assert d["kpi"]["total_visits"] == 0
    assert d["daily_visits"] == [] and d["language_distribution"] == []
    assert all(a["visit_count"] == 0 for a in d["top_artifacts"])
