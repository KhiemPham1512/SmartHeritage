# Kế hoạch hoàn thiện Smart Heritage (lập 2026-11-22)

Người đọc: cả nhóm. Nguồn chuẩn: `docs/nghiepvu.md` (nghiệp vụ) và Figma (giao diện). Đầu ra cuối: TV1 (Khiêm) tích hợp và sửa mọi phần còn lỗi; file của ai bị sửa sẽ ghi trong mô tả PR.

## 1. Hiện trạng đã kiểm (ngày 22/11)

| Vùng | Kết quả kiểm | Mức |
| --- | --- | --- |
| Git | PR #13 (mobile M6) merge vào `main`, `develop` chưa có → hai nhánh lệch nhau | Cao |
| Admin web | `npm run build` **lỗi 7 chỗ**: W3/W4 import `../types`, `mediaUrl`, `uploadMedia` không tồn tại; `App.tsx` truyền props sai cho W3/W4. `npm run lint` 5 lỗi. `lucide-react` chưa được commit vào package.json | Chặn |
| Backend — bảo mật | Mọi API quản trị (CRUD hiện vật/beacon/user, upload, kiểm duyệt, thống kê) **không yêu cầu đăng nhập**; VISITOR đăng nhập được Web Admin; mật khẩu so sánh cả dạng thô, SHA-256 không salt | Cao |
| Backend — nghiệp vụ | Chưa có CRUD khu di tích (Sites); không có trường video; chưa có API ngôn ngữ (W8); `resolve` bỏ qua UUID; upload không giới hạn kích thước, tên file có thể trùng | Trung bình |
| W2 Dashboard | Biểu đồ Top 10, lượt theo ngày, ngôn ngữ, "+12,5%" là **số cứng của Figma**; số thật chỉ hiện khi bấm ô ngày; bộ lọc ngày chưa chạy; "Xuất PDF" = in trang | Cao |
| W1 Login | "Ghi nhớ đăng nhập", "Quên mật khẩu?" chưa có tác dụng; không chặn vai trò VISITOR | Trung bình |
| Mobile | Đăng nhập/đăng ký **giả** (không gọi `/api/auth/*`) → lịch sử không theo tài khoản; địa chỉ server cố định `127.0.0.1` (cần `adb reverse`) | Cao |
| Beacon giả lập | Đang là màn hình trong app khách, danh sách 5 minor viết cứng; cần 2 điện thoại Android | Trung bình |
| Icon Figma | `docs/local/logos/` có icon thẻ thống kê W3, thiết bị W5, điểm iBeacon W6, đánh giá W7 nhưng **chưa màn nào dùng** (W3–W8 dùng lucide) | Thấp |
| Kiểm thử | Không có test tự động nào | Trung bình |
| Figma | Đã đối chiếu bằng ảnh export (`docs/local/W1..W8 *.png`, `docs/local/SmartHeritage/1..10 *.png`) — chi tiết mục 1b | — |

## 1b. Đối chiếu Figma (ảnh export 22/11)

| Màn | Khớp | Lệch / thiếu so với Figma |
| --- | --- | --- |
| Khung chung (sidebar, header) | Bố cục, màu, menu, khối "Bảo tàng Di sản Việt" | Header thiếu chuông thông báo + mũi tên menu người dùng; avatar luôn "NA"; vai trò luôn "Quản trị viên" |
| W1 | Bố cục 2 cột, logo, form, nút | Code điền sẵn email/mật khẩu (Figma để trống); có dòng "Hệ thống quản lý di sản thông minh" Figma không có; nền phải Figma xám rất nhạt |
| W2 | 4 KPI, Top 10, đường theo ngày, donut | Thiếu icon KPI (người, bảo tàng, sóng, sao), icon nút Xuất Excel/PDF, mũi tên ô ngày; màu cột 4–10 Figma nhạt hơn; **số liệu cứng** |
| W3 | (cần dựng lại sau khi sửa build) | Figma có: 4 KPI có icon (bộ `the_thong_ke`), tab Tất cả/Đang hiển thị/Bản nháp, cột ảnh + ngôn ngữ đã dịch, phân trang |
| W4 | — | Figma có: bộ ảnh/**video** (MP4 ≤ 100 MB), ảnh ≤ 10 MB, MP3 ≤ 20 MB, tab theo **từng ngôn ngữ** + "Thêm ngôn ngữ" (mỗi ngôn ngữ có audio riêng), trình soạn văn bản, Lưu bản nháp |
| W5 | Bảng + form sửa | Figma có icon KPI (bộ `thiet_bi`), cột "Tín hiệu gần nhất", phân trang. Figma dùng UUID/minor minh họa khác DB — giữ theo DB |
| W6 | Sơ đồ + panel điểm chọn | Figma có: chọn **khu di tích + tầng**, tải ảnh sơ đồ, vòng bán kính kích hoạt, kéo thả, zoom, chú thích màu (icon bộ `diem_ibeacon_tren_ban_do`, `vi_tri_loi_vao`) |
| W7 | Bảng kiểm duyệt | Figma có: lọc theo hiện vật, sắp xếp Mới nhất, **lý do ẩn** (DB chưa có cột), nút "Quy tắc kiểm duyệt", icon KPI (bộ `danh_gia`) |
| W8 | Bảng tài khoản | Figma có: **bảng ngôn ngữ bật/tắt** (VI/EN/KO/FR/JA, số hiện vật đã dịch), lọc vai trò, trạng thái Tạm khóa, khối "Phạm vi phân quyền" |
| Mobile 1–10 | Figma ở mức wireframe; cấu trúc 4 tab (Trang chủ/Bản đồ/Đã lưu/Cá nhân) khớp code TV2 | Kiểm trên APK thật ở GĐ4 |

## 2. Các giai đoạn

Mỗi giai đoạn một nhánh `feature/*` từ `develop`, một PR, merge khi TV1 duyệt.

### GĐ0 — Dọn nền (làm ngay, ~0,5 ngày)
- [x] Đồng bộ M6: nhánh `feature/gd0-nen-tang` tạo từ `develop` + `main` (PR vào develop sẽ mang theo M6).
- [x] Sửa build web: bổ sung `src/types.ts`, `mediaUrl`, `uploadMedia` trong `api.ts`, nối props W3/W4 trong `App.tsx` (lấy lại từ nhánh `feature/web-w3-w4`/`minhtam` nếu còn); sửa 5 lỗi lint; commit `lucide-react`.
- [x] Khung test `backend/tests/` (pytest + TestClient).
- Nghiệm thu: `npm run build` và `npm run lint` sạch; `pytest` chạy được.

### GĐ1 — Backend: bảo mật + nghiệp vụ còn thiếu (TV1, ~1,5 ngày)
- [x] `require_staff`/`require_admin` cho mọi API quản trị; `/api/users*` chỉ ADMIN; API khách giữ mở cho Guest.
- [x] Đăng nhập Web chỉ cho ADMIN/CONTENT_MANAGER; mật khẩu PBKDF2 có salt; dữ liệu mẫu đã băm; script `backend/scripts/rehash_plaintext_passwords.py` cho DB cũ.
- [x] Upload: giới hạn kích thước (ảnh 10 MB, MP3 20 MB, MP4 100 MB), tên file uuid. Khách chỉ thấy hiện vật PUBLISHED. `GET /api/beacons/public` cho beacon-sim.
- [x] `resolve` kiểm cả UUID (tùy chọn, mobile cũ vẫn chạy). [x] Cập nhật tọa độ W6 gọi `sp_AssignBeaconSafe_UpdLock`.
- [x] `GET /api/analytics/dashboard?from=&to=` có lọc ngày, KPI thời lượng xem trung bình, % so với kỳ trước, pin yếu/mất tín hiệu; export CSV theo cùng khoảng ngày.
- [ ] Theo Figma: gallery ảnh/video (dùng cột `gallery_json` sẵn có), lý do ẩn bình luận, khóa tài khoản, lọc theo site/tầng cho bản đồ.
- [x] Bảng `Languages` + `ArtifactTranslations` + hàm `fn_LocalizedArtifacts` (chưa dịch → lùi về tiếng Anh); API `/api/languages`, `/api/artifacts/{id}/translations/{lang}`; CRUD `/api/sites` (ẩn bằng `is_active`, không xóa).
- [x] Migration cho DB cũ `database/migrations/` + `backend/scripts/apply_migrations.py`. **Phát hiện: DB máy TV1 thiếu toàn bộ 7 SP, 2 View, 3 Index** (tạo từ script cũ) → đã bổ sung; mọi thành viên cần chạy `apply_migrations.py`.
- Nghiệm thu: test pytest cho 401/403 theo vai trò, hash mật khẩu, resolve, dashboard có lọc ngày.

### GĐ2 — W1, W2 theo Figma + dữ liệu thật (TV1, ~1 ngày)
- [x] W1: ghi nhớ đăng nhập (localStorage khi tích, sessionStorage khi không), báo lỗi khi tài khoản không có quyền quản trị, "Quên mật khẩu?" theo Figma; 401 → tự đăng xuất.
- [x] W2: mọi biểu đồ lấy từ API, chọn khoảng ngày, trạng thái rỗng/lỗi; Xuất Excel kèm token; Xuất PDF bằng trang in có CSS riêng.
- [x] So từng chi tiết với ảnh Figma W1/W2 (chụp màn hình thật, header có chuông/avatar/vai trò). Dữ liệu demo: `backend/scripts/seed_demo_visits.py` (`--clear` để xóa).

### GĐ3 — W3–W8 theo Figma (gốc: TV3/TV4/TV5; thực hiện: TV1 + Claude, ~1,5 ngày)
- [ ] Thay icon bằng bộ icon trong `docs/local/logos/` (W3 thẻ thống kê, W5 thiết bị, W6 điểm iBeacon + lối vào, W7 đánh giá).
- [ ] So từng màn với ảnh Figma, sửa lệch.
- [ ] W4: tải ảnh/MP3 chạy thật; W6: kéo thả tọa độ lưu qua API; W7: ẩn/hiện/xóa; W8: tài khoản + ngôn ngữ.
- [ ] Mọi màn xử lý 401/403 sau khi bật RBAC.

### GĐ4 — Mobile tích hợp thật (gốc: TV2; thực hiện: TV1 + Claude, ~1,5 ngày)
- [ ] Đăng nhập/đăng ký/hồ sơ gọi API thật, lưu token; Guest dùng `device_id` riêng cho từng máy.
- [ ] Màn cấu hình địa chỉ server (IP LAN) thay cho `127.0.0.1` cố định.
- [ ] Ngưỡng kích hoạt lấy `trigger_distance` của từng beacon từ API (thay hằng số 2.0 m); gửi thời lượng xem thật.
- [ ] Đối chiếu APK `docs/app-debug.apk` với mã nguồn trên `main`.
- [ ] Bỏ màn giả lập khỏi app khách sau khi GĐ5 chạy ổn.

### GĐ5 — App giả lập trạm phát `beacon-sim/` (TV1 + Claude, ~1 ngày)
- [x] Đã dựng app + unit test; hướng dẫn demo 2 điện thoại: `beacon-sim/README.md`. [ ] Kiểm tay trên 2 điện thoại thật (TV1).
- App Android riêng (Kotlin + Compose + AltBeacon `BeaconTransmitter`), cài trên điện thoại thứ 2.
- Nhập địa chỉ server → `GET /api/beacons` → danh sách BC-xxx (UUID/Major/Minor/TxPower, hiện vật, trạng thái). Mất mạng thì dùng 5 beacon mẫu.
- Chạm để phát / dừng; chọn mức công suất phát (thấp/trung bình/cao) để mô phỏng đứng gần/xa; báo lỗi khi máy không hỗ trợ BLE advertising.
- Nghiệm thu (kiểm tay): máy A chạy app khách, máy B phát BC-001 → máy A bật popup Trống đồng Ngọc Lũ trong < 2 giây; phát lại BC-001 không bật lặp; chuyển BC-002 → bật hiện vật mới.

### GĐ6 — Kiểm thử tích hợp, tài liệu, nộp (cả nhóm, ~1 ngày)
- [ ] Kịch bản demo đầu-cuối: Admin thêm hiện vật + MP3 (W4) → gán beacon + tọa độ (W5/W6) → khách đến gần (beacon-sim) → nghe thuyết minh → đánh giá → Admin kiểm duyệt (W7) → số liệu lên W2 → xuất báo cáo.
- [ ] Cập nhật README (API, phân quyền, cách chạy 2 điện thoại), Test Report, User Manual.
- [ ] PR `develop → main` cho bản nộp.

## 3. Thứ tự và phụ thuộc
GĐ0 → GĐ1 → (GĐ2, GĐ3 song song) ; GĐ1 → GĐ4 ; GĐ5 độc lập (chỉ cần `GET /api/beacons`) → GĐ6.

## 4. Cần nhóm/TV1 quyết định
1. **Đa ngôn ngữ** (nghiệp vụ + Figma W4/W8 đều yêu cầu quản lý ngôn ngữ và bản dịch): (a) thêm bảng `Languages` + `ArtifactTranslations`, giữ cột `_vi/_en` cũ để mobile không vỡ — đúng Figma, thêm ~1 ngày; hoặc (b) chỉ VI/EN, W8 hiển thị ngôn ngữ dạng bật/tắt nhưng chỉ VI/EN có nội dung. Đề xuất: (a).
2. Quản lý khu di tích (Sites): Figma W6 có ô chọn khu di tích + tầng → thêm API CRUD Sites, giao diện tạo site đặt trong W6?
3. Docker/triển khai (Task Package 5): có cần cho buổi bảo vệ không?
