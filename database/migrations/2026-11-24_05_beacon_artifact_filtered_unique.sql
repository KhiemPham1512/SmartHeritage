-- =========================================================================
-- MIGRATION 2026-11-24 (05): NHIỀU iBEACON "CHƯA GẮN HIỆN VẬT" CÙNG LÚC
-- Ràng buộc UNIQUE cũ trên Beacons.artifact_id coi mọi NULL là trùng → chỉ nhập kho được 1 beacon chưa gắn.
-- Thay bằng chỉ mục UNIQUE có lọc (WHERE artifact_id IS NOT NULL). Không đổi dữ liệu; chạy lại an toàn.
-- =========================================================================
USE SmartHeritageDB;
GO

-- Tên ràng buộc cũ do SQL Server tự sinh (UQ__Beacons__xxxx), mỗi máy một khác → tìm theo cột
DECLARE @uq SYSNAME = (
    SELECT kc.name
    FROM sys.key_constraints kc
    JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
    JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE kc.parent_object_id = OBJECT_ID('dbo.Beacons') AND kc.type = 'UQ' AND c.name = 'artifact_id'
      AND (SELECT COUNT(*) FROM sys.index_columns x
           WHERE x.object_id = kc.parent_object_id AND x.index_id = kc.unique_index_id) = 1
);
IF @uq IS NOT NULL
BEGIN
    DECLARE @sql NVARCHAR(400) = N'ALTER TABLE dbo.Beacons DROP CONSTRAINT ' + QUOTENAME(@uq);
    EXEC sp_executesql @sql;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Beacons_Artifact' AND object_id = OBJECT_ID('dbo.Beacons'))
    CREATE UNIQUE NONCLUSTERED INDEX UX_Beacons_Artifact ON dbo.Beacons(artifact_id) WHERE artifact_id IS NOT NULL;
GO
