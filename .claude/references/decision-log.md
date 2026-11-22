# Nhật ký quyết định

| Ngày | Quyết định |
| --- | --- |
| 2026-11-22 | Git chế độ (A) GitHub nhóm: feature/* → PR vào `develop`; không tự merge/push. |
| 2026-11-22 | Claude được sửa mọi phần (backend, W1–W8, mobile) trên nhánh riêng; người dùng tự báo nhóm. |
| 2026-11-22 | Beacon giả lập làm thành app Android riêng `beacon-sim/`, đọc danh sách từ `GET /api/beacons`. |
| 2026-11-22 | Đối chiếu Figma bằng PNG người dùng export: `docs/local/W1..W8 *.png`, `docs/local/SmartHeritage/1..10 *.png` (Figma MCP gói Starter hết lượt). |
| 2026-11-22 | Git nguy hiểm chung đã do hook toàn máy `~/.claude/hooks/block-dangerous-git.sh` chặn (git-guardrails đã chạy trước đó ở cấp máy); hook dự án `guard-project.py` chỉ thêm: push thẳng `develop`, chạy `init_schema.sql`. Không chạy lại `git-guardrails-claude-code`. |
| 2026-11-22 | Chế độ nạp skill: theo nhu cầu. Chưa chạy skill một-lần nào. |
| 2026-11-22 | Người dùng giao 'làm theo ý Claude': chọn đa ngôn ngữ phương án (a) bảng Languages + ArtifactTranslations; CRUD Sites làm trong W6; Docker để sau (YAGNI). |
