# FILE: backend/app/routers/language_router.py — Ngôn ngữ hệ thống (W8) & bản dịch hiện vật (W4)
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.auth import require_admin, require_staff
from app.database import get_db
from app.schemas import LanguageUpdate, TranslationUpsert

router = APIRouter(prefix="/api", tags=["5. Languages & Translations (W4, W8, Mobile 1, 10)"])

# VI/EN lưu ở cột _vi/_en của Artifacts (sửa qua PUT /api/artifacts/{id}); bảng bản dịch chỉ cho ngôn ngữ khác
BASE_LANGS = ("vi", "en")


@router.get("/languages")
def list_languages(db: Session = Depends(get_db)):
    # Công khai: app khách lọc is_enabled; W8 dùng translated_count / total_artifacts
    rows = db.execute(text("""
        SELECT l.lang_code, l.name_native, l.name_vi, l.is_enabled, l.is_default, l.sort_order,
               CASE WHEN l.lang_code IN ('vi', 'en') THEN (SELECT COUNT(*) FROM dbo.Artifacts)
                    ELSE (SELECT COUNT(*) FROM dbo.ArtifactTranslations t WHERE t.lang_code = l.lang_code)
               END AS translated_count,
               (SELECT COUNT(*) FROM dbo.Artifacts) AS total_artifacts
        FROM dbo.Languages l
        ORDER BY l.sort_order, l.lang_code
    """)).mappings().all()
    return [{**r, "is_enabled": bool(r["is_enabled"]), "is_default": bool(r["is_default"])} for r in rows]


@router.put("/languages/{lang_code}", dependencies=[Depends(require_admin)])
def toggle_language(lang_code: str, req: LanguageUpdate, db: Session = Depends(get_db)):
    lang = db.execute(text("SELECT is_default FROM dbo.Languages WHERE lang_code = :c"), {"c": lang_code}).first()
    if not lang:
        raise HTTPException(status_code=404, detail="Không tìm thấy ngôn ngữ")
    if lang.is_default and not req.is_enabled:
        raise HTTPException(status_code=400, detail="Không thể tắt ngôn ngữ mặc định")
    db.execute(text("UPDATE dbo.Languages SET is_enabled = :e WHERE lang_code = :c"),
               {"e": 1 if req.is_enabled else 0, "c": lang_code})
    db.commit()
    return {"message": f"Đã {'bật' if req.is_enabled else 'tắt'} ngôn ngữ {lang_code}"}


def _require_artifact(db: Session, artifact_id: int) -> None:
    if not db.execute(text("SELECT 1 FROM dbo.Artifacts WHERE artifact_id = :id"), {"id": artifact_id}).first():
        raise HTTPException(status_code=404, detail="Không tìm thấy hiện vật")


def _sync_translated_langs(db: Session, artifact_id: int) -> None:
    # Giữ cột translated_langs (W3 hiển thị nhãn VI/EN/KO) khớp với bản dịch thật
    db.execute(text("""
        UPDATE dbo.Artifacts
        SET translated_langs = 'VI,EN' + COALESCE((
            SELECT ',' + STRING_AGG(UPPER(lang_code), ',') WITHIN GROUP (ORDER BY lang_code)
            FROM dbo.ArtifactTranslations WHERE artifact_id = :id
        ), '')
        WHERE artifact_id = :id
    """), {"id": artifact_id})


@router.get("/artifacts/{artifact_id}/translations", dependencies=[Depends(require_staff)])
def list_artifact_translations(artifact_id: int, db: Session = Depends(get_db)):
    _require_artifact(db, artifact_id)
    rows = db.execute(text("""
        SELECT lang_code, name, summary, description, audio_url, updated_at
        FROM dbo.ArtifactTranslations WHERE artifact_id = :id ORDER BY lang_code
    """), {"id": artifact_id}).mappings().all()
    return [dict(r) for r in rows]


@router.put("/artifacts/{artifact_id}/translations/{lang_code}", dependencies=[Depends(require_staff)])
def upsert_artifact_translation(artifact_id: int, lang_code: str, req: TranslationUpsert, db: Session = Depends(get_db)):
    if lang_code in BASE_LANGS:
        raise HTTPException(status_code=400, detail="Tiếng Việt/Anh sửa trực tiếp ở thông tin hiện vật")
    if not db.execute(text("SELECT 1 FROM dbo.Languages WHERE lang_code = :c"), {"c": lang_code}).first():
        raise HTTPException(status_code=404, detail="Ngôn ngữ chưa có trong hệ thống")
    _require_artifact(db, artifact_id)
    db.execute(text("""
        MERGE dbo.ArtifactTranslations WITH (HOLDLOCK) AS t
        USING (SELECT :aid AS artifact_id, :lang AS lang_code) AS s
        ON t.artifact_id = s.artifact_id AND t.lang_code = s.lang_code
        WHEN MATCHED THEN UPDATE SET name = :name, summary = :summary, description = :description,
                                     audio_url = :audio_url, updated_at = SYSDATETIME()
        WHEN NOT MATCHED THEN INSERT (artifact_id, lang_code, name, summary, description, audio_url)
                              VALUES (:aid, :lang, :name, :summary, :description, :audio_url);
    """), {"aid": artifact_id, "lang": lang_code, **req.model_dump()})
    _sync_translated_langs(db, artifact_id)
    db.commit()
    return {"message": f"Đã lưu bản dịch {lang_code.upper()} cho hiện vật #{artifact_id}"}


@router.delete("/artifacts/{artifact_id}/translations/{lang_code}", dependencies=[Depends(require_staff)])
def delete_artifact_translation(artifact_id: int, lang_code: str, db: Session = Depends(get_db)):
    db.execute(text("DELETE FROM dbo.ArtifactTranslations WHERE artifact_id = :id AND lang_code = :c"),
               {"id": artifact_id, "c": lang_code})
    _sync_translated_langs(db, artifact_id)
    db.commit()
    return {"message": f"Đã xóa bản dịch {lang_code.upper()} của hiện vật #{artifact_id}"}
