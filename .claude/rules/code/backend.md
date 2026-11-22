---
paths:
  - "backend/**"
  - "database/**"
---
# Backend FastAPI + SQL Server (chỉ nạp khi đụng vùng này)

- SQL viết bằng `text()` với tham số `:ten`; **không** ghép chuỗi giá trị người dùng vào SQL. Lọc tùy chọn theo mẫu `WHERE 1=1` + nối điều kiện cố định như code hiện có.
- Mọi endpoint quản trị (tạo/sửa/xóa hiện vật, beacon, site, upload, users, kiểm duyệt, analytics, export) phải có phụ thuộc phân quyền `ADMIN`/`CONTENT_MANAGER`; `/api/users*` quản trị chỉ `ADMIN`. Endpoint khách (resolve, artifacts public, visit-logs, favorites, feedbacks POST, sites/map GET) mở cho guest.
- App khách chỉ thấy hiện vật `PUBLISHED`; beacon `LOST_SIGNAL` không resolve.
- Mật khẩu: không so sánh mật khẩu thô; hash có salt. Dữ liệu mẫu trong `init_schema.sql` phải lưu hash.
- Không đổi tên trường JSON đang được mobile dùng (`model/*.kt`) nếu chưa sửa mobile cùng PR.
- Thay đổi schema: sửa `database/init_schema.sql` VÀ thêm migration idempotent `database/migrations/<ngày>_<nn>_*.sql` cho DB đã có (không xóa dữ liệu). Sửa index/view/hàm/SP (PHẦN 3–5) → chạy `python database/migrations/build_objects_migration.py` để sinh lại migration 02. Nâng cấp DB: `backend/scripts/apply_migrations.py`. Thử script SQL trên DB tạm, không chạy `init_schema.sql` trên DB người dùng.
- Nội dung đa ngôn ngữ: truy vấn dùng `LEFT JOIN dbo.fn_LocalizedArtifacts(:lang) l ON l.artifact_id = a.artifact_id` rồi lấy `l.name/l.summary/l.description/l.audio_url`; không tự viết `CASE WHEN :lang`. VI/EN ở cột `_vi/_en`, ngôn ngữ khác ở `ArtifactTranslations`.
- Test: `backend/tests/` dùng pytest + `TestClient`; test chạm DB thật thì đánh dấu và bỏ qua khi không kết nối được.
- Upload: giới hạn đuôi + kích thước, tên file duy nhất (uuid), không dùng tên gốc làm đường dẫn.
