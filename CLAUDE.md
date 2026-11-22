# Smart Heritage — hướng dẫn cho Claude Code
Hệ thống du lịch thông minh iBeacon: app Android cho khách, Web Admin cho ban quản lý, FastAPI + SQL Server. Đồ án nhóm 5 người; người dùng là TV1 (Leader: DB, backend, W1/W2) và là đầu ra tích hợp cuối cùng. Trả lời bằng tiếng Việt, dễ hiểu. Windows 11, PowerShell 5.1.

## Lệnh (PowerShell, từ gốc repo)
- Backend: `cd backend; .\.venv\Scripts\python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload` (Swagger `/docs`, kiểm DB `/api/health-db`)
- Test backend: `backend\.venv\Scripts\python -m pytest backend\tests -q`
- Web: `cd admin-web; npm run dev` · kiểm: `npm run build; npm run lint`
- Mobile: `cd mobile; .\gradlew.bat assembleDebug` · Beacon giả lập: `cd beacon-sim; .\gradlew.bat assembleDebug`
- DB: chạy `database/init_schema.sql` trong SSMS (script xóa và tạo lại toàn bộ dữ liệu — chỉ chạy khi người dùng đồng ý).
- Python dùng `py -3.12` (máy có cả 3.14, không dùng cho venv).

## Cấu trúc mã
- `backend/app/` FastAPI: `routers/` (auth, artifact, beacon, analytics), `auth.py` JWT, `schemas.py`
- `database/init_schema.sql` 6 bảng, 3 index, 2 view, 7 SP (trọng tâm môn HQTCSDL)
- `admin-web/src/` React 19 + Vite: `App.tsx` khung, `api.ts`, `pages/W1..W8`
- `mobile/` Kotlin + Compose (TV2) · `beacon-sim/` app Android phát iBeacon giả lập
- `docs/nghiepvu.md` nghiệp vụ gốc · `docs/local/logos/` icon Figma · ảnh Figma: `docs/local/W1..W8 *.png` (web), `docs/local/SmartHeritage/1..10 *.png` (mobile, wireframe)

## Việc LUÔN làm (chi tiết ở .claude/rules/, tự nạp)
- Đầu phiên: chỉ đọc `.claude/local/handoff.md`; grep `handoff-tried.md` trước khi thử hướng mới.
- Nghiệp vụ chuẩn là `docs/nghiepvu.md`; giao diện chuẩn là Figma (ảnh export trong `docs/local/`). Lệch thì báo, không tự chế spec.
- Được sửa mọi phần (backend, W1–W8, mobile, beacon-sim) trên nhánh riêng; ghi file của thành viên nào bị sửa vào mô tả PR.
- agy chỉ cho việc lớn (đổi ≥4 file, refactor cơ học, test fail log dài, tài liệu dài, review rủi ro): kế hoạch một lần → `pipeline` → Claude kiểm tra (`rules/agent/agy.md`). Việc nhỏ tự làm; việc nhỏ/quen đề nghị Sonnet medium.
- Git: GitHub nhóm, feature/* → PR vào `develop`; không merge/push khi chưa hỏi (`rules/agent/git.md`).
- Không commit `.env`, `*.apk`, `.claude/local/`, `.claude/settings.local.json`.

## Mở khi cần (không tự nạp)
Mục lục: .claude/README.md · Kế hoạch: docs/ke-hoach-hoan-thien.md · Quyết định: .claude/references/decision-log.md
