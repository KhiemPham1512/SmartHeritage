import pytest

from app.auth import get_current_user
from app.main import app
from tests.test_public_db import DB_OK

needs_db = pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")


@pytest.fixture
def staff(client):
    app.dependency_overrides[get_current_user] = lambda: {"user_id": 1, "role": "CONTENT_MANAGER", "is_active": True}
    yield client
    app.dependency_overrides.clear()


def _first_artifact(client) -> int:
    return client.get("/api/artifacts").json()[0]["artifact_id"]


def test_hide_reason_is_limited_to_200_chars(staff):
    r = staff.put("/api/feedbacks/1/visibility", json={"is_visible": False, "hide_reason": "x" * 201})
    assert r.status_code == 422


def test_listing_rejects_unknown_sort(staff):
    assert staff.get("/api/feedbacks", params={"sort": "random"}).status_code == 422


@needs_db
def test_feedback_for_unknown_artifact_is_404(client):
    r = client.post("/api/feedbacks", json={"artifact_id": 999999, "rating": 5, "visitor_name": "x"})
    assert r.status_code == 404


@needs_db
def test_hide_with_reason_then_approve_clears_reason(staff):
    aid = _first_artifact(staff)
    fid = staff.post("/api/feedbacks", json={"artifact_id": aid, "rating": 1, "visitor_name": "Pytest", "comment": "Mua hàng giá rẻ"}).json()["feedback_id"]
    try:
        assert staff.put(f"/api/feedbacks/{fid}/visibility", json={"is_visible": False, "hide_reason": "Quảng cáo / Spam"}).status_code == 200
        hidden = staff.get("/api/feedbacks", params={"is_visible": False, "search": "Mua hàng giá rẻ"}).json()["items"]
        assert [(i["feedback_id"], i["hide_reason"]) for i in hidden] == [(fid, "Quảng cáo / Spam")]
        # khách không thấy bình luận đã ẩn
        assert all(f["feedback_id"] != fid for f in staff.get(f"/api/artifacts/{aid}").json()["feedbacks"])

        assert staff.put(f"/api/feedbacks/{fid}/visibility", json={"is_visible": True}).status_code == 200
        shown = staff.get("/api/feedbacks", params={"search": "Mua hàng giá rẻ"}).json()["items"]
        assert shown[0]["is_visible"] and shown[0]["hide_reason"] is None
    finally:
        staff.delete(f"/api/feedbacks/{fid}")


@needs_db
def test_hidden_feedback_does_not_change_average(staff):
    before = staff.get("/api/feedbacks").json()["stats"]["avg_rating"]
    aid = _first_artifact(staff)
    fid = staff.post("/api/feedbacks", json={"artifact_id": aid, "rating": 1, "visitor_name": "PytestSpam", "comment": "spam"}).json()["feedback_id"]
    try:
        staff.put(f"/api/feedbacks/{fid}/visibility", json={"is_visible": False, "hide_reason": "Quảng cáo / Spam"})
        stats = staff.get("/api/feedbacks").json()["stats"]
        assert stats["avg_rating"] == before
        # Dashboard và W7 dùng cùng một cách tính
        assert staff.get("/api/analytics/dashboard").json()["kpi"]["avg_rating"] == before
    finally:
        staff.delete(f"/api/feedbacks/{fid}")


@needs_db
def test_hide_missing_feedback_is_404(staff):
    assert staff.put("/api/feedbacks/999999/visibility", json={"is_visible": False}).status_code == 404


@needs_db
def test_pagination_sort_and_artifact_filter(staff):
    aid = _first_artifact(staff)
    ids = [staff.post("/api/feedbacks", json={"artifact_id": aid, "rating": r, "visitor_name": "PytestPage", "comment": f"pagetest-{r}"}).json()["feedback_id"] for r in (2, 4, 3)]
    try:
        page = staff.get("/api/feedbacks", params={"search": "pagetest-", "page_size": 2, "page": 1, "sort": "rating_desc"}).json()
        assert page["total"] == 3 and page["page_size"] == 2
        assert [i["rating"] for i in page["items"]] == [4, 3]
        page2 = staff.get("/api/feedbacks", params={"search": "pagetest-", "page_size": 2, "page": 2, "sort": "rating_desc"}).json()
        assert [i["rating"] for i in page2["items"]] == [2]
        only = staff.get("/api/feedbacks", params={"artifact_id": aid, "search": "pagetest-"}).json()
        assert only["total"] == 3 and all(i["artifact_id"] == aid for i in only["items"])
        none = staff.get("/api/feedbacks", params={"artifact_id": 999999}).json()
        assert none["total"] == 0 and none["items"] == []
        assert {"total_feedbacks", "avg_rating", "visible_count", "hidden_count"} <= set(page["stats"])
    finally:
        for fid in ids:
            staff.delete(f"/api/feedbacks/{fid}")
