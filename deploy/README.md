# Triển khai Smart Heritage

Hệ thống gồm **SQL Server** + **backend FastAPI** (đồng thời phục vụ **web admin** đã build) + **app Android**. Ở chế độ production chỉ cần chạy **một tiến trình** (backend) và một CSDL; web admin mở ở cùng địa chỉ, app khách trỏ vào địa chỉ đó.

| Cách | Hợp với | Chi phí | Trạng thái |
| --- | --- | --- | --- |
| **A. Laptop + Wi-Fi** (`deploy/windows`) | Buổi bảo vệ/demo tại chỗ | 0 đồng | Đã chạy thử trên máy dev |
| **B. Laptop + Cloudflare Tunnel** | Cho người ở xa xem thử qua HTTPS | 0 đồng | Dùng lại A; chưa chạy thử tunnel |
| **C. Docker Compose + Caddy trên VPS** (`deploy/docker`) | Chạy thường trực có tên miền + HTTPS | Thuê VPS (RAM ≥ 4 GB) + tên miền | Đã viết, **chưa chạy thử** (máy dev chưa cài Docker); logic khởi tạo CSDL đã kiểm trên SQL Server thật |

## A. Chạy trên laptop (mạng Wi-Fi nội bộ)

Điều kiện: đã cài SQL Server, đã chạy `database/init_schema.sql`, đã có `backend\.venv` và `backend\.env` (README mục 9).

```powershell
powershell -ExecutionPolicy Bypass -File deploy\windows\run-production.ps1 -OpenFirewall   # -OpenFirewall cần quyền Administrator
```

Script tự: build web → tạo `JWT_SECRET` riêng trong `backend\.env` (lần đầu) → chạy `uvicorn` ở `0.0.0.0:8000` với `APP_ENV=production`. Sau đó:

- Web admin: `http://<IP máy>:8000/` · Swagger: `/docs`
- Điện thoại cùng Wi-Fi: app khách → **Máy chủ** → nhập `<IP máy>:8000` → Kiểm tra kết nối → Lưu.
- Beacon Sim cũng nhập đúng địa chỉ đó.

Lỗi hay gặp: điện thoại không vào được → thường là Windows Firewall chặn cổng 8000 (dùng `-OpenFirewall`) hoặc mạng Wi-Fi đặt ở chế độ *Public* thay vì *Private*.

## B. Mở cho người ở xa bằng Cloudflare Tunnel (tạm thời)

Khi A đang chạy, cài `cloudflared` rồi:

```powershell
cloudflared tunnel --url http://localhost:8000
```

Lệnh in một địa chỉ dạng `https://xxxx.trycloudflare.com` (HTTPS, không cần mở cổng router). Địa chỉ **đổi mỗi lần chạy** và máy phải đang bật. Trong app khách nhập đúng `https://xxxx.trycloudflare.com` (không thêm cổng).

## C. Docker Compose trên VPS

```bash
cd deploy/docker
cp .env.example .env        # điền DB_PASSWORD, JWT_SECRET (>= 32 ký tự: openssl rand -hex 32)
docker compose up -d --build
```

Gồm: `db` (SQL Server 2022 Express, dữ liệu ở volume `dbdata`) → `db-init` (tạo CSDL nếu chưa có, ngược lại chỉ áp migration; **không xóa dữ liệu**) → `api` (backend + web). Truy cập `http://<IP VPS>:8000/`.

**HTTPS có tên miền:** trỏ DNS `A` của tên miền về IP VPS, mở cổng 80/443, đặt `DOMAIN=...` và `API_BIND=127.0.0.1` trong `.env`, rồi `docker compose --profile https up -d --build`. Caddy tự xin và gia hạn chứng chỉ Let's Encrypt. App khách nhập `https://<tên miền>`.

Lần chạy đầu cần kiểm: `docker compose ps` (db healthy, db-init exited 0, api healthy) và `curl http://localhost:8000/healthz`. Nếu `db-init` lỗi, xem `docker compose logs db-init`.

## Việc bắt buộc trước khi cho người khác dùng

1. **Đổi mật khẩu mọi tài khoản mẫu** (`123456`): đăng nhập Admin → *Ngôn ngữ & tài khoản* → Sửa → Đặt lại mật khẩu (hoặc khóa các tài khoản mẫu không dùng).
2. **Dữ liệu mẫu:** xóa lượt tham quan demo bằng `python scripts\seed_demo_visits.py --clear` (trong `backend`) nếu đã từng sinh.
3. **`JWT_SECRET` riêng**: production từ chối khởi động nếu thiếu hoặc dùng khóa mặc định (kiểm bằng `/healthz` sau khi chạy).
4. **HTTP thường chỉ dùng trong mạng nội bộ.** Ra Internet thì dùng B hoặc C (HTTPS) — mật khẩu và token đi qua mạng.
5. **Sao lưu:** CSDL `BACKUP DATABASE SmartHeritageDB TO DISK = N'...\SmartHeritageDB.bak'` (Docker: sao lưu volume `dbdata`); ảnh/MP3 Admin tải lên nằm ở `backend/static/` (Docker: volume `static`).

## Biến môi trường (backend)

| Biến | Ý nghĩa |
| --- | --- |
| `APP_ENV=production` | Bắt buộc `JWT_SECRET` mạnh; không mở CORS; tự phục vụ `admin-web/dist` ở `/` |
| `JWT_SECRET` | Khóa ký đăng nhập (≥ 32 ký tự) |
| `CORS_ORIGINS` | Danh sách origin cho phép, phân cách bằng dấu phẩy (chỉ cần khi web ở tên miền khác API) |
| `WEB_DIST` | Thư mục web đã build (mặc định `admin-web/dist` khi production) |
| `DB_SERVER`, `DB_NAME`, `USE_WINDOWS_AUTH`, `DB_USER`, `DB_PASSWORD` | Kết nối SQL Server (như `backend/.env.example`) |

Không có `APP_ENV=production` (khi phát triển) thì mọi thứ chạy như trước: web `npm run dev` ở cổng 5173 gọi API cổng 8000.
