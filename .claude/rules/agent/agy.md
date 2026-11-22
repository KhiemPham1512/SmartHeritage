# agy (Antigravity) — chỉ cho việc lớn

Trạng thái 2026-11-22: **chưa tìm thấy `connect.py` của bridge trên máy** → pipeline agy chưa dùng được; Claude tự làm mọi việc cho tới khi người dùng cài bridge và chạy `connect.py config --test-cmd "D:\HQTCSDL\SmartHeritage\backend\.venv\Scripts\python.exe -m pytest D:\HQTCSDL\SmartHeritage\backend\tests -q"`.

Khi bridge có sẵn:
- Giao pipeline khi MỘT điều đúng: đổi ≥4 file (tính cả test); refactor cơ học/sửa hàng loạt; test fail log dài hoặc chưa rõ nguyên nhân; tài liệu ~1.500 dòng trở lên; cần review độc lập diff rủi ro. Còn lại Claude tự làm, không nạp skill `agy-worker`.
- Claude lập kế hoạch một lần (đọc tối thiểu, chốt giao diện, ca biên, tiêu chí nghiệm thu) → một `pipeline` (`edit` có cả test + code, `test --diagnose`, `review`, `docs`, `ask`, `search`, `claude`) → `connect.py start --run pipeline --plan-file plan.json`, rồi `wait`.
- Claude kiểm: đọc báo cáo, tự chạy test một lần, đọc diff chỗ rủi ro; `stopped` thì gửi pipeline nhỏ hơn.
- Bước `kind: "claude"` (thêm `"blocking": true` nếu việc sau phụ thuộc): điện thoại thật/BLE, trình duyệt, SSMS, file `CLAUDE.md`/`.claude/**`, quyết định của người dùng.
- agy không sửa `CLAUDE.md`, `.claude/**`, `.env`, file ngoài repo. Không gửi cho agy: `backend/.env`, chuỗi kết nối DB, dữ liệu người dùng thật.
- Câu hỏi luôn đi sau `--query`; xem bảng lệnh trong SKILL.md của `agy-worker`, không đoán cú pháp.
