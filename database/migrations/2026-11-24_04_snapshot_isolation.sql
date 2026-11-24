-- =========================================================================
-- MIGRATION 2026-11-24 (04): BẬT MỨC CÔ LẬP SNAPSHOT (đa phiên bản - Chương 3 HQTCSDL)
-- Báo cáo W2 đọc dữ liệu đã commit, không bị treo khi khách đang ghi lịch sử tham quan.
-- Không đổi dữ liệu; chạy lại nhiều lần an toàn. Chạy SAU 01–03.
-- =========================================================================
USE SmartHeritageDB;
GO

IF (SELECT snapshot_isolation_state FROM sys.databases WHERE name = DB_NAME()) = 0
    ALTER DATABASE SmartHeritageDB SET ALLOW_SNAPSHOT_ISOLATION ON;
GO
