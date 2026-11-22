# Việc cần TV1 tự kiểm / tự làm (Claude không làm được hoặc chưa kiểm được)

Cập nhật lần cuối: 2026-11-22. Làm tuần tự từ trên xuống; xong mục nào thì đánh `[x]`. Gặp lỗi: ghi lại bước, màn hình và thông báo lỗi rồi gửi Claude.

Chuẩn bị chung: backend chạy `uvicorn app.main:app --host 0.0.0.0 --port 8000` (cổng 8000), web `npm run dev` (cổng 5173), đăng nhập `ngocanh@smartheritage.vn` / `123456`. Hai điện thoại và máy tính cùng một Wi-Fi.

## A. Bắt buộc trước khi nộp (luồng cốt lõi của đồ án)

### A1. Cài và chạy app beacon-sim trên điện thoại thứ 2
- [ ] Điện thoại Xiaomi (Android 15) chặn `adb install`: bật *Tùy chọn nhà phát triển → Cài đặt qua USB* (và *Gỡ lỗi USB (Cài đặt bảo mật)* nếu có), rồi chạy `D:\Android\SDK\platform-tools\adb.exe install -r beacon-sim\app\build\outputs\apk\debug\app-debug.apk`. Hoặc mở thư mục `beacon-sim/` bằng Android Studio → Run.
  - Nếu chưa có APK: `$env:JAVA_HOME="D:\Android\Android Studio\jbr"; cd beacon-sim; .\gradlew.bat assembleDebug`.
- [ ] Mở app → cấp quyền Bluetooth → nhập `<IP máy tính>:8000` (`ipconfig`) → **Tải**. Kỳ vọng: dòng "Từ server: N thiết bị". Lỗi → ghi lại thông báo hiện dưới ô nhập.
- [ ] Bấm **Phát** ở BC-002 với công suất **Cao**. Kỳ vọng: thẻ trạng thái chuyển cam "Đang phát BC-002 · Major 1 · Minor 102".
- Máy nào báo "không hỗ trợ phát BLE" thì dùng máy khác (cần Android 8+ có BLE advertising).

### A2. Thử luồng iBeacon đầu-cuối với app khách (APK TV2: `docs/app-debug.apk`)
- [ ] Máy A cài app khách, mở Trang chủ (radar). Máy B phát BC-002 → Máy A bật popup "Bình gốm Chu Đậu" trong **dưới 2 giây** khi đặt gần nhau.
- [ ] Để nguyên máy B: popup **không** bật lại trong 5 phút (chống lặp).
- [ ] Máy B đổi sang BC-003 → máy A bật popup hiện vật mới ngay.
- [ ] Máy B công suất **Thấp** và đặt xa 3–5 m: máy A không bật popup (vượt ngưỡng 2,0 m).
- [ ] Bấm "Xem chi tiết" → nghe thuyết minh MP3; tắt máy B giữa chừng → vẫn nghe tiếp (cache).
- [ ] Máy B phát BC-004 (mất tín hiệu): máy A **không** bật popup.
- Lưu ý: app khách của TV2 đang gọi `127.0.0.1:8000`. Cắm USB rồi `adb reverse tcp:8000 tcp:8000`, hoặc chờ GĐ4 (cấu hình IP trong app). Ghi lại máy nào chạy được.

## B. Chức năng web chưa được bấm thử trong trình duyệt

### B1. W4 — Thêm / sửa hiện vật
- [ ] Sửa HV-002: đổi tên tiếng Việt → **Lưu thay đổi** → W3 hiện tên mới.
- [ ] Mở HV-001 → **+ Thêm ngôn ngữ → 한국어** → điền 3 ô → Lưu → W3 hiện nhãn `KO`; mở lại: tab 한국어 còn nội dung. Xóa tab bằng "Xóa bản dịch này" → Lưu → nhãn KO biến mất.
- [ ] Tạo hiện vật mới: ô Mã đã điền sẵn (HV-006 hoặc mã còn trống); upload ảnh JPG, MP3 tiếng Việt, MP3 tiếng Anh, 1 video MP4 → **Tạo hiện vật** → xuất hiện ở W3.
- [ ] Thử file quá lớn (ảnh > 10 MB) → hiện thông báo vượt dung lượng, không treo.
- [ ] **Lưu bản nháp** → hiện vật có trạng thái Bản nháp và app khách **không** thấy nó.
- [ ] Bấm **Hủy** khi đã sửa → hỏi "Bỏ các thay đổi chưa lưu?".
- [ ] Gán beacon trống cho hiện vật mới (ô "iBeacon đã gắn") → W5 hiện beacon đó gắn với hiện vật.

### B2. W1, W2, W3
- [ ] W1: tích "Ghi nhớ đăng nhập" → đóng hẳn trình duyệt, mở lại → vẫn đăng nhập; không tích → mở lại phải đăng nhập lại.
- [ ] W1: đăng nhập `nguyenvana@email.com` / `123456` (khách) → báo "không có quyền truy cập trang quản trị".
- [ ] W1: "Quên mật khẩu?" hiện thông báo liên hệ quản trị viên (nội dung do Claude tự viết; sửa nếu nhóm muốn khác).
- [ ] W2: chọn "Tháng này", "7 ngày", khoảng tùy chọn → số liệu, biểu đồ đổi theo; chọn khoảng chưa có dữ liệu → hiện "Chưa có …".
- [ ] W2: **Xuất Excel** → mở file `.csv` bằng Excel, tiếng Việt không lỗi font; tên file có khoảng ngày.
- [ ] W2: **Xuất PDF** → hộp thoại in: không có sidebar/header, biểu đồ không bị cắt (lưu thành PDF thử).
- [ ] W3: tìm kiếm, lọc khu vực/trạng thái, tab, nút Xóa (có hộp xác nhận); thêm nhiều hơn 8 hiện vật để thử phân trang.

### B3. Phân quyền
- [ ] Đăng nhập `minhduc@smartheritage.vn` / `123456` (Quản lý nội dung): vào được W2–W7; mở W8 → thấy bảng ngôn ngữ (công tắc bị khóa) và dòng "Chỉ Quản trị viên mới xem được tài khoản", không có nút "+ Thêm tài khoản".
- [ ] Swagger `http://localhost:8000/docs` → nút **Authorize** → dán token (lấy từ `POST /api/auth/login`) mới gọi được API quản trị.

## C. Máy của từng thành viên (gửi cho nhóm)

- [ ] Mỗi người sau khi `git pull`: trong thư mục `backend` chạy
  - `.\.venv\Scripts\python scripts\apply_migrations.py`
  - `.\.venv\Scripts\python scripts\rehash_plaintext_passwords.py`

  Không chạy → API báo `Invalid object name 'dbo.fn_LocalizedArtifacts'` và không đăng nhập được. Chạy `.\.venv\Scripts\python -m pytest tests -q` để kiểm: phải xanh, nếu `test_db_objects` đỏ là DB còn thiếu đối tượng.
- [ ] `cd admin-web; npm install` (đã thêm `lucide-react`).
- [ ] Máy cần dữ liệu demo cho W2: `python scripts\seed_demo_visits.py` (xóa lại bằng `--clear`). **Trước ngày nộp/quay demo phải quyết định** giữ hay xóa dữ liệu demo.

## D. Việc cần bạn quyết định / làm tay

- [ ] **DB máy bạn thiếu BC-001 và HV-001** (bị xóa khi thử trước đó). Muốn khôi phục đủ 5 beacon mẫu: nhờ Claude, hoặc chạy lại `database/init_schema.sql` (⚠ xóa toàn bộ dữ liệu, mất cả dữ liệu demo).
- [ ] `docs/nghiepvu.md` và `docs/local/` (logo, ảnh Figma) đang **chưa commit**: có đưa lên repo cho cả nhóm không?
- [ ] File ảnh rác `backend/static/images/media_1795337919.jpg` (do bạn thử upload) — xóa tay nếu không cần.
- [ ] Merge `develop → main` khi đã xong mục A và B (`main` chỉ cho bản kiểm chứng để nộp). Hiện `main` vẫn là bản cũ trước GĐ0.
- [ ] Docker (Task Package 5): có cần cho buổi bảo vệ không? Nếu có, báo Claude để thêm.
- [ ] Nội dung tài liệu nộp (URS, SRS, UML, ERD, Test Report, User Manual): Claude có thể viết nháp từ code và `docs/nghiepvu.md`, cần bạn chốt danh sách.

## E. Sẽ bổ sung khi Claude làm xong

### E1. W5 — Thiết bị iBeacon
- [ ] Bấm một dòng → form bên dưới đổi theo; sửa "Bán kính phát sóng" từ 2 → 3 mét → **Lưu thiết bị** → bảng hiện 3 m.
- [ ] Đổi "Gán vào hiện vật" sang hiện vật đang có beacon khác → lưu → beacon cũ trở thành "Chưa gán" (mỗi hiện vật một beacon). Chọn "— Chưa gán —" → lưu → hiện vật mất beacon.
- [ ] **+ Thêm iBeacon**: mã BC-xxx và Minor đã điền sẵn → lưu; thử Minor `70000` hoặc UUID sai → báo lỗi, không lưu. Thử trùng Minor với beacon khác → báo "đã tồn tại".
- [ ] Mở "Thông số nâng cao": đổi Pin về 15% và Trạng thái = Pin yếu → thẻ KPI "Pin yếu" tăng.
- [ ] **Xóa thiết bị** (có hộp xác nhận).

### E2. W6 — Bản đồ khu di tích (phần kéo thả Claude chưa thao tác được)
- [ ] **Kéo** một pin sang phòng khác → số X/Y bên phải đổi theo; dòng "(chưa lưu)" hiện; bấm **Cập nhật tọa độ** → tải lại trang vẫn giữ vị trí mới.
- [ ] **Click** lên sơ đồ khi đã chọn một beacon → pin nhảy tới điểm click.
- [ ] Kéo 2–3 pin rồi bấm **Lưu vị trí (n)** ở thanh trên → cả n pin được lưu.
- [ ] Phím mũi tên khi pin đang focus (Tab tới pin) → nhích 5 px; Shift+mũi tên → 20 px.
- [ ] Zoom 50% → 200%: sơ đồ cuộn được, pin vẫn đúng chỗ.
- [ ] **Tải ảnh sơ đồ mặt bằng** (PNG bất kỳ ≤ 10 MB) → ảnh đổi. Muốn quay lại ảnh cũ: tải lại `backend/static/images/floorplan_tang1.png`.
- [ ] Mở app khách → Bản đồ (Màn 7): vị trí hiện vật khớp tọa độ vừa lưu (app của TV2 có thể dùng vị trí vẽ sẵn riêng; ghi lại nếu lệch).

### E3. W7 — Đánh giá & phản hồi (DB hiện gần như trống bình luận)
- [ ] Tạo vài bình luận: trên app khách hoặc Swagger `POST /api/feedbacks` (artifact_id, rating, visitor_name, comment).
- [ ] **Ẩn** một bình luận → hộp chọn lý do → "Quảng cáo / Spam" → dòng đỏ "Lý do ẩn: …" hiện; thẻ "Đã ẩn" +1; điểm trung bình không tính bình luận đó. Chọn "Khác" phải nhập lý do.
- [ ] **Duyệt** lại → lý do biến mất, hiển thị lại. Khách xem hiện vật trong app: bình luận ẩn không hiện.
- [ ] Lọc theo hiện vật / số sao / trạng thái; gõ tìm kiếm (chờ ~0,3 giây); đổi "Sắp xếp"; phân trang khi có > 6 bình luận.
- [ ] Nút "Quy tắc kiểm duyệt" mở hộp nội dung (nội dung các lý do do Claude tự soạn; sửa theo quy định của bảo tàng nếu cần).

### E4. W8 — Ngôn ngữ & tài khoản
- [ ] Bật công tắc **Français** → app khách (sau GĐ4) hiện thêm lựa chọn; tắt lại. Ngôn ngữ **Tiếng Việt** (mặc định) không tắt được.
- [ ] **+ Thêm tài khoản** (Quản lý nội dung) → đăng nhập thử bằng tài khoản mới ở W1.
- [ ] **Sửa** một tài khoản: đổi vai trò, nhập mật khẩu mới → đăng nhập bằng mật khẩu mới được, mật khẩu cũ bị từ chối.
- [ ] Menu "⋯" → **Khóa tài khoản** → tài khoản đó đăng nhập báo "tạm khóa"; **Mở khóa** lại.
- [ ] Dòng của chính bạn: không khóa/hạ quyền được (nút "⋯" bị tắt; hộp Sửa khóa ô Vai trò).
- [ ] Gõ ô tìm kiếm, lọc vai trò, phân trang (tạo > 6 tài khoản để thử).
