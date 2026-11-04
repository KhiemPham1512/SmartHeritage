# FILE: backend/app/routers/beacon_router.py
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas import BeaconCreate, BeaconUpdate, BeaconCoordinateUpdate, SiteMapUpdate

router = APIRouter(prefix="/api", tags=["3. iBeacons & Floor Map (M3 - W5, W6, Mobile 7)"])


@router.get("/beacons/stats")
def get_beacons_kpi_cards(db: Session = Depends(get_db)):
    # Phục vụ 4 thẻ KPI đầu màn hình W5: Tổng số Beacon, Đang hoạt động, Pin yếu, Mất tín hiệu
    row = db.execute(text("""
        SELECT
            COUNT(*) AS total_beacons,
            SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_count,
            SUM(CASE WHEN status = 'LOW_BATTERY' THEN 1 ELSE 0 END) AS low_battery_count,
            SUM(CASE WHEN status = 'LOST_SIGNAL' THEN 1 ELSE 0 END) AS lost_signal_count
        FROM dbo.Beacons
    """)).mappings().first()
    return dict(row)


@router.get("/beacons")
def get_all_beacons(
    status: str = Query(None, description="Lọc theo trạng thái: ACTIVE, LOW_BATTERY, LOST_SIGNAL"),
    search: str = Query(None, description="Tìm kiếm theo mã BC-001, UUID hoặc tên hiện vật"),
    db: Session = Depends(get_db)
):
    sql = """
        SELECT 
            b.beacon_id,
            b.beacon_code,
            b.artifact_id,
            a.artifact_code,
            a.name_vi AS artifact_name_vi,
            a.name_en AS artifact_name_en,
            b.zone_name,
            b.uuid,
            b.major,
            b.minor,
            b.tx_power,
            b.trigger_distance,
            b.battery_level,
            b.map_x,
            b.map_y,
            b.status,
            b.last_seen_at
        FROM dbo.Beacons b
        LEFT JOIN dbo.Artifacts a ON b.artifact_id = a.artifact_id
        WHERE 1=1
    """
    params = {}
    if status:
        sql += " AND b.status = :status"
        params["status"] = status
    if search:
        sql += " AND (b.beacon_code LIKE :s OR b.uuid LIKE :s OR a.name_vi LIKE :s)"
        params["s"] = f"%{search}%"

    sql += " ORDER BY b.beacon_code ASC"
    rows = db.execute(text(sql), params).mappings().all()
    return [dict(r) for r in rows]


@router.post("/beacons")
def create_beacon(req: BeaconCreate, db: Session = Depends(get_db)):
    # Kiểm tra trùng mã BC-xxx hoặc cặp (UUID, Major, Minor)
    dup = db.execute(
        text("""
            SELECT beacon_id FROM dbo.Beacons 
            WHERE beacon_code = :code OR (uuid = :uuid AND major = :major AND minor = :minor)
        """),
        {"code": req.beacon_code, "uuid": req.uuid, "major": req.major, "minor": req.minor}
    ).first()
    if dup:
        raise HTTPException(status_code=400, detail="Mã Beacon hoặc bộ số (UUID, Major, Minor) đã tồn tại")

    # Nếu gán vào hiện vật đang thuộc về Beacon khác -> tự động gỡ khỏi Beacon cũ để tránh lỗi UNIQUE
    if req.artifact_id is not None:
        db.execute(
            text("UPDATE dbo.Beacons SET artifact_id = NULL WHERE artifact_id = :aid"),
            {"aid": req.artifact_id}
        )

    row = db.execute(
        text("""
            INSERT INTO dbo.Beacons (
                beacon_code, artifact_id, zone_name, uuid, major, minor,
                tx_power, trigger_distance, battery_level, map_x, map_y, status
            )
            OUTPUT INSERTED.beacon_id, INSERTED.beacon_code, INSERTED.major, INSERTED.minor, INSERTED.status
            VALUES (
                :beacon_code, :artifact_id, :zone_name, :uuid, :major, :minor,
                :tx_power, :trigger_distance, :battery_level, :map_x, :map_y, :status
            )
        """),
        req.model_dump()
    ).mappings().first()
    db.commit()
    return dict(row)


@router.put("/beacons/{beacon_id}")
def update_beacon(beacon_id: int, req: BeaconUpdate, db: Session = Depends(get_db)):
    exists = db.execute(text("SELECT beacon_id FROM dbo.Beacons WHERE beacon_id = :id"), {"id": beacon_id}).first()
    if not exists:
        raise HTTPException(status_code=404, detail="Không tìm thấy thiết bị iBeacon")

    if req.artifact_id is not None:
        db.execute(
            text("UPDATE dbo.Beacons SET artifact_id = NULL WHERE artifact_id = :aid AND beacon_id != :bid"),
            {"aid": req.artifact_id, "bid": beacon_id}
        )

    db.execute(
        text("""
            UPDATE dbo.Beacons
            SET beacon_code = COALESCE(:beacon_code, beacon_code),
                artifact_id = COALESCE(:artifact_id, artifact_id),
                zone_name = COALESCE(:zone_name, zone_name),
                uuid = COALESCE(:uuid, uuid),
                major = COALESCE(:major, major),
                minor = COALESCE(:minor, minor),
                tx_power = COALESCE(:tx_power, tx_power),
                trigger_distance = COALESCE(:trigger_distance, trigger_distance),
                battery_level = COALESCE(:battery_level, battery_level),
                map_x = COALESCE(:map_x, map_x),
                map_y = COALESCE(:map_y, map_y),
                status = COALESCE(:status, status)
            WHERE beacon_id = :beacon_id
        """),
        {**req.model_dump(), "beacon_id": beacon_id}
    )
    db.commit()
    return {"message": f"Đã cập nhật thiết bị iBeacon #{beacon_id} thành công"}


@router.put("/beacons/{beacon_id}/coordinates")
def update_beacon_map_position(beacon_id: int, req: BeaconCoordinateUpdate, db: Session = Depends(get_db)):
    # Phục vụ nút "Cập nhật tọa độ" ở khung bên phải màn hình W6 (Bản đồ khu di tích)
    beacon = db.execute(
        text("SELECT beacon_id, artifact_id FROM dbo.Beacons WHERE beacon_id = :id"),
        {"id": beacon_id}
    ).mappings().first()
    if not beacon:
        raise HTTPException(status_code=404, detail="Không tìm thấy iBeacon")

    target_artifact_id = req.artifact_id if req.artifact_id is not None else beacon["artifact_id"]

    if req.artifact_id is not None:
        db.execute(
            text("UPDATE dbo.Beacons SET artifact_id = NULL WHERE artifact_id = :aid AND beacon_id != :bid"),
            {"aid": req.artifact_id, "bid": beacon_id}
        )

    db.execute(
        text("""
            UPDATE dbo.Beacons
            SET artifact_id = :artifact_id,
                zone_name = COALESCE(:zone_name, zone_name),
                trigger_distance = COALESCE(:trigger_distance, trigger_distance),
                map_x = :map_x,
                map_y = :map_y
            WHERE beacon_id = :beacon_id
        """),
        {
            "artifact_id": target_artifact_id,
            "zone_name": req.zone_name,
            "trigger_distance": req.trigger_distance,
            "map_x": req.map_x,
            "map_y": req.map_y,
            "beacon_id": beacon_id
        }
    )

    # Đồng bộ tọa độ (map_x, map_y) sang bảng Artifacts để Mobile Màn 7 hiển thị khớp 100%
    if target_artifact_id:
        db.execute(
            text("UPDATE dbo.Artifacts SET map_x = :x, map_y = :y WHERE artifact_id = :aid"),
            {"x": req.map_x, "y": req.map_y, "aid": target_artifact_id}
        )

    db.commit()
    return {"message": "Đã cập nhật tọa độ trên bản đồ khu di tích thành công"}


@router.delete("/beacons/{beacon_id}")
def delete_beacon(beacon_id: int, db: Session = Depends(get_db)):
    db.execute(text("DELETE FROM dbo.Beacons WHERE beacon_id = :id"), {"id": beacon_id})
    db.commit()
    return {"message": f"Đã xóa thiết bị iBeacon #{beacon_id}"}


@router.get("/sites/map")
def get_interactive_floor_map(
    site_id: int = Query(1, description="ID khu di tích"),
    lang: str = Query("vi", description="Ngôn ngữ: vi hoặc en"),
    db: Session = Depends(get_db)
):
    # Phục vụ màn hình W6 (Web) và Màn 7 (Bản đồ tương tác trên Mobile)
    site = db.execute(
        text("""
            SELECT site_id,
                   CASE WHEN :lang = 'en' THEN name_en ELSE name_vi END AS site_name,
                   name_vi, name_en, floor_name, address, map_image_url
            FROM dbo.Sites WHERE site_id = :sid
        """),
        {"sid": site_id, "lang": lang}
    ).mappings().first()

    if not site:
        raise HTTPException(status_code=404, detail="Không tìm thấy khu di tích")

    pins = db.execute(
        text("""
            SELECT 
                b.beacon_id,
                b.beacon_code,
                b.major,
                b.minor,
                b.trigger_distance,
                b.battery_level,
                b.status AS beacon_status,
                b.map_x,
                b.map_y,
                b.zone_name,
                a.artifact_id,
                a.artifact_code,
                CASE WHEN :lang = 'en' THEN a.name_en ELSE a.name_vi END AS artifact_name,
                CASE WHEN :lang = 'en' THEN a.summary_en ELSE a.summary_vi END AS summary,
                a.image_url,
                CASE WHEN :lang = 'en' THEN a.audio_en_url ELSE a.audio_vi_url END AS audio_url
            FROM dbo.Beacons b
            LEFT JOIN dbo.Artifacts a ON b.artifact_id = a.artifact_id
            ORDER BY b.beacon_code ASC
        """),
        {"lang": lang}
    ).mappings().all()

    return {
        "site": dict(site),
        "pins": [dict(p) for p in pins]
    }


@router.put("/sites/{site_id}/map")
def update_site_floorplan_image(site_id: int, req: SiteMapUpdate, db: Session = Depends(get_db)):
    db.execute(
        text("""
            UPDATE dbo.Sites
            SET floor_name = COALESCE(:floor_name, floor_name),
                map_image_url = :map_image_url
            WHERE site_id = :sid
        """),
        {"floor_name": req.floor_name, "map_image_url": req.map_image_url, "sid": site_id}
    )
    db.commit()
    return {"message": "Đã cập nhật sơ đồ mặt bằng khu di tích"}