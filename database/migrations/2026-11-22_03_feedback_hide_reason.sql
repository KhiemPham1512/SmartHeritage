-- =========================================================================
-- MIGRATION 2026-11-22 (03): LÝ DO ẨN BÌNH LUẬN (Figma W7 - "Lý do ẩn: Quảng cáo / Spam")
-- Chạy lại nhiều lần an toàn, không xóa dữ liệu. Chạy SAU 01 và 02.
-- =========================================================================
USE SmartHeritageDB;
GO

IF COL_LENGTH('dbo.Feedbacks', 'hide_reason') IS NULL
    ALTER TABLE dbo.Feedbacks ADD hide_reason NVARCHAR(200) NULL;
GO
