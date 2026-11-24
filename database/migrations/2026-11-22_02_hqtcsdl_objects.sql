-- =========================================================================
-- MIGRATION 2026-11-22 (02): INDEX, VIEW, HÀM, STORED PROCEDURE (trọng tâm môn HQTCSDL)
-- SINH TỰ ĐỘNG từ init_schema.sql PHẦN 3–5 bởi build_objects_migration.py — KHÔNG sửa tay.
-- Dành cho DB tạo từ init_schema.sql bản cũ (thiếu SP/View/Index). Chạy lại nhiều lần an toàn.
-- Chạy SAU 2026-11-22_01_languages_translations.sql.
-- =========================================================================
USE SmartHeritageDB;
GO

-- PHẦN 3: TỐI ƯU HÓA TRUY VẤN MỨC VẬT LÝ (INDEXING)
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Beacons_Major_Minor')
    CREATE NONCLUSTERED INDEX IX_Beacons_Major_Minor ON dbo.Beacons(major, minor) INCLUDE (artifact_id, status, trigger_distance);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_VisitLogs_Artifact_Date')
    CREATE NONCLUSTERED INDEX IX_VisitLogs_Artifact_Date ON dbo.VisitLogs(artifact_id, visited_at) INCLUDE (duration_seconds, lang_used, is_favorite);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Feedbacks_Artifact_Visible')
    CREATE NONCLUSTERED INDEX IX_Feedbacks_Artifact_Visible ON dbo.Feedbacks(artifact_id, is_visible) INCLUDE (rating);
-- Dashboard W2 lọc VisitLogs theo khoảng ngày (visited_at >= ... AND visited_at < ...) → Index Seek thay vì quét bảng
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_VisitLogs_VisitedAt')
    CREATE NONCLUSTERED INDEX IX_VisitLogs_VisitedAt ON dbo.VisitLogs(visited_at) INCLUDE (duration_seconds, lang_used);
-- Mobile Màn 9 (Yêu thích & Lịch sử) và nút tim: lọc theo tài khoản HOẶC theo máy khách vãng lai, mới nhất trước
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_VisitLogs_User_Date')
    CREATE NONCLUSTERED INDEX IX_VisitLogs_User_Date ON dbo.VisitLogs(user_id, visited_at) INCLUDE (artifact_id, is_favorite, duration_seconds);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_VisitLogs_Device_Date')
    CREATE NONCLUSTERED INDEX IX_VisitLogs_Device_Date ON dbo.VisitLogs(device_id, visited_at) INCLUDE (artifact_id, is_favorite, duration_seconds);
GO

-- =========================================================================
-- PHẦN 4: MỨC KHUNG NHÌN (VIEW LEVEL ABSTRACTION - CHƯƠNG 1 HQTCSDL)
-- Che giấu các cột nhạy cảm và chỉ cung cấp dữ liệu đã xuất bản cho Du khách
-- =========================================================================
CREATE OR ALTER VIEW dbo.vw_PublicArtifacts_VI_EN
AS
SELECT
    a.artifact_id,
    a.artifact_code,
    a.zone_name,
    a.name_vi,
    a.name_en,
    a.summary_vi,
    a.summary_en,
    a.image_url,
    a.audio_vi_url,
    a.audio_en_url,
    a.audio_duration_sec,
    a.map_x,
    a.map_y,
    b.beacon_code,
    b.major,
    b.minor,
    b.trigger_distance
FROM dbo.Artifacts a
LEFT JOIN dbo.Beacons b ON a.artifact_id = b.artifact_id
WHERE a.status = 'PUBLISHED';
GO

-- Hàm bảng nội tuyến (Inline TVF): nội dung hiện vật theo ngôn ngữ, chưa dịch thì lùi về tiếng Anh.
-- API dùng chung: LEFT JOIN dbo.fn_LocalizedArtifacts(:lang) l ON l.artifact_id = a.artifact_id
CREATE OR ALTER FUNCTION dbo.fn_LocalizedArtifacts (@lang VARCHAR(10))
RETURNS TABLE
AS
RETURN
    SELECT
        a.artifact_id,
        name = CASE WHEN @lang = 'vi' THEN a.name_vi WHEN @lang = 'en' THEN a.name_en ELSE COALESCE(t.name, a.name_en) END,
        summary = CASE WHEN @lang = 'vi' THEN a.summary_vi WHEN @lang = 'en' THEN a.summary_en ELSE COALESCE(t.summary, a.summary_en) END,
        description = CASE WHEN @lang = 'vi' THEN a.description_vi WHEN @lang = 'en' THEN a.description_en ELSE COALESCE(t.description, a.description_en) END,
        audio_url = CASE WHEN @lang = 'vi' THEN a.audio_vi_url WHEN @lang = 'en' THEN a.audio_en_url ELSE COALESCE(t.audio_url, a.audio_en_url) END,
        is_translated = CASE WHEN @lang IN ('vi', 'en') OR t.artifact_id IS NOT NULL THEN 1 ELSE 0 END
    FROM dbo.Artifacts a
    LEFT JOIN dbo.ArtifactTranslations t ON t.artifact_id = a.artifact_id AND t.lang_code = @lang;
GO

-- View giám sát sức khỏe iBeacon sử dụng cấu trúc rẽ nhánh CASE...WHEN (Chương 1 T-SQL)
CREATE OR ALTER VIEW dbo.vw_BeaconHealthMonitor
AS
SELECT
    b.beacon_id,
    b.beacon_code,
    b.major,
    b.minor,
    b.battery_level,
    b.status,
    a.artifact_code,
    a.name_vi AS artifact_name,
    HealthEvaluation = CASE
        WHEN b.status = 'LOST_SIGNAL' OR b.battery_level = 0 THEN N'Khẩn cấp: Mất tín hiệu / Hết pin'
        WHEN b.battery_level < 25 THEN N'Cảnh báo: Cần thay pin sớm'
        WHEN b.battery_level BETWEEN 25 AND 60 THEN N'Bình thường: Pin trung bình'
        ELSE N'Tốt: Pin khỏe & Tín hiệu ổn định'
    END
FROM dbo.Beacons b
LEFT JOIN dbo.Artifacts a ON b.artifact_id = a.artifact_id;
GO

-- =========================================================================
-- PHẦN 5: LẬP TRÌNH T-SQL & QUẢN LÝ GIAO TÁC, CÔ LẬP, KHÓA (CHƯƠNG 1 & 2)
-- =========================================================================

-- 5.1. Giải quyết VẤN ĐỀ MẤT DỮ LIỆU CẬP NHẬT (Lost Update) bằng WITH (UPDLOCK)
-- Tình huống: 2 Admin cùng lúc gán hiện vật hoặc đổi tọa độ của cùng 1 thiết bị iBeacon
CREATE OR ALTER PROCEDURE dbo.sp_AssignBeaconSafe_UpdLock
    @beacon_id INT,
    @artifact_id INT,
    @map_x FLOAT,
    @map_y FLOAT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRAN;

    DECLARE @current_artifact INT;

    -- Nâng cấp khóa đọc (RLock) thành khóa cập nhật (ULock) bằng WITH (UPDLOCK)
    -- Ngăn giao tác thứ 2 đọc cùng lúc và ghi đè làm mất cập nhật của giao tác thứ 1
    SELECT @current_artifact = artifact_id
    FROM dbo.Beacons WITH (UPDLOCK)
    WHERE beacon_id = @beacon_id;

    IF @@ROWCOUNT = 0
    BEGIN
        ROLLBACK TRAN;
        RAISERROR(N'Không tìm thấy thiết bị iBeacon cần cập nhật.', 16, 1);
        RETURN;
    END

    -- Gỡ artifact_id khỏi Beacon cũ (nếu đang gắn ở Beacon khác) để bảo toàn ràng buộc UNIQUE
    IF @artifact_id IS NOT NULL
    BEGIN
        UPDATE dbo.Beacons WITH (UPDLOCK)
        SET artifact_id = NULL
        WHERE artifact_id = @artifact_id AND beacon_id <> @beacon_id;
    END

    -- Cập nhật gán hiện vật và tọa độ cho Beacon đích
    UPDATE dbo.Beacons
    SET artifact_id = @artifact_id,
        map_x = @map_x,
        map_y = @map_y
    WHERE beacon_id = @beacon_id;

    IF @@ERROR <> 0
    BEGIN
        IF @@TRANCOUNT > 0 ROLLBACK TRAN;
        RETURN;
    END

    -- Đồng bộ tọa độ sang bảng Artifacts để đảm bảo tính Nhất quán (Consistency - ACID)
    IF @artifact_id IS NOT NULL
    BEGIN
        UPDATE dbo.Artifacts
        SET map_x = @map_x,
            map_y = @map_y
        WHERE artifact_id = @artifact_id;

        IF @@ERROR <> 0
        BEGIN
            IF @@TRANCOUNT > 0 ROLLBACK TRAN;
            RETURN;
        END
    END

    COMMIT TRAN;
END
GO

-- 5.2. Giải quyết LOST UPDATE khi cộng dồn thời gian nghe thuyết minh (Tương tự bài toán Chuyển tiền)
CREATE OR ALTER PROCEDURE dbo.sp_AddVisitDuration_UpdLock
    @log_id INT,
    @extra_seconds INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRAN;

    DECLARE @current_duration INT;

    -- Xin khóa ULock trên dòng log_id để giao tác thứ 2 phải chờ đến khi giao tác 1 Commit
    SELECT @current_duration = duration_seconds
    FROM dbo.VisitLogs WITH (UPDLOCK)
    WHERE log_id = @log_id;

    SET @current_duration = @current_duration + @extra_seconds;

    UPDATE dbo.VisitLogs
    SET duration_seconds = @current_duration
    WHERE log_id = @log_id;

    IF @@ERROR <> 0
    BEGIN
        ROLLBACK TRAN;
        RETURN;
    END

    COMMIT TRAN;
END
GO

-- 5.3. Giải quyết VẤN ĐỀ ĐỌC DỮ LIỆU RÁC (Dirty Read) với mức cô lập READ COMMITTED
CREATE OR ALTER PROCEDURE dbo.sp_ResolveBeacon_ReadCommitted
    @major INT,
    @minor INT,
    @lang VARCHAR(10) = 'vi'
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRAN;
    -- Ngăn du khách đọc phải nội dung hiện vật mà Admin đang sửa dở dang nhưng chưa Commit (hoặc sắp Rollback)
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

    SELECT TOP 1
        b.beacon_id, b.beacon_code, b.major, b.minor, b.trigger_distance,
        a.artifact_id, a.artifact_code,
        Name = CASE WHEN @lang = 'en' THEN a.name_en ELSE a.name_vi END,
        Summary = CASE WHEN @lang = 'en' THEN a.summary_en ELSE a.summary_vi END,
        AudioUrl = CASE WHEN @lang = 'en' THEN a.audio_en_url ELSE a.audio_vi_url END
    FROM dbo.Beacons b
    INNER JOIN dbo.Artifacts a ON b.artifact_id = a.artifact_id
    WHERE b.major = @major AND b.minor = @minor AND a.status = 'PUBLISHED';

    COMMIT TRAN;
END
GO

-- 5.4. Giải quyết VẤN ĐỀ KHÔNG ĐỌC LẠI ĐƯỢC DỮ LIỆU (Non-Repeatable Read) với REPEATABLE READ
CREATE OR ALTER PROCEDURE dbo.sp_GetArtifactConsistent_RepeatableRead
    @artifact_id INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRAN;
    -- Giữ khóa đọc (Shared Lock) đến khi kết thúc giao tác, ngăn giao tác khác UPDATE làm lệch dữ liệu giữa 2 lần đọc
    SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;

    -- Lần đọc 1: Lấy thông tin cơ bản hiện vật cho phần tiêu đề báo cáo
    SELECT artifact_id, artifact_code, name_vi, status, audio_duration_sec
    FROM dbo.Artifacts
    WHERE artifact_id = @artifact_id;

    -- Lần đọc 2: Kiểm tra lại chính hiện vật đó để đối chiếu chi tiết
    SELECT artifact_id, artifact_code, name_vi, status, audio_duration_sec
    FROM dbo.Artifacts
    WHERE artifact_id = @artifact_id;

    COMMIT TRAN;
END
GO

-- 5.5. Giải quyết VẤN ĐỀ BÓNG MA (Phantom Read) với mức cô lập SERIALIZABLE
CREATE OR ALTER PROCEDURE dbo.sp_DashboardSnapshot_Serializable
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRAN;
    -- Khóa phạm vi (Range Lock), ngăn giao tác khác INSERT thêm dòng mới vào VisitLogs làm lệch tổng số thống kê
    SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

    -- Bước xử lý 1: Đếm tổng số lượt tham quan toàn hệ thống
    SELECT COUNT(*) AS total_visits_step1 FROM dbo.VisitLogs;

    -- Bước xử lý 2: Liệt kê chi tiết theo từng ngôn ngữ (Tổng các dòng phải khớp tuyệt đối với Bước 1)
    SELECT lang_used, COUNT(*) AS visit_count
    FROM dbo.VisitLogs
    GROUP BY lang_used;

    COMMIT TRAN;
END
GO

-- 5.6. Ứng dụng Vòng lặp WHILE + IF EXISTS: Tự động cấp mã hiện vật mới (HV-001, HV-002...)
-- Nếu có mã bị xóa trống ở giữa thì tự động điền vào chỗ trống đó (Áp dụng bài tập Chương 1 T-SQL)
CREATE OR ALTER PROCEDURE dbo.sp_GenerateNextArtifactCode
    @next_code VARCHAR(50) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @stt INT = 1;
    DECLARE @candidate VARCHAR(50);

    WHILE (1 = 1)
    BEGIN
        SET @candidate = 'HV-' + RIGHT('000' + CAST(@stt AS VARCHAR(10)), 3);
        IF NOT EXISTS (SELECT 1 FROM dbo.Artifacts WHERE artifact_code = @candidate)
        BEGIN
            SET @next_code = @candidate;
            BREAK;
        END
        SET @stt = @stt + 1;
    END
END
GO

-- 5.7. Ứng dụng Con trỏ CURSOR + @@FETCH_STATUS: Duyệt kiểm tra pin toàn bộ iBeacon
CREATE OR ALTER PROCEDURE dbo.sp_AuditBeaconHealth_Cursor
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @b_id INT, @b_code VARCHAR(50), @battery INT, @cur_status VARCHAR(20);
    DECLARE @new_status VARCHAR(20);

    DECLARE cursor_beacons CURSOR FOR
        SELECT beacon_id, beacon_code, battery_level, status
        FROM dbo.Beacons
        ORDER BY beacon_code ASC;

    OPEN cursor_beacons;
    FETCH NEXT FROM cursor_beacons INTO @b_id, @b_code, @battery, @cur_status;

    WHILE (@@FETCH_STATUS = 0)
    BEGIN
        SET @new_status = CASE
            WHEN @battery = 0 THEN 'LOST_SIGNAL'
            WHEN @battery < 20 THEN 'LOW_BATTERY'
            ELSE 'ACTIVE'
        END;

        IF @new_status <> @cur_status
        BEGIN
            UPDATE dbo.Beacons SET status = @new_status WHERE beacon_id = @b_id;
        END

        PRINT N'Thiết bị ' + @b_code + N' - Pin: ' + CAST(@battery AS NVARCHAR(10)) + N'% -> Trạng thái: ' + @new_status;
        FETCH NEXT FROM cursor_beacons INTO @b_id, @b_code, @battery, @cur_status;
    END

    CLOSE cursor_beacons;
    DEALLOCATE cursor_beacons;
END
GO

-- 5.8. Nút tim (Mobile Màn 6, 9): chống MẤT CẬP NHẬT và BÓNG MA cùng lúc
-- Lỗi cũ: "đọc xem đã có dòng chưa" rồi mới INSERT/UPDATE ở mức READ COMMITTED → 2 lần bấm đồng thời
-- cùng thấy "chưa có" và cùng INSERT (bóng ma), hoặc cùng đọc is_favorite = 0 rồi cùng ghi 1 (mất cập nhật).
-- UPDLOCK: khóa cập nhật U không tương thích với U → giao tác thứ 2 phải chờ ngay từ lúc đọc (không deadlock).
-- HOLDLOCK (= SERIALIZABLE cho riêng câu này): khóa cả KHOẢNG khóa chỉ mục → chặn INSERT dòng "ma" cho tới khi COMMIT.
CREATE OR ALTER PROCEDURE dbo.sp_ToggleFavorite_UpdLock
    @artifact_id INT,
    @user_id INT = NULL,
    @device_id VARCHAR(100) = NULL,
    @is_favorite BIT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON; -- lỗi giữa chừng → hủy toàn bộ giao tác (Atomicity)
    DECLARE @log_id INT, @current BIT;

    BEGIN TRAN;
    -- Tách 2 nhánh IF thay vì "(@user_id IS NULL OR ...)" để điều kiện SARGable → Index Seek trên đúng chỉ mục
    IF @user_id IS NOT NULL
        SELECT TOP 1 @log_id = log_id, @current = is_favorite
        FROM dbo.VisitLogs WITH (UPDLOCK, HOLDLOCK)
        WHERE user_id = @user_id AND artifact_id = @artifact_id
        ORDER BY visited_at DESC;
    ELSE
        SELECT TOP 1 @log_id = log_id, @current = is_favorite
        FROM dbo.VisitLogs WITH (UPDLOCK, HOLDLOCK)
        WHERE device_id = @device_id AND artifact_id = @artifact_id
        ORDER BY visited_at DESC;

    IF @log_id IS NULL
    BEGIN
        INSERT INTO dbo.VisitLogs (user_id, device_id, artifact_id, lang_used, duration_seconds, is_favorite)
        VALUES (@user_id, @device_id, @artifact_id, 'vi', 30, 1);
        SET @is_favorite = 1;
    END
    ELSE
    BEGIN
        SET @is_favorite = CASE WHEN @current = 1 THEN 0 ELSE 1 END;
        UPDATE dbo.VisitLogs SET is_favorite = @is_favorite WHERE log_id = @log_id;
    END

    COMMIT TRAN;
END
GO
