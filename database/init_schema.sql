-- =========================================================================
-- FILE: database/init_schema.sql
-- HỆ QUẢN TRỊ CSDL: MICROSOFT SQL SERVER (T-SQL)
-- ĐỒNG BỘ 100% VỚI FIGMA (W1-W8, MOBILE 1-10) & GIÁO TRÌNH HỆ QUẢN TRỊ CSDL
-- =========================================================================

USE master;
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'SmartHeritageDB')
BEGIN
    CREATE DATABASE SmartHeritageDB;
END
GO

USE SmartHeritageDB;
GO

-- =========================================================================
-- PHẦN 1: DỌN DẸP ĐỐI TƯỢNG CŨ ĐỂ CHẠY LẠI AN TOÀN (IDEMPOTENT SCRIPT)
-- =========================================================================
IF OBJECT_ID('dbo.sp_AssignBeaconSafe_UpdLock', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_AssignBeaconSafe_UpdLock;
IF OBJECT_ID('dbo.sp_AddVisitDuration_UpdLock', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_AddVisitDuration_UpdLock;
IF OBJECT_ID('dbo.sp_ResolveBeacon_ReadCommitted', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_ResolveBeacon_ReadCommitted;
IF OBJECT_ID('dbo.sp_GetArtifactConsistent_RepeatableRead', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_GetArtifactConsistent_RepeatableRead;
IF OBJECT_ID('dbo.sp_DashboardSnapshot_Serializable', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_DashboardSnapshot_Serializable;
IF OBJECT_ID('dbo.sp_GenerateNextArtifactCode', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_GenerateNextArtifactCode;
IF OBJECT_ID('dbo.sp_AuditBeaconHealth_Cursor', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_AuditBeaconHealth_Cursor;

IF OBJECT_ID('dbo.vw_PublicArtifacts_VI_EN', 'V') IS NOT NULL DROP VIEW dbo.vw_PublicArtifacts_VI_EN;
IF OBJECT_ID('dbo.vw_BeaconHealthMonitor', 'V') IS NOT NULL DROP VIEW dbo.vw_BeaconHealthMonitor;

IF OBJECT_ID('dbo.Feedbacks', 'U') IS NOT NULL DROP TABLE dbo.Feedbacks;
IF OBJECT_ID('dbo.VisitLogs', 'U') IS NOT NULL DROP TABLE dbo.VisitLogs;
IF OBJECT_ID('dbo.Beacons', 'U') IS NOT NULL DROP TABLE dbo.Beacons;
IF OBJECT_ID('dbo.Artifacts', 'U') IS NOT NULL DROP TABLE dbo.Artifacts;
IF OBJECT_ID('dbo.Sites', 'U') IS NOT NULL DROP TABLE dbo.Sites;
IF OBJECT_ID('dbo.Users', 'U') IS NOT NULL DROP TABLE dbo.Users;
GO

-- =========================================================================
-- PHẦN 2: ĐỊNH NGHĨA MỨC LUẬN LÝ (DDL - 6 BẢNG CỐT LÕI & RÀNG BUỘC TOÀN VẸN)
-- =========================================================================

-- 1. BẢNG Users (Khớp với W1, W8 và Mobile 2, 3, 10)
CREATE TABLE dbo.Users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name NVARCHAR(150) NOT NULL,
    avatar_url VARCHAR(500) NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'VISITOR'
        CONSTRAINT CK_Users_Role CHECK (role IN ('ADMIN', 'CONTENT_MANAGER', 'VISITOR')),
    preferred_lang VARCHAR(10) NOT NULL DEFAULT 'vi'
        CONSTRAINT CK_Users_Lang CHECK (preferred_lang IN ('vi', 'en', 'ko', 'ja', 'zh')),
    auto_beacon_notify BIT NOT NULL DEFAULT 1,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 2. BẢNG Sites (Khớp với W6 và Mobile 4, 7)
CREATE TABLE dbo.Sites (
    site_id INT IDENTITY(1,1) PRIMARY KEY,
    name_vi NVARCHAR(255) NOT NULL,
    name_en NVARCHAR(255) NOT NULL,
    description_vi NVARCHAR(MAX) NULL,
    description_en NVARCHAR(MAX) NULL,
    address NVARCHAR(500) NULL,
    floor_name NVARCHAR(150) NULL DEFAULT N'Tầng 1 - Khu trưng bày chính',
    map_image_url VARCHAR(500) NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 3. BẢNG Artifacts (Khớp với W3, W4 và Mobile 4, 5, 6, 7)
CREATE TABLE dbo.Artifacts (
    artifact_id INT IDENTITY(1,1) PRIMARY KEY,
    site_id INT NOT NULL DEFAULT 1,
    artifact_code VARCHAR(50) NOT NULL UNIQUE,
    zone_name NVARCHAR(150) NOT NULL DEFAULT N'Khu trưng bày Đông Sơn - Tầng 1',
    name_vi NVARCHAR(255) NOT NULL,
    name_en NVARCHAR(255) NOT NULL,
    summary_vi NVARCHAR(500) NOT NULL,
    summary_en NVARCHAR(500) NOT NULL,
    description_vi NVARCHAR(MAX) NOT NULL,
    description_en NVARCHAR(MAX) NOT NULL,
    image_url VARCHAR(500) NULL,
    gallery_json NVARCHAR(MAX) NULL,
    audio_vi_url VARCHAR(500) NULL,
    audio_en_url VARCHAR(500) NULL,
    audio_duration_sec INT NOT NULL DEFAULT 225
        CONSTRAINT CK_Artifacts_Duration CHECK (audio_duration_sec > 0),
    translated_langs VARCHAR(50) NOT NULL DEFAULT 'VI,EN',
    map_x FLOAT NOT NULL DEFAULT 340.0,
    map_y FLOAT NOT NULL DEFAULT 215.0,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'
        CONSTRAINT CK_Artifacts_Status CHECK (status IN ('PUBLISHED', 'DRAFT', 'HIDDEN')),
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Artifacts_Sites FOREIGN KEY (site_id) REFERENCES dbo.Sites(site_id) ON DELETE CASCADE
);
GO

-- 4. BẢNG Beacons (Khớp với W5, W6 và Mobile 4, 5)
CREATE TABLE dbo.Beacons (
    beacon_id INT IDENTITY(1,1) PRIMARY KEY,
    beacon_code VARCHAR(50) NOT NULL UNIQUE,
    artifact_id INT NULL UNIQUE,
    zone_name NVARCHAR(150) NULL DEFAULT N'Tầng 1 - Phòng Đông Sơn',
    uuid VARCHAR(36) NOT NULL DEFAULT 'FDA50693-A4E2-4FB1-AFCF-C6EB07647825',
    major INT NOT NULL DEFAULT 1
        CONSTRAINT CK_Beacons_Major CHECK (major >= 0),
    minor INT NOT NULL
        CONSTRAINT CK_Beacons_Minor CHECK (minor >= 0),
    tx_power INT NOT NULL DEFAULT -59,
    trigger_distance FLOAT NOT NULL DEFAULT 2.0
        CONSTRAINT CK_Beacons_Distance CHECK (trigger_distance > 0 AND trigger_distance <= 50.0),
    battery_level INT NOT NULL DEFAULT 100
        CONSTRAINT CK_Beacons_Battery CHECK (battery_level BETWEEN 0 AND 100),
    map_x FLOAT NOT NULL DEFAULT 340.0,
    map_y FLOAT NOT NULL DEFAULT 215.0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT CK_Beacons_Status CHECK (status IN ('ACTIVE', 'LOW_BATTERY', 'LOST_SIGNAL')),
    last_seen_at DATETIME2 NULL DEFAULT SYSDATETIME(),
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_Beacon_Signal UNIQUE (uuid, major, minor),
    CONSTRAINT FK_Beacons_Artifacts FOREIGN KEY (artifact_id) REFERENCES dbo.Artifacts(artifact_id) ON DELETE SET NULL
);
GO

-- 5. BẢNG VisitLogs (Khớp với W2 và Mobile 9 - Yêu thích & Lịch sử)
CREATE TABLE dbo.VisitLogs (
    log_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL,
    device_id VARCHAR(100) NULL,
    artifact_id INT NOT NULL,
    beacon_id INT NULL,
    lang_used VARCHAR(10) NOT NULL DEFAULT 'vi',
    duration_seconds INT NOT NULL DEFAULT 60
        CONSTRAINT CK_VisitLogs_Duration CHECK (duration_seconds >= 0),
    is_favorite BIT NOT NULL DEFAULT 0,
    visited_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_VisitLogs_Users FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id) ON DELETE SET NULL,
    CONSTRAINT FK_VisitLogs_Artifacts FOREIGN KEY (artifact_id) REFERENCES dbo.Artifacts(artifact_id) ON DELETE CASCADE
);
GO

-- 6. BẢNG Feedbacks (Khớp với W7 và Mobile 6, 8 - Chia sẻ cảm nhận)
CREATE TABLE dbo.Feedbacks (
    feedback_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL,
    visitor_name NVARCHAR(150) NOT NULL DEFAULT N'Khách tham quan',
    artifact_id INT NOT NULL,
    rating TINYINT NOT NULL
        CONSTRAINT CK_Feedbacks_Rating CHECK (rating BETWEEN 1 AND 5),
    comment NVARCHAR(1000) NULL,
    is_visible BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Feedbacks_Users FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id) ON DELETE SET NULL,
    CONSTRAINT FK_Feedbacks_Artifacts FOREIGN KEY (artifact_id) REFERENCES dbo.Artifacts(artifact_id) ON DELETE CASCADE
);
GO

-- =========================================================================
-- PHẦN 3: TỐI ƯU HÓA TRUY VẤN MỨC VẬT LÝ (INDEXING)
-- =========================================================================
CREATE NONCLUSTERED INDEX IX_Beacons_Major_Minor ON dbo.Beacons(major, minor) INCLUDE (artifact_id, status, trigger_distance);
CREATE NONCLUSTERED INDEX IX_VisitLogs_Artifact_Date ON dbo.VisitLogs(artifact_id, visited_at) INCLUDE (duration_seconds, lang_used, is_favorite);
CREATE NONCLUSTERED INDEX IX_Feedbacks_Artifact_Visible ON dbo.Feedbacks(artifact_id, is_visible) INCLUDE (rating);
GO

-- =========================================================================
-- PHẦN 4: MỨC KHUNG NHÌN (VIEW LEVEL ABSTRACTION - CHƯƠNG 1 HQTCSDL)
-- Che giấu các cột nhạy cảm và chỉ cung cấp dữ liệu đã xuất bản cho Du khách
-- =========================================================================
CREATE VIEW dbo.vw_PublicArtifacts_VI_EN
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

-- View giám sát sức khỏe iBeacon sử dụng cấu trúc rẽ nhánh CASE...WHEN (Chương 1 T-SQL)
CREATE VIEW dbo.vw_BeaconHealthMonitor
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
CREATE PROCEDURE dbo.sp_AssignBeaconSafe_UpdLock
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
CREATE PROCEDURE dbo.sp_AddVisitDuration_UpdLock
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
CREATE PROCEDURE dbo.sp_ResolveBeacon_ReadCommitted
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
CREATE PROCEDURE dbo.sp_GetArtifactConsistent_RepeatableRead
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
CREATE PROCEDURE dbo.sp_DashboardSnapshot_Serializable
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
CREATE PROCEDURE dbo.sp_GenerateNextArtifactCode
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
CREATE PROCEDURE dbo.sp_AuditBeaconHealth_Cursor
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

-- =========================================================================
-- PHẦN 6: NẠP DỮ LIỆU MẪU TRONG GIAO TÁC AN TOÀN (SEED DATA TRANSACTION)
-- =========================================================================
BEGIN TRAN;

INSERT INTO dbo.Users (email, password_hash, full_name, role, preferred_lang, is_active)
VALUES 
('ngocanh@smartheritage.vn', '123456', N'Ngọc Anh (Admin)', 'ADMIN', 'vi', 1),
('minhduc@smartheritage.vn', '123456', N'Trần Minh Đức', 'CONTENT_MANAGER', 'vi', 1),
('lethuha@smartheritage.vn', '123456', N'Lê Thu Hà', 'CONTENT_MANAGER', 'en', 1),
('nguyenvana@email.com', '123456', N'Nguyễn Văn A', 'VISITOR', 'vi', 1),
('john.davis@gmail.com', '123456', N'John Davis', 'VISITOR', 'en', 1);

INSERT INTO dbo.Sites (name_vi, name_en, description_vi, description_en, address, floor_name, map_image_url)
VALUES (
    N'Bảo tàng Lịch sử Quốc gia',
    N'Vietnam National Museum of History',
    N'Nơi lưu giữ và trưng bày các bảo vật quốc gia trải dài từ thời tiền sử đến triều đại nhà Nguyễn.',
    N'Preserving and exhibiting national treasures from prehistoric times to the Nguyen Dynasty.',
    N'Số 1 Tràng Tiền, Hoàn Kiếm, Hà Nội / Số 2 Nguyễn Bỉnh Khiêm, Quận 1, TP.HCM',
    N'Tầng 1 - Khu trưng bày chính',
    '/static/images/floorplan_tang1.png'
);

INSERT INTO dbo.Artifacts 
(site_id, artifact_code, zone_name, name_vi, name_en, summary_vi, summary_en, description_vi, description_en, image_url, audio_vi_url, audio_en_url, audio_duration_sec, translated_langs, map_x, map_y, status)
VALUES
(1, 'HV-001', N'Khu trưng bày Đông Sơn - Tầng 1',
 N'Trống đồng Ngọc Lũ', N'Ngoc Lu Bronze Drum',
 N'Trống đồng Ngọc Lũ — Dấu ấn văn hóa Đông Sơn rực rỡ cách đây hơn 2.500 năm.',
 N'Ngoc Lu Bronze Drum — The pinnacle of ancient Dong Son bronze casting culture.',
 N'Trống đồng Ngọc Lũ là một trong những chiếc trống đồng cổ nhất và đẹp nhất thuộc nền văn hóa Đông Sơn (niên đại khoảng 2.500 năm trước). Mặt trống đúc nổi ngôi sao 14 cánh cùng các họa tiết chim Lạc và cảnh sinh hoạt của cư dân Việt cổ.',
 N'The Ngoc Lu Bronze Drum is the most iconic artifact of the Dong Son culture (dating back 2,500 years). Its tympanum features a 14-pointed star surrounded by Lac birds and scenes of ancient communal life.',
 '/static/images/hv_001.jpg', '/static/audio/ngoc-lu-thuyet-minh-vi.mp3', '/static/audio/ngoc-lu-thuyet-minh-en.mp3', 225, 'VI,EN,KO,JA', 340, 215, 'PUBLISHED'),

(1, 'HV-002', N'Phòng Gốm Cổ - Tầng 1',
 N'Bình gốm Chu Đậu', N'Chu Dau Ceramic Vase',
 N'Tuyệt tác gốm hoa lam Việt Nam thế kỷ XV với nét vẽ thủ công tinh xảo.',
 N'A 15th-century Vietnamese blue-and-white ceramic masterpiece.',
 N'Bình gốm hoa lam Chu Đậu (Hải Dương) có niên đại thế kỷ XV, nổi tiếng với lớp men trắng ngà trong trẻo và họa tiết hoa cúc, chim phượng vẽ bằng màu xanh coban.',
 N'Dating from the 15th century, Chu Dau blue-and-white ceramics are celebrated for their ivory glaze and intricate cobalt-blue floral motifs.',
 '/static/images/hv_002.jpg', '/static/audio/chu-dau-vi.mp3', '/static/audio/chu-dau-en.mp3', 180, 'VI,EN', 520, 180, 'PUBLISHED'),

(1, 'HV-003', N'Khu Chăm Pa - Tầng 1',
 N'Tượng Apsara', N'Champa Apsara Sandstone Relief',
 N'Phù điêu vũ nữ Apsara bằng đá sa thạch tiêu biểu cho nghệ thuật điêu khắc Chăm Pa.',
 N'Sandstone relief of an Apsara celestial dancer from the Champa Kingdom.',
 N'Tượng vũ nữ Apsara thuộc phong cách nghệ thuật Trà Kiệu (thế kỷ X), thể hiện đường nét uyển chuyển và nghệ thuật chạm khắc đá sa thạch đỉnh cao của vương quốc Chăm Pa.',
 N'Carved in the 10th-century Tra Kieu style, this sandstone Apsara dancer embodies the grace and sculptural mastery of the Champa civilization.',
 '/static/images/hv_003.jpg', '/static/audio/apsara-vi.mp3', '/static/audio/apsara-en.mp3', 195, 'VI,EN,JA', 280, 420, 'PUBLISHED'),

(1, 'HV-004', N'Khu trưng bày Đông Sơn - Tầng 1',
 N'Thạp đồng Đào Thịnh', N'Dao Thinh Bronze Jar',
 N'Bảo vật quốc gia phản ánh tín ngưỡng phồn thực và kỹ thuật đúc đồng Đông Sơn.',
 N'National treasure reflecting fertility beliefs and Dong Son bronze mastery.',
 N'Thạp đồng Đào Thịnh có kích thước đồ sộ và nắp đúc tượng các cặp đôi nam nữ, phản ánh đời sống tâm linh và ước vọng sinh sôi nảy nở của cư dân nông nghiệp lúa nước.',
 N'The monumental Dao Thinh bronze jar features figurines on its lid, symbolizing fertility rituals of ancient wet-rice agricultural communities.',
 '/static/images/hv_004.jpg', '/static/audio/dao-thinh-vi.mp3', '/static/audio/dao-thinh-en.mp3', 210, 'VI,EN', 410, 310, 'PUBLISHED'),

(1, 'HV-005', N'Phòng Triều Nguyễn - Tầng 1',
 N'Ấn vàng Triều Nguyễn', N'Nguyen Dynasty Imperial Golden Seal',
 N'Bảo vật hoàng cung biểu trưng cho quyền lực tối cao của các vị vua nhà Nguyễn.',
 N'Imperial golden seal symbolizing the supreme authority of the Nguyen Emperors.',
 N'Chiếc ấn vàng đúc dưới triều vua Minh Mạng năm 1823 với núm ấn hình rồng uốn lượn, được sử dụng trong các đại lễ và văn kiện trọng đại của triều đình Huế.',
 N'Cast in 1823 during Emperor Minh Mang reign, this dragon-knobbed golden seal was used for the most solemn imperial decrees of the Hue Court.',
 '/static/images/hv_005.jpg', '/static/audio/an-vang-vi.mp3', '/static/audio/an-vang-en.mp3', 240, 'VI,EN,KO', 610, 450, 'PUBLISHED');

INSERT INTO dbo.Beacons (beacon_code, artifact_id, zone_name, uuid, major, minor, trigger_distance, battery_level, map_x, map_y, status)
VALUES
('BC-001', 1, N'Tầng 1 - Phòng Đông Sơn', 'FDA50693-A4E2-4FB1-AFCF-C6EB07647825', 1, 101, 2.0, 95, 340, 215, 'ACTIVE'),
('BC-002', 2, N'Tầng 1 - Phòng Gốm Cổ', 'FDA50693-A4E2-4FB1-AFCF-C6EB07647825', 1, 102, 2.5, 88, 520, 180, 'ACTIVE'),
('BC-003', 3, N'Tầng 1 - Khu Chăm Pa', 'FDA50693-A4E2-4FB1-AFCF-C6EB07647825', 1, 103, 1.5, 18, 280, 420, 'LOW_BATTERY'),
('BC-004', 4, N'Tầng 1 - Phòng Đông Sơn', 'FDA50693-A4E2-4FB1-AFCF-C6EB07647825', 1, 104, 2.0, 0, 410, 310, 'LOST_SIGNAL'),
('BC-005', 5, N'Tầng 1 - Phòng Triều Nguyễn', 'FDA50693-A4E2-4FB1-AFCF-C6EB07647825', 1, 105, 2.0, 92, 610, 450, 'ACTIVE');

INSERT INTO dbo.VisitLogs (user_id, device_id, artifact_id, beacon_id, lang_used, duration_seconds, is_favorite)
VALUES
(4, 'android-user-04', 1, 1, 'vi', 145, 1),
(4, 'android-user-04', 2, 2, 'vi', 95, 1),
(5, 'android-user-05', 1, 1, 'en', 180, 1),
(5, 'android-user-05', 3, 3, 'en', 110, 0);

INSERT INTO dbo.Feedbacks (user_id, visitor_name, artifact_id, rating, comment, is_visible)
VALUES
(4, N'Nguyễn Minh', 1, 5, N'Thuyết minh tự động rất nhạy, đi tới gần là máy tự hiện thông tin Trống đồng Ngọc Lũ rất tiện lợi.', 1),
(5, N'John Davis', 2, 5, N'Beautiful Chu Dau ceramic vase! The English audio guide made the history come alive.', 1),
(NULL, N'Trần Bảo Ngọc', 3, 4, N'Không gian trưng bày đẹp, âm thanh thuyết minh rõ ràng dễ nghe.', 1);

IF @@ERROR <> 0
    ROLLBACK TRAN;
ELSE
    COMMIT TRAN;
GO