# FILE: backend/app/routers/artifact_router.py
import os
import uuid
from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas import ArtifactCreate, ArtifactUpdate
from app.auth import require_staff, get_optional_user, STAFF_ROLES

router = APIRouter(prefix="/api", tags=["2. Artifacts CMS & Media (M2 - W3, W4, Mobile 4,6,7)"])

# Đuôi file -> (thư mục static, dung lượng tối đa MB) theo giới hạn ghi trên Figma W4
UPLOAD_RULES = {
    ".jpg": ("images", 10), ".jpeg": ("images", 10), ".png": ("images", 10), ".webp": ("images", 10),
    ".mp3": ("audio", 20), ".wav": ("audio", 20), ".m4a": ("audio", 20),
    ".mp4": ("videos", 100),
}


@router.get("/artifacts/stats", dependencies=[Depends(require_staff)])
def get_artifacts_kpi_cards(db: Session = Depends(get_db)):
    row = db.execute(text("""
        SELECT
            COUNT(*) AS total_artifacts,
            SUM(CASE WHEN a.status = 'PUBLISHED' THEN 1 ELSE 0 END) AS published_count,
            SUM(CASE WHEN a.status IN ('HIDDEN', 'DRAFT') THEN 1 ELSE 0 END) AS hidden_or_draft_count,
            COUNT(b.beacon_id) AS assigned_beacon_count
        FROM dbo.Artifacts a
        LEFT JOIN dbo.Beacons b ON a.artifact_id = b.artifact_id
    """)).mappings().first()
    return dict(row)


@router.get("/artifacts")
def get_artifacts(
    lang: str = Query("vi", description="Ngôn ngữ: vi hoặc en"),
    status: str = Query(None, description="Lọc trạng thái: PUBLISHED, DRAFT, HIDDEN"),
    zone: str = Query(None, description="Lọc theo khu trưng bày"),
    search: str = Query(None, description="Tìm theo tên hoặc mã HV-001"),
    db: Session = Depends(get_db),
    user: dict | None = Depends(get_optional_user)
):
    if not user or user["role"] not in STAFF_ROLES:
        status = "PUBLISHED"  # khách/app mobile chỉ thấy hiện vật đã xuất bản
    sql = """
        SELECT 
            a.artifact_id,
            a.artifact_code,
            a.zone_name,
            l.name AS name,
            l.summary AS summary,
            l.description AS description,
            a.name_vi, a.name_en,
            a.summary_vi, a.summary_en,
            a.description_vi, a.description_en,
            a.image_url,
            l.audio_url AS audio_url,
            a.audio_vi_url, a.audio_en_url,
            a.audio_duration_sec,
            a.translated_langs,
            a.map_x, a.map_y,
            a.status,
            b.beacon_id,
            b.beacon_code,
            b.uuid, b.major, b.minor,
            b.trigger_distance,
            b.status AS beacon_status
        FROM dbo.Artifacts a
        LEFT JOIN dbo.Beacons b ON a.artifact_id = b.artifact_id
        LEFT JOIN dbo.fn_LocalizedArtifacts(:lang) l ON l.artifact_id = a.artifact_id
        WHERE 1=1
    """
    params = {"lang": lang}
    if status:
        sql += " AND a.status = :status"
        params["status"] = status
    if zone:
        sql += " AND a.zone_name LIKE :zone"
        params["zone"] = f"%{zone}%"
    if search:
        sql += " AND (a.name_vi LIKE :search OR a.name_en LIKE :search OR a.artifact_code LIKE :search)"
        params["search"] = f"%{search}%"

    sql += " ORDER BY a.artifact_code ASC"
    rows = db.execute(text(sql), params).mappings().all()
    return [dict(r) for r in rows]


@router.get("/artifacts/{artifact_id}")
def get_artifact_detail(
    artifact_id: int,
    lang: str = Query("vi", description="Ngôn ngữ: vi hoặc en"),
    db: Session = Depends(get_db),
    user: dict | None = Depends(get_optional_user)
):
    row = db.execute(
        text("""
            SELECT 
                a.*,
                l.name AS name,
                l.summary AS summary,
                l.description AS description,
                l.audio_url AS audio_url,
                b.beacon_id, b.beacon_code, b.major, b.minor, b.trigger_distance
            FROM dbo.Artifacts a
            LEFT JOIN dbo.Beacons b ON a.artifact_id = b.artifact_id
            LEFT JOIN dbo.fn_LocalizedArtifacts(:lang) l ON l.artifact_id = a.artifact_id
            WHERE a.artifact_id = :id
        """),
        {"id": artifact_id, "lang": lang}
    ).mappings().first()

    is_staff = bool(user) and user["role"] in STAFF_ROLES
    if not row or (row["status"] != "PUBLISHED" and not is_staff):
        raise HTTPException(status_code=404, detail="Không tìm thấy hiện vật")

    feedbacks = db.execute(
        text("""
            SELECT feedback_id, visitor_name, rating, comment, created_at
            FROM dbo.Feedbacks
            WHERE artifact_id = :id AND is_visible = 1
            ORDER BY created_at DESC
        """),
        {"id": artifact_id}
    ).mappings().all()

    result = dict(row)
    result["feedbacks"] = [dict(f) for f in feedbacks]
    return result


@router.post("/artifacts", dependencies=[Depends(require_staff)])
def create_artifact(req: ArtifactCreate, db: Session = Depends(get_db)):
    exists = db.execute(
        text("SELECT artifact_id FROM dbo.Artifacts WHERE artifact_code = :c"),
        {"c": req.artifact_code}
    ).first()
    if exists:
        raise HTTPException(status_code=400, detail=f"Mã hiện vật {req.artifact_code} đã tồn tại")

    row = db.execute(
        text("""
            INSERT INTO dbo.Artifacts (
                site_id, artifact_code, zone_name, name_vi, name_en,
                summary_vi, summary_en, description_vi, description_en,
                image_url, gallery_json, audio_vi_url, audio_en_url,
                audio_duration_sec, translated_langs, map_x, map_y, status
            )
            OUTPUT INSERTED.artifact_id, INSERTED.artifact_code, INSERTED.name_vi, INSERTED.status
            VALUES (
                :site_id, :artifact_code, :zone_name, :name_vi, :name_en,
                :summary_vi, :summary_en, :description_vi, :description_en,
                :image_url, :gallery_json, :audio_vi_url, :audio_en_url,
                :audio_duration_sec, :translated_langs, :map_x, :map_y, :status
            )
        """),
        req.model_dump()
    ).mappings().first()
    db.commit()
    return dict(row)


@router.put("/artifacts/{artifact_id}", dependencies=[Depends(require_staff)])
def update_artifact(artifact_id: int, req: ArtifactUpdate, db: Session = Depends(get_db)):
    exists = db.execute(text("SELECT artifact_id FROM dbo.Artifacts WHERE artifact_id = :id"), {"id": artifact_id}).first()
    if not exists:
        raise HTTPException(status_code=404, detail="Không tìm thấy hiện vật cần sửa")

    db.execute(
        text("""
            UPDATE dbo.Artifacts
            SET zone_name = COALESCE(:zone_name, zone_name),
                name_vi = COALESCE(:name_vi, name_vi),
                name_en = COALESCE(:name_en, name_en),
                summary_vi = COALESCE(:summary_vi, summary_vi),
                summary_en = COALESCE(:summary_en, summary_en),
                description_vi = COALESCE(:description_vi, description_vi),
                description_en = COALESCE(:description_en, description_en),
                image_url = COALESCE(:image_url, image_url),
                gallery_json = COALESCE(:gallery_json, gallery_json),
                audio_vi_url = COALESCE(:audio_vi_url, audio_vi_url),
                audio_en_url = COALESCE(:audio_en_url, audio_en_url),
                audio_duration_sec = COALESCE(:audio_duration_sec, audio_duration_sec),
                translated_langs = COALESCE(:translated_langs, translated_langs),
                map_x = COALESCE(:map_x, map_x),
                map_y = COALESCE(:map_y, map_y),
                status = COALESCE(:status, status)
            WHERE artifact_id = :artifact_id
        """),
        {**req.model_dump(), "artifact_id": artifact_id}
    )
    db.commit()
    return {"message": f"Đã cập nhật hiện vật #{artifact_id} thành công"}


@router.delete("/artifacts/{artifact_id}", dependencies=[Depends(require_staff)])
def delete_artifact(artifact_id: int, db: Session = Depends(get_db)):
    db.execute(text("DELETE FROM dbo.Artifacts WHERE artifact_id = :id"), {"id": artifact_id})
    db.commit()
    return {"message": f"Đã xóa hiện vật #{artifact_id}"}


@router.post("/upload/media", dependencies=[Depends(require_staff)])
def upload_media_file(file: UploadFile = File(...)):
    ext = os.path.splitext(file.filename or "")[1].lower()
    if ext not in UPLOAD_RULES:
        raise HTTPException(status_code=400, detail="Chỉ hỗ trợ ảnh (.jpg, .png, .webp), âm thanh (.mp3, .wav, .m4a) hoặc video (.mp4)")
    folder, max_mb = UPLOAD_RULES[ext]
    safe_name = f"media_{uuid.uuid4().hex}{ext}"
    save_path = os.path.join("static", folder, safe_name)
    os.makedirs(os.path.dirname(save_path), exist_ok=True)

    written = 0
    with open(save_path, "wb") as buffer:
        while chunk := file.file.read(1024 * 1024):
            written += len(chunk)
            if written > max_mb * 1024 * 1024:
                break
            buffer.write(chunk)
    if written > max_mb * 1024 * 1024:
        os.remove(save_path)
        raise HTTPException(status_code=413, detail=f"File vượt quá giới hạn {max_mb} MB")

    return {
        "filename": safe_name,
        "url": f"/static/{folder}/{safe_name}",
        "type": folder
    }
