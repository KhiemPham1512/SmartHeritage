# Beacon Sim — trạm iBeacon giả lập

App Android riêng thay cho app "Beacon Simulator" trên Store (đã gỡ). Cài trên **điện thoại thứ 2**, đặt cạnh "hiện vật"; điện thoại khách chạy app Smart Heritage sẽ bắt sóng như với cục iBeacon thật.

> Máy tính Windows không phát iBeacon được (Windows chặn mã nhà sản xuất Apple 0x004C), nên bắt buộc dùng điện thoại Android có hỗ trợ BLE advertising (hầu hết máy từ Android 8 trở lên).

## Build & cài

```powershell
$env:JAVA_HOME = "D:\Android\Android Studio\jbr"   # JDK đi kèm Android Studio (đổi theo máy)
cd beacon-sim
# lần đầu: tạo local.properties với dòng  sdk.dir=D\:\\Android\\SDK  (đường dẫn Android SDK của máy)
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Hoặc mở thư mục `beacon-sim/` bằng Android Studio → Run.

## Dùng khi demo (2 điện thoại + 1 máy tính)

1. Máy tính chạy backend: `uvicorn app.main:app --host 0.0.0.0 --port 8000`. Xem IP LAN: `ipconfig` (vd. `192.168.1.5`). Hai điện thoại cùng Wi-Fi với máy tính.
2. **Máy B (trạm phát)** mở Beacon Sim → nhập `192.168.1.5:8000` → **Tải**: danh sách lấy từ `GET /api/beacons/public` (UUID/Major/Minor/TxPower đúng như Admin cấu hình ở W5). Không có mạng thì app dùng 5 beacon mẫu BC-001…BC-005.
3. Chọn công suất **Cao (gần)** → bấm **Phát** ở beacon cần thử (vd. BC-002).
4. **Máy A (khách)** mở Smart Heritage, đứng gần máy B → popup hiện vật bật lên.
5. Thử chống lặp: giữ nguyên → không bật lại trong thời gian cooldown. Chuyển máy B sang beacon khác → máy A bật hiện vật mới ngay.
6. Giả lập "khách đi xa": chọn công suất **Thấp (xa)** (dừng rồi phát lại) → khoảng cách ước lượng vượt ngưỡng `trigger_distance`, máy A không bật popup.

Beacon ghi "app khách sẽ bỏ qua" là beacon `LOST_SIGNAL` hoặc chưa gắn hiện vật — backend không resolve chúng (đúng nghiệp vụ).

## Mã nguồn

- `BeaconCatalog.kt` — đọc JSON API, danh sách mẫu, mức công suất (có unit test).
- `MainActivity.kt` — giao diện Compose + `BeaconTransmitter` (AltBeacon, cùng layout iBeacon với app khách).
