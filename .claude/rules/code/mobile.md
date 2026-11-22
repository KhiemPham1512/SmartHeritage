---
paths:
  - "mobile/**"
  - "beacon-sim/**"
---
# Android app khách + beacon giả lập (chỉ nạp khi đụng vùng này)

- iBeacon layout `m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24`, UUID nhóm `FDA50693-A4E2-4FB1-AFCF-C6EB07647825`, Major 1, Minor 101..105 (khớp bảng `Beacons`). Đổi giá trị → sửa cả DB seed, `BeaconConfig.kt`, beacon-sim.
- Kích hoạt khi khoảng cách ≤ `trigger_distance` (mặc định 2.0 m), chống lặp bằng cooldown theo minor; vào beacon khác thì kích hoạt ngay.
- Địa chỉ backend phải cấu hình được (IP LAN của máy chạy FastAPI); `127.0.0.1` chỉ chạy khi có `adb reverse tcp:8000 tcp:8000`.
- `beacon-sim/` là app riêng (applicationId `com.smartheritage.beaconsim`), lấy danh sách từ `GET /api/beacons/public` (không cần token), phát bằng AltBeacon `BeaconTransmitter`. Không phụ thuộc code của `mobile/`. Hướng dẫn: `beacon-sim/README.md`.
- Build Android trên máy này: `$env:JAVA_HOME = "D:\Android\Android Studio\jbr"`, SDK `D:\Android\SDK`, `adb` ở `D:\Android\SDK\platform-tools`.
- Windows không phát iBeacon được (chặn company ID Apple 0x004C) → luôn cần một điện thoại Android thứ hai có BLE advertising.
- BLE/điện thoại thật Claude không chạy được: ghi bước kiểm tay cho người dùng, không tuyên bố đã kiểm.
