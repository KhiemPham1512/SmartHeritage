# Mục lục sổ tay `.claude/` (quy ước — file này không tự nạp)

| Đường dẫn | Chứa gì | Nạp khi nào |
| --- | --- | --- |
| `../CLAUDE.md` | Giới thiệu, lệnh, việc luôn làm | Mỗi phiên (cơ chế thật) |
| `rules/agent/workflow.md` | Vòng làm việc, báo cáo, dọn dẹp, khi nào hỏi | Mỗi phiên |
| `rules/agent/git.md` | Quy tắc GitHub nhóm | Mỗi phiên |
| `rules/agent/agy.md` | Khi nào giao agy, cách viết pipeline | Mỗi phiên |
| `rules/agent/skills.md` | Bảng điều hướng skill, cách tìm/nạp | Mỗi phiên |
| `rules/code/backend.md` | Quy ước FastAPI, RBAC, SQL | Khi đụng `backend/**`, `database/**` |
| `rules/code/web.md` | Quy ước admin-web, Figma, icon | Khi đụng `admin-web/**` |
| `rules/code/mobile.md` | Quy ước Android, iBeacon, beacon-sim | Khi đụng `mobile/**`, `beacon-sim/**` |
| `rules/docs/docs.md` | Tài liệu để đâu | Khi đụng `docs/**`, `README.md` |
| `skills/update-handoff/` | Cập nhật handoff 4 tầng | Khi người dùng gõ "update" hoặc cuối phiên |
| `hooks/guard-project.py` | Chặn push thẳng `develop` và chạy `init_schema.sql` (git nguy hiểm chung + push `main` đã có hook toàn máy `~/.claude/hooks/block-dangerous-git.sh`) | Tự chạy trước mỗi lệnh shell (cơ chế thật) |
| `references/decision-log.md` | Quyết định của người dùng, skill một-lần đã chạy | Khi cần tra (quy ước) |
| `local/handoff*.md` | Trạng thái phiên (không commit) | `handoff.md` đầu phiên; còn lại chỉ grep |
