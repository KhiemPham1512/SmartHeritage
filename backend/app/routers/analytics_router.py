# FILE: backend/app/routers/analytics_router.py
import io
import csv
from datetime import date, datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import StreamingResponse
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.database import get_db
from app.auth import get_optional_user, require_staff
from app.schemas import (
    VisitLogCreate, FavoriteToggleRequest,
    FeedbackCreate, FeedbackVisibilityUpdate
)

router = APIRouter(prefix="/api", tags=["4. Visitor Interactions & Analytics (M6, M7 - W2, W7, Mobile 8, 9)"])


# =========================================================================
# M6: TƯƠNG TÁC KHÁCH THAM QUAN (YÊU THÍCH, LỊCH SỬ & GỬI ĐÁNH GIÁ - MOBILE MÀN 8, 9)
# =========================================================================
def identity(user: dict | None) -> int | None:
    # Danh tính lấy từ token; user_id do client gửi không được tin (tránh đọc/ghi lịch sử của người khác).
    # Không có token hợp lệ → chế độ Khách, định danh theo device_id.
    return user["user_id"] if user else None


@router.post("/visit-logs")
def record_visit_log(req: VisitLogCreate, db: Session = Depends(get_db), user: dict | None = Depends(get_optional_user)):
    row = db.execute(
        text("""
            INSERT INTO dbo.VisitLogs (user_id, device_id, artifact_id, beacon_id, lang_used, duration_seconds, is_favorite)
            OUTPUT INSERTED.log_id, INSERTED.visited_at
            VALUES (:user_id, :device_id, :artifact_id, :beacon_id, :lang_used, :duration_seconds, :is_favorite)
        """),
        {**req.model_dump(), "user_id": identity(user)}
    ).mappings().first()
    db.commit()
    return {"message": "Đã ghi nhận lịch sử tham quan", "log": dict(row)}


@router.post("/favorites/toggle")
def toggle_favorite_artifact(req: FavoriteToggleRequest, db: Session = Depends(get_db), user: dict | None = Depends(get_optional_user)):
    uid = identity(user)
    # Kiểm tra xem khách (theo user_id hoặc device_id) đã có log với hiện vật này chưa
    existing = db.execute(
        text("""
            SELECT TOP 1 log_id, is_favorite
            FROM dbo.VisitLogs
            WHERE artifact_id = :aid
              AND ((:uid IS NOT NULL AND user_id = :uid) OR (:uid IS NULL AND device_id = :did))
            ORDER BY visited_at DESC
        """),
        {"aid": req.artifact_id, "uid": uid, "did": req.device_id}
    ).mappings().first()

    if existing:
        new_state = 0 if existing["is_favorite"] else 1
        db.execute(
            text("UPDATE dbo.VisitLogs SET is_favorite = :fav WHERE log_id = :lid"),
            {"fav": new_state, "lid": existing["log_id"]}
        )
    else:
        new_state = 1
        db.execute(
            text("""
                INSERT INTO dbo.VisitLogs (user_id, device_id, artifact_id, lang_used, duration_seconds, is_favorite)
                VALUES (:uid, :did, :aid, 'vi', 30, 1)
            """),
            {"uid": uid, "did": req.device_id, "aid": req.artifact_id}
        )

    db.commit()
    return {"artifact_id": req.artifact_id, "is_favorite": bool(new_state)}


@router.get("/visit-logs/me")
def get_my_favorites_and_history(
    user_id: int = Query(None, description="Bỏ qua (giữ để tương thích app cũ): danh tính lấy từ token"),
    device_id: str = Query("android-guest-01", description="Device ID (nếu dùng chế độ Guest)"),
    lang: str = Query("vi", description="Ngôn ngữ: vi hoặc en"),
    db: Session = Depends(get_db),
    user: dict | None = Depends(get_optional_user)
):
    # Phục vụ cả 2 Tab "Yêu thích" và "Lịch sử" trên Mobile Màn 9
    rows = db.execute(
        text("""
            SELECT 
                v.log_id,
                v.artifact_id,
                a.artifact_code,
                a.zone_name,
                l.name AS name,
                l.summary AS summary,
                a.image_url,
                v.duration_seconds,
                v.is_favorite,
                v.visited_at
            FROM dbo.VisitLogs v
            INNER JOIN dbo.Artifacts a ON v.artifact_id = a.artifact_id
            LEFT JOIN dbo.fn_LocalizedArtifacts(:lang) l ON l.artifact_id = a.artifact_id
            WHERE (:uid IS NOT NULL AND v.user_id = :uid)
               OR (:uid IS NULL AND v.device_id = :did)
            ORDER BY v.visited_at DESC
        """),
        {"uid": identity(user), "did": device_id, "lang": lang}
    ).mappings().all()

    history = [dict(r) for r in rows]
    # Lọc danh sách hiện vật đã thả tim (không trùng lặp artifact_id)
    seen_fav_ids = set()
    favorites = []
    for item in history:
        if item["is_favorite"] and item["artifact_id"] not in seen_fav_ids:
            seen_fav_ids.add(item["artifact_id"])
            favorites.append(item)

    return {
        "favorites": favorites,
        "history": history
    }


@router.post("/feedbacks")
def submit_feedback(req: FeedbackCreate, db: Session = Depends(get_db), user: dict | None = Depends(get_optional_user)):
    # Phục vụ hộp thoại "Chia sẻ cảm nhận của bạn" ở Mobile Màn 8
    if not db.execute(text("SELECT 1 FROM dbo.Artifacts WHERE artifact_id = :id"), {"id": req.artifact_id}).first():
        raise HTTPException(status_code=404, detail="Không tìm thấy hiện vật")
    row = db.execute(
        text("""
            INSERT INTO dbo.Feedbacks (user_id, visitor_name, artifact_id, rating, comment, is_visible)
            OUTPUT INSERTED.feedback_id, INSERTED.visitor_name, INSERTED.rating, INSERTED.comment, INSERTED.created_at
            VALUES (:user_id, :visitor_name, :artifact_id, :rating, :comment, 1)
        """),
        {**req.model_dump(), "user_id": identity(user)}
    ).mappings().first()
    db.commit()
    return dict(row)


# =========================================================================
# M7: KIỂM DUYỆT ĐÁNH GIÁ (WEB W7) & THỐNG KÊ DASHBOARD + XUẤT BÁO CÁO (WEB W2)
# =========================================================================
FEEDBACK_SORTS = {
    "newest": "f.created_at DESC",
    "oldest": "f.created_at ASC",
    "rating_desc": "f.rating DESC, f.created_at DESC",
    "rating_asc": "f.rating ASC, f.created_at DESC",
}


@router.get("/feedbacks", dependencies=[Depends(require_staff)])
def list_feedbacks_for_moderation(
    rating: int = Query(None, ge=1, le=5, description="Lọc theo số sao: 1..5"),
    is_visible: bool = Query(None, description="Lọc trạng thái: true (Hiển thị), false (Đã ẩn)"),
    search: str = Query(None, description="Tìm kiếm theo tên khách hoặc nội dung bình luận"),
    artifact_id: int = Query(None, description="Lọc theo hiện vật"),
    sort: str = Query("newest", pattern="^(newest|oldest|rating_desc|rating_asc)$"),
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100),
    db: Session = Depends(get_db)
):
    # 4 thẻ KPI trên đầu màn hình W7 (Kiểm duyệt đánh giá & phản hồi) — tính trên toàn bộ, không theo bộ lọc
    stats = db.execute(text("""
        SELECT
            COUNT(*) AS total_feedbacks,
            ROUND(COALESCE(AVG(CASE WHEN is_visible = 1 THEN CAST(rating AS FLOAT) END), 0), 1) AS avg_rating,
            COALESCE(SUM(CASE WHEN is_visible = 1 THEN 1 ELSE 0 END), 0) AS visible_count,
            COALESCE(SUM(CASE WHEN is_visible = 0 THEN 1 ELSE 0 END), 0) AS hidden_count
        FROM dbo.Feedbacks
    """)).mappings().first()

    where = "WHERE 1=1"
    params = {}
    if rating is not None:
        where += " AND f.rating = :rating"
        params["rating"] = rating
    if is_visible is not None:
        where += " AND f.is_visible = :vis"
        params["vis"] = 1 if is_visible else 0
    if artifact_id is not None:
        where += " AND f.artifact_id = :aid"
        params["aid"] = artifact_id
    if search:
        where += " AND (f.visitor_name LIKE :s OR f.comment LIKE :s OR a.name_vi LIKE :s)"
        params["s"] = f"%{search}%"

    base = f"FROM dbo.Feedbacks f INNER JOIN dbo.Artifacts a ON f.artifact_id = a.artifact_id {where}"
    total = db.execute(text(f"SELECT COUNT(*) {base}"), params).scalar()
    items = db.execute(
        text(f"""
            SELECT f.feedback_id, f.user_id, f.visitor_name, f.artifact_id, a.artifact_code,
                   a.name_vi AS artifact_name, f.rating, f.comment, f.is_visible, f.hide_reason, f.created_at
            {base}
            ORDER BY {FEEDBACK_SORTS[sort]}
            OFFSET :off ROWS FETCH NEXT :n ROWS ONLY
        """),
        {**params, "off": (page - 1) * page_size, "n": page_size}
    ).mappings().all()

    return {
        "stats": dict(stats),
        "total": total,
        "page": page,
        "page_size": page_size,
        "items": [dict(i) for i in items]
    }


@router.put("/feedbacks/{feedback_id}/visibility", dependencies=[Depends(require_staff)])
def moderate_feedback_visibility(
    feedback_id: int,
    req: FeedbackVisibilityUpdate,
    db: Session = Depends(get_db)
):
    # Ẩn kèm lý do (hiện ở W7); duyệt lại thì xóa lý do
    updated = db.execute(
        text("UPDATE dbo.Feedbacks SET is_visible = :vis, hide_reason = :reason WHERE feedback_id = :fid"),
        {"vis": 1 if req.is_visible else 0, "reason": None if req.is_visible else (req.hide_reason or None), "fid": feedback_id}
    ).rowcount
    if not updated:
        db.rollback()
        raise HTTPException(status_code=404, detail="Không tìm thấy đánh giá")
    db.commit()
    status_str = "Hiển thị" if req.is_visible else "Đã ẩn"
    return {"message": f"Đã chuyển trạng thái bình luận #{feedback_id} sang: {status_str}"}

@router.delete("/feedbacks/{feedback_id}", dependencies=[Depends(require_staff)])
def delete_feedback(feedback_id: int, db: Session = Depends(get_db)):
    db.execute(text("DELETE FROM dbo.Feedbacks WHERE feedback_id = :fid"), {"fid": feedback_id})
    db.commit()
    return {"message": f"Đã xóa đánh giá #{feedback_id}"}


def percent_change(current: int, previous: int) -> float | None:
    if not previous:
        return None
    return round((current - previous) * 100.0 / previous, 1)


def resolve_range(date_from: date | None, date_to: date | None) -> tuple[date, date]:
    # Mặc định 30 ngày gần nhất (tính cả hôm nay)
    end = date_to or date.today()
    start = date_from or (end - timedelta(days=29))
    if start > end:
        raise HTTPException(status_code=400, detail="Ngày bắt đầu phải trước hoặc bằng ngày kết thúc")
    return start, end


# visited_at là DATETIME2 → so sánh nửa mở [from, to + 1 ngày)
IN_RANGE = "visited_at >= :d_from AND visited_at < DATEADD(day, 1, :d_to)"


@router.get("/analytics/dashboard", dependencies=[Depends(require_staff)])
def get_dashboard_analytics(
    date_from: date | None = Query(None, alias="from", description="YYYY-MM-DD, mặc định 29 ngày trước 'to'"),
    date_to: date | None = Query(None, alias="to", description="YYYY-MM-DD, mặc định hôm nay"),
    db: Session = Depends(get_db)
):
    # Phục vụ toàn bộ màn hình W2: 4 thẻ KPI + Top 10 hiện vật + Lượt khách theo ngày + Ngôn ngữ sử dụng
    start, end = resolve_range(date_from, date_to)
    span = (end - start).days + 1
    params = {"d_from": start, "d_to": end,
              "p_from": start - timedelta(days=span), "p_to": start - timedelta(days=1)}

    kpi = dict(db.execute(text(f"""
        SELECT
            (SELECT COUNT(*) FROM dbo.VisitLogs WHERE {IN_RANGE}) AS total_visits,
            (SELECT COUNT(*) FROM dbo.VisitLogs
              WHERE visited_at >= :p_from AND visited_at < DATEADD(day, 1, :p_to)) AS prev_visits,
            (SELECT COALESCE(AVG(duration_seconds), 0) FROM dbo.VisitLogs WHERE {IN_RANGE}) AS avg_duration_sec,
            (SELECT COUNT(*) FROM dbo.Artifacts) AS total_artifacts,
            (SELECT COUNT(*) FROM dbo.Artifacts WHERE status = 'PUBLISHED') AS published_artifacts,
            (SELECT COUNT(*) FROM dbo.Beacons WHERE status = 'ACTIVE') AS active_beacons,
            (SELECT COUNT(*) FROM dbo.Beacons) AS total_beacons,
            (SELECT COUNT(*) FROM dbo.Beacons WHERE status = 'LOW_BATTERY') AS low_battery_count,
            (SELECT COUNT(*) FROM dbo.Beacons WHERE status = 'LOST_SIGNAL') AS lost_signal_count,
            (SELECT ROUND(COALESCE(AVG(CAST(rating AS FLOAT)), 0), 1) FROM dbo.Feedbacks WHERE is_visible = 1) AS avg_rating,
            (SELECT COUNT(*) FROM dbo.Feedbacks WHERE is_visible = 1) AS total_reviews
    """), params).mappings().first())
    kpi["visits_change_pct"] = percent_change(kpi["total_visits"], kpi["prev_visits"])

    top_artifacts = db.execute(text("""
        SELECT TOP 10
            a.artifact_id,
            a.artifact_code,
            a.name_vi,
            a.zone_name,
            COUNT(v.log_id) AS visit_count,
            COALESCE(AVG(v.duration_seconds), 0) AS avg_duration_sec
        FROM dbo.Artifacts a
        LEFT JOIN dbo.VisitLogs v ON a.artifact_id = v.artifact_id
             AND v.visited_at >= :d_from AND v.visited_at < DATEADD(day, 1, :d_to)
        GROUP BY a.artifact_id, a.artifact_code, a.name_vi, a.zone_name
        ORDER BY visit_count DESC, a.artifact_code ASC
    """), params).mappings().all()

    daily_visits = db.execute(text(f"""
        SELECT CONVERT(VARCHAR(10), visited_at, 23) AS visit_date, COUNT(*) AS visit_count
        FROM dbo.VisitLogs
        WHERE {IN_RANGE}
        GROUP BY CONVERT(VARCHAR(10), visited_at, 23)
        ORDER BY visit_date ASC
    """), params).mappings().all()

    language_dist = db.execute(text(f"""
        SELECT UPPER(lang_used) AS language_code, COUNT(*) AS usage_count
        FROM dbo.VisitLogs
        WHERE {IN_RANGE}
        GROUP BY UPPER(lang_used)
        ORDER BY usage_count DESC
    """), params).mappings().all()

    return {
        "range": {"from": start.isoformat(), "to": end.isoformat()},
        "kpi": kpi,
        "top_artifacts": [dict(r) for r in top_artifacts],
        "daily_visits": [dict(r) for r in daily_visits],
        "language_distribution": [dict(r) for r in language_dist],
        "generated_at": datetime.now().isoformat(timespec="minutes"),
    }


@router.get("/analytics/export-csv", dependencies=[Depends(require_staff)])
def export_analytics_report_csv(
    date_from: date | None = Query(None, alias="from"),
    date_to: date | None = Query(None, alias="to"),
    db: Session = Depends(get_db)
):
    # Phục vụ nút "Xuất Excel" trên màn hình W2 (Định dạng CSV UTF-8-SIG mở trực tiếp bằng Excel)
    start, end = resolve_range(date_from, date_to)
    rows = db.execute(text("""
        SELECT 
            a.artifact_code,
            a.name_vi,
            a.zone_name,
            b.beacon_code,
            COUNT(v.log_id) AS total_visits,
            COALESCE(AVG(v.duration_seconds), 0) AS avg_duration_seconds,
            (SELECT ROUND(COALESCE(AVG(CAST(f.rating AS FLOAT)), 0), 1) FROM dbo.Feedbacks f WHERE f.artifact_id = a.artifact_id) AS avg_rating
        FROM dbo.Artifacts a
        LEFT JOIN dbo.Beacons b ON a.artifact_id = b.artifact_id
        LEFT JOIN dbo.VisitLogs v ON a.artifact_id = v.artifact_id
             AND v.visited_at >= :d_from AND v.visited_at < DATEADD(day, 1, :d_to)
        GROUP BY a.artifact_id, a.artifact_code, a.name_vi, a.zone_name, b.beacon_code
        ORDER BY total_visits DESC
    """), {"d_from": start, "d_to": end}).mappings().all()

    output = io.StringIO()
    output.write("\ufeff")  # BOM UTF-8 giúp Microsoft Excel hiển thị chuẩn tiếng Việt
    writer = csv.writer(output)
    writer.writerow([
        "Mã Hiện Vật", "Tên Hiện Vật", "Khu Trưng Bày", "Mã iBeacon",
        "Tổng Lượt Xem", "Thời Gian Nghe Trung Bình (Giây)", "Điểm Đánh Giá (Sao)"
    ])
    for r in rows:
        writer.writerow([
            r["artifact_code"], r["name_vi"], r["zone_name"], r["beacon_code"] or "Chưa gán",
            r["total_visits"], r["avg_duration_seconds"], r["avg_rating"]
        ])

    output.seek(0)
    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv; charset=utf-8",
        headers={"Content-Disposition": f"attachment; filename=SmartHeritage_Report_{start}_{end}.csv"}
    )
