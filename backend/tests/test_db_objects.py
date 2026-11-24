"""DB phải có đủ đối tượng HQTCSDL mà API dùng / đồ án trình bày. Thiếu → chạy database/migrations/*.sql."""
import pytest
from sqlalchemy import text

from app.database import engine
from tests.test_public_db import DB_OK

REQUIRED = {
    "P": ["sp_AssignBeaconSafe_UpdLock", "sp_AddVisitDuration_UpdLock", "sp_ResolveBeacon_ReadCommitted",
          "sp_GetArtifactConsistent_RepeatableRead", "sp_DashboardSnapshot_Serializable",
          "sp_GenerateNextArtifactCode", "sp_AuditBeaconHealth_Cursor", "sp_ToggleFavorite_UpdLock"],
    "V": ["vw_PublicArtifacts_VI_EN", "vw_BeaconHealthMonitor"],
    "IF": ["fn_LocalizedArtifacts"],
    "U": ["Languages", "ArtifactTranslations"],
}
INDEXES = ["IX_Beacons_Major_Minor", "IX_VisitLogs_Artifact_Date", "IX_Feedbacks_Artifact_Visible",
           "IX_VisitLogs_VisitedAt", "IX_VisitLogs_User_Date", "IX_VisitLogs_Device_Date", "UX_Beacons_Artifact"]


@pytest.mark.skipif(not DB_OK, reason="Không kết nối được SQL Server")
def test_database_has_all_course_objects():
    with engine.connect() as c:
        present = {(r.type.strip(), r.name) for r in c.execute(text("SELECT type, name FROM sys.objects"))}
        indexes = {r.name for r in c.execute(text("SELECT name FROM sys.indexes WHERE name IS NOT NULL"))}
    missing = [f"{t}:{n}" for t, names in REQUIRED.items() for n in names if (t, n) not in present]
    missing += [f"INDEX:{n}" for n in INDEXES if n not in indexes]
    assert not missing, f"Thiếu {missing} — chạy database/migrations/2026-11-22_01_*.sql rồi 02_*.sql trong SSMS"
