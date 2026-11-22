---
paths:
  - "docs/**"
  - "README.md"
  - "admin-web/README.md"
---
# Tài liệu (chỉ nạp khi đụng vùng này)

- `docs/nghiepvu.md`: nghiệp vụ gốc, chỉ người dùng sửa.
- `docs/ke-hoach-hoan-thien.md`: kế hoạch + phân công hiện hành; cập nhật trạng thái khi xong việc.
- `docs/local/`: tài nguyên cục bộ (logo, ảnh Figma), không xóa. `docs/*.apk` không commit (đã ignore).
- `README.md`: giữ đúng với code (endpoint, lệnh, phân công). Thay đổi API → cập nhật bảng trong README cùng PR.
- Handoff trong `.claude/local/` (không commit), cập nhật bằng skill `update-handoff`.
