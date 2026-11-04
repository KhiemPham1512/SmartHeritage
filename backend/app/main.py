# FILE: backend/app/main.py
import os
from fastapi import FastAPI, Depends, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.database import get_db, DRIVER, DB_SERVER, DB_NAME
from app.routers import auth_router, artifact_router, beacon_router, analytics_router

app = FastAPI(
    title="Smart Heritage API",
    description="RESTful API đồng bộ 100% với Figma Web Admin (W1-W8) và Mobile App (1-10)",
    version="1.3.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

os.makedirs("static/images", exist_ok=True)
os.makedirs("static/audio", exist_ok=True)
app.mount("/static", StaticFiles(directory="static"), name="static")

# Gắn kết đầy đủ các Router theo từng Cột mốc (M1, M2, M3, M6, M7)
app.include_router(auth_router.router)
app.include_router(artifact_router.router)
app.include_router(beacon_router.router)
app.include_router(analytics_router.router)


@app.get("/api/health-db", tags=["0. System Health (M0)"])
def check_database_connection(db: Session = Depends(get_db)):
    row = db.execute(text("SELECT COUNT(*) AS total FROM dbo.Artifacts")).mappings().first()
    return {
        "status": "CONNECTED",
        "server": DB_SERVER,
        "database": DB_NAME,
        "driver": DRIVER,
        "total_artifacts": row["total"]
    }


@app.get("/api/beacons/resolve", tags=["3. iBeacons & Floor Map (M3 - W5, W6, Mobile 7)"])
def resolve_beacon_artifact(
    major: int = Query(..., description="Mã Major (VD: 1)"),
    minor: int = Query(..., description="Mã Minor (VD: 101)"),
    lang: str = Query("vi", description="Ngôn ngữ: vi hoặc en"),
    db: Session = Depends(get_db)
):
    query = text("""
        SELECT TOP 1
            b.beacon_id,
            b.beacon_code,
            b.uuid, b.major, b.minor,
            b.trigger_distance,
            a.artifact_id,
            a.artifact_code,
            a.zone_name,
            CASE WHEN :lang = 'en' THEN a.name_en ELSE a.name_vi END AS name,
            CASE WHEN :lang = 'en' THEN a.summary_en ELSE a.summary_vi END AS summary,
            CASE WHEN :lang = 'en' THEN a.description_en ELSE a.description_vi END AS description,
            a.image_url,
            CASE WHEN :lang = 'en' THEN a.audio_en_url ELSE a.audio_vi_url END AS audio_url,
            a.audio_duration_sec,
            a.map_x, a.map_y
        FROM dbo.Beacons b
        INNER JOIN dbo.Artifacts a ON b.artifact_id = a.artifact_id
        WHERE b.major = :major AND b.minor = :minor 
          AND b.status IN ('ACTIVE', 'LOW_BATTERY') 
          AND a.status = 'PUBLISHED'
    """)
    row = db.execute(query, {"major": major, "minor": minor, "lang": lang}).mappings().first()
    if not row:
        raise HTTPException(status_code=404, detail="Không tìm thấy hiện vật gắn với iBeacon này.")

    db.execute(text("UPDATE dbo.Beacons SET last_seen_at = SYSDATETIME() WHERE beacon_id = :id"), {"id": row["beacon_id"]})
    db.commit()
    return dict(row)