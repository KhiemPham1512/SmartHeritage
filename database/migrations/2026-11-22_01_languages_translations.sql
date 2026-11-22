-- =========================================================================
-- MIGRATION 2026-11-22: ĐA NGÔN NGỮ (Languages + ArtifactTranslations + fn_LocalizedArtifacts)
-- Dành cho DB đã tạo bằng init_schema.sql bản cũ. Chạy lại nhiều lần an toàn, KHÔNG xóa dữ liệu.
-- DB mới tạo bằng init_schema.sql hiện tại đã có sẵn các đối tượng này.
-- =========================================================================
USE SmartHeritageDB;
GO

-- 1. Danh mục ngôn ngữ hệ thống (W8)
IF OBJECT_ID('dbo.Languages', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Languages (
        lang_code VARCHAR(10) NOT NULL PRIMARY KEY,
        name_native NVARCHAR(50) NOT NULL,
        name_vi NVARCHAR(50) NOT NULL,
        is_enabled BIT NOT NULL DEFAULT 1,
        is_default BIT NOT NULL DEFAULT 0,
        sort_order INT NOT NULL DEFAULT 0
    );
END
GO

MERGE dbo.Languages AS t
USING (VALUES
    ('vi', N'Tiếng Việt', N'Tiếng Việt', 1, 1, 1),
    ('en', N'English', N'Tiếng Anh', 1, 0, 2),
    ('ko', N'한국어', N'Tiếng Hàn', 1, 0, 3),
    ('fr', N'Français', N'Tiếng Pháp', 0, 0, 4),
    ('ja', N'日本語', N'Tiếng Nhật', 0, 0, 5),
    ('zh', N'中文', N'Tiếng Trung', 0, 0, 6)
) AS s (lang_code, name_native, name_vi, is_enabled, is_default, sort_order)
ON t.lang_code = s.lang_code
WHEN NOT MATCHED THEN
    INSERT (lang_code, name_native, name_vi, is_enabled, is_default, sort_order)
    VALUES (s.lang_code, s.name_native, s.name_vi, s.is_enabled, s.is_default, s.sort_order);
GO

-- 2. Users.preferred_lang: thay CHECK cố định bằng khóa ngoại tới Languages
IF OBJECT_ID('dbo.CK_Users_Lang', 'C') IS NOT NULL
    ALTER TABLE dbo.Users DROP CONSTRAINT CK_Users_Lang;
GO
IF OBJECT_ID('dbo.FK_Users_Languages', 'F') IS NULL
    ALTER TABLE dbo.Users ADD CONSTRAINT FK_Users_Languages
        FOREIGN KEY (preferred_lang) REFERENCES dbo.Languages(lang_code);
GO

-- 3. Bản dịch hiện vật cho ngôn ngữ ngoài VI/EN (VI/EN vẫn nằm ở cột name_vi/name_en... của Artifacts)
IF OBJECT_ID('dbo.ArtifactTranslations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ArtifactTranslations (
        artifact_id INT NOT NULL,
        lang_code VARCHAR(10) NOT NULL,
        name NVARCHAR(255) NOT NULL,
        summary NVARCHAR(500) NOT NULL,
        description NVARCHAR(MAX) NOT NULL,
        audio_url VARCHAR(500) NULL,
        updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT PK_ArtifactTranslations PRIMARY KEY (artifact_id, lang_code),
        CONSTRAINT FK_ArtTrans_Artifacts FOREIGN KEY (artifact_id) REFERENCES dbo.Artifacts(artifact_id) ON DELETE CASCADE,
        CONSTRAINT FK_ArtTrans_Languages FOREIGN KEY (lang_code) REFERENCES dbo.Languages(lang_code),
        CONSTRAINT CK_ArtTrans_NotViEn CHECK (lang_code NOT IN ('vi', 'en'))
    );
END
GO

-- 4. Hàm bảng nội tuyến: nội dung hiện vật theo ngôn ngữ, chưa dịch thì lùi về tiếng Anh
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

-- 5. Đồng bộ cột translated_langs với bản dịch thật (dữ liệu mẫu cũ ghi KO/JA nhưng chưa có nội dung)
UPDATE a
SET translated_langs = 'VI,EN' + COALESCE((
    SELECT ',' + UPPER(t.lang_code)
    FROM dbo.ArtifactTranslations t
    WHERE t.artifact_id = a.artifact_id
    ORDER BY t.lang_code
    FOR XML PATH('')
), '')
FROM dbo.Artifacts a;
GO
