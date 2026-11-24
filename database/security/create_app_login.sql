-- =========================================================================
-- TÙY CHỌN (Bài 5 - Bảo mật & Phân quyền): tài khoản SQL riêng cho backend, QUYỀN TỐI THIỂU
-- Mặc định backend kết nối bằng Windows Auth/sa (toàn quyền). Lỗi SQL injection hay lộ chuỗi kết nối
-- khi đó = mất cả server. Với login này backend chỉ đọc/ghi dữ liệu và gọi SP, không tạo/xóa bảng.
--
-- Chạy trong SSMS ở chế độ SQLCMD (Query > SQLCMD Mode), sửa mật khẩu ở dòng :setvar.
-- Sau đó trong backend/.env:  USE_WINDOWS_AUTH=false, DB_USER=smartheritage_app, DB_PASSWORD=<mật khẩu>
-- Migration (ALTER/CREATE) vẫn chạy bằng tài khoản quản trị, KHÔNG bằng login này.
-- =========================================================================
:setvar APP_PASSWORD "DOI_MAT_KHAU_MANH_TRUOC_KHI_CHAY"

USE master;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'smartheritage_app')
    CREATE LOGIN smartheritage_app WITH PASSWORD = N'$(APP_PASSWORD)',
        DEFAULT_DATABASE = SmartHeritageDB, CHECK_POLICY = ON;   -- áp chính sách mật khẩu của Windows
GO

USE SmartHeritageDB;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'smartheritage_app')
    CREATE USER smartheritage_app FOR LOGIN smartheritage_app WITH DEFAULT_SCHEMA = dbo;
GO

-- Chỉ DML trên schema dbo + chạy SP/hàm. Không GRANT ALTER/CONTROL/db_owner → không DROP/ALTER được bảng.
GRANT SELECT, INSERT, UPDATE, DELETE ON SCHEMA::dbo TO smartheritage_app;
GRANT EXECUTE ON SCHEMA::dbo TO smartheritage_app;
-- Báo cáo W2 dùng mức cô lập SNAPSHOT: chỉ cần ALLOW_SNAPSHOT_ISOLATION (migration 04), không cần quyền thêm.
GO
