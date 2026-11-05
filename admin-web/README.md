# 🖥️ Smart Heritage — Tài liệu Kỹ thuật & Hướng dẫn Phát triển `admin-web` (W1–W8)

Tài liệu này nằm ngay trong thư mục `admin-web/` nhằm hướng dẫn chi tiết cho các thành viên **TV3 (Minh Tâm)**, **TV4** và **TV5** cách kéo mã nguồn về máy, quy tắc cô lập file chống xung đột Git và cú pháp gọi API Backend FastAPI (cổng `8000`) cho các màn hình **`W3` $\rightarrow$ `W8`**.

---

## 1. Quy tắc Cô lập Tệp tin (File Isolation Policy — Bắt buộc tuân thủ)

Khung điều hướng Sidebar, thanh Header trên cùng (`src/App.tsx`), hàm gọi API tự động đính kèm JWT Token (`src/api.ts`) cùng 2 màn hình **`W1_Login.tsx`** và **`W2_Dashboard.tsx`** đã được **Leader (Phạm Gia Khiêm - TV1)** thiết lập hoàn chỉnh.

Để **không bao giờ xảy ra xung đột Git (`Merge Conflict`)** khi gộp nhánh vào `develop`:

1. **TUYỆT ĐỐI KHÔNG SỬA** các file dùng chung: `src/App.tsx`, `src/api.ts`, `src/index.css`, `package.json`.
2. Mỗi thành viên **CHỈ MỞ VÀ CODE TRONG ĐÚNG 2 FILE** được giao bên trong thư mục `src/pages/`:

| Thành viên | Tên nhánh Git bắt buộc | 2 File duy nhất được phép chỉnh sửa |
| :--- | :--- | :--- |
| **TV3 (Minh Tâm)** | `feature/web-w3-w4` | `src/pages/W3_Artifacts.tsx`<br/>`src/pages/W4_ArtifactForm.tsx` |
| **TV4** | `feature/web-w5-w6` | `src/pages/W5_Beacons.tsx`<br/>`src/pages/W6_FloorMap.tsx` |
| **TV5** | `feature/web-w7-w8` | `src/pages/W7_Feedbacks.tsx`<br/>`src/pages/W8_Users.tsx` |

---

## 2. Quy trình Git & Khởi chạy cho TV3, TV4, TV5

### Bước 1: Kéo khung `admin-web` mới nhất từ nhánh `develop` và tạo nhánh riêng

Mở Terminal tại thư mục gốc `SmartHeritage` và chạy:

```powershell
git checkout develop
git pull origin develop

# Tạo nhánh riêng theo đúng phân công (Ví dụ TV3 gõ: git checkout -b feature/web-w3-w4)
git checkout -b <ten-nhanh-cua-ban>
```

### Bước 2: Chạy thử giao diện Web trên máy

Đảm bảo bạn đã bật Backend FastAPI ở Terminal 1 (`uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`), sau đó mở Terminal 2:

```powershell
cd admin-web
npm install
npm run dev
```

Mở trình duyệt vào `http://localhost:5173/`, đăng nhập bằng tài khoản `ngocanh@smartheritage.vn` / mật khẩu `123456`, sau đó bấm vào mục menu của bạn ở thanh bên trái để kiểm tra giao diện đang làm.

---

## 3. Bảng màu & Style chuẩn Figma để đồng bộ với W1 và W2

Khi viết `CSSProperties` (inline style) trong các file `W3` $\rightarrow$ `W8`, các bạn sử dụng thống nhất bảng màu sau để toàn bộ 8 màn hình đồng bộ 100%:

* **Màu nền trang (đã có sẵn ở khung ngoài):** `#f1ede4`
* **Thẻ khung trắng (Card / Table Container):** `backgroundColor: "#ffffff"`, `border: "1.5px solid #d4cdc3"`, `borderRadius: 11`, `padding: "16px 20px"`
* **Màu đỏ đô chủ đạo (Nút chính / Điểm nhấn):** `backgroundColor: "#80140e"`, chữ trắng `#ffffff`
* **Màu nền hồng nhạt (Badge trạng thái nổi bật):** `backgroundColor: "#fde8e6"`, `border: "1.5px solid #eca9a4"`, chữ `#80140e`
* **Màu chữ tiêu đề chính:** `#0f172a` (`fontWeight: 800`)
* **Màu chữ nội dung phụ / nhãn:** `#334155` (`fontWeight: 600`)

---

## 4. Đặc tả API chi tiết cho từng thành viên

Trong file `src/api.ts`, Leader đã viết sẵn hàm `apiFetch<T>(path, options)` và hằng số `API_BASE = "http://localhost:8000"`. Bạn chỉ cần `import { apiFetch, API_BASE } from "../api";` ở đầu file của mình để sử dụng.

---

### 4.1. Phần việc của TV3 (Minh Tâm): `W3_Artifacts.tsx` & `W4_ArtifactForm.tsx`

#### A. Màn hình `W3_Artifacts.tsx` (Quản lý danh sách hiện vật)
1. **Lấy số liệu 4 thẻ KPI đầu trang W3:**
   * **Endpoint:** `GET /api/artifacts/stats`
   * **Dữ liệu trả về:**
     ```json
     {
       "total": 5,
       "published": 5,
       "hidden_or_draft": 0,
       "with_beacon": 5
     }
     ```
2. **Lấy danh sách hiện vật (có hỗ trợ lọc):**
   * **Endpoint:** `GET /api/artifacts?lang=vi` *(Có thể nối thêm `&search=...&status=PUBLISHED`)*
   * **Dữ liệu trả về:** Mảng các đối tượng hiện vật (`artifact_id`, `artifact_code`, `zone_name`, `name_vi`, `name_en`, `summary_vi`, `image_url`, `audio_vi_url`, `beacon_code`, `translated_langs`, `status`).
3. **Xóa hiện vật:**
   * **Endpoint:** `DELETE /api/artifacts/{artifact_id}`

#### B. Màn hình `W4_ArtifactForm.tsx` (Thêm / Sửa hiện vật đa ngôn ngữ & Upload file)
1. **Tải file Ảnh (`.jpg`/`.png`) hoặc Audio thuyết minh (`.mp3`) lên Server:**
   * **Endpoint:** `POST /api/upload/media` (dùng `FormData`)
   * **Cách viết hàm upload trong `W4_ArtifactForm.tsx`:**
     ```tsx
     const handleUploadFile = async (file: File) => {
       const formData = new FormData();
       formData.append("file", file);
       const res = await fetch(API_BASE + "/api/upload/media", {
         method: "POST",
         body: formData,
       });
       const data = await res.json();
       return data.url as string; // Ví dụ: "/static/images/media_171000.jpg"
     };
     ```
2. **Tạo mới hiện vật (`POST /api/artifacts`) hoặc Cập nhật (`PUT /api/artifacts/{id}`):**
   * **Body JSON gửi lên:**
     ```json
     {
       "site_id": 1,
       "artifact_code": "HV-006",
       "zone_name": "Khu trưng bày Đông Sơn - Tầng 1",
       "name_vi": "Mũi giáo đồng Đông Sơn",
       "name_en": "Dong Son Bronze Spearhead",
       "summary_vi": "Vũ khí bằng đồng thau tiêu biểu của người Việt cổ.",
       "summary_en": "Typical bronze weapon of ancient Vietnamese people.",
       "description_vi": "Bài thuyết minh chi tiết tiếng Việt...",
       "description_en": "Detailed English narration...",
       "image_url": "/static/images/hv_001.jpg",
       "audio_vi_url": "/static/audio/ngoc-lu-thuyet-minh-vi.mp3",
       "audio_en_url": "/static/audio/ngoc-lu-thuyet-minh-en.mp3",
       "audio_duration_sec": 180,
       "translated_langs": "VI,EN",
       "map_x": 350,
       "map_y": 250,
       "status": "PUBLISHED"
     }
     ```

---

### 4.2. Phần việc của TV4: `W5_Beacons.tsx` & `W6_FloorMap.tsx`

#### A. Màn hình `W5_Beacons.tsx` (Quản lý thiết bị phát sóng iBeacon)
1. **Lấy số liệu 4 thẻ KPI trạng thái thiết bị đầu trang W5:**
   * **Endpoint:** `GET /api/beacons/stats`
   * **Dữ liệu trả về:**
     ```json
     {
       "total": 5,
       "active": 4,
       "low_battery": 1,
       "lost_signal": 0
     }
     ```
2. **Lấy danh sách thiết bị iBeacon:**
   * **Endpoint:** `GET /api/beacons`
   * **Dữ liệu trả về:** Mảng gồm `beacon_id`, `beacon_code` (`BC-001`...), `artifact_id`, `artifact_code`, `artifact_name_vi`, `zone_name`, `uuid`, `major`, `minor`, `trigger_distance`, `battery_level`, `map_x`, `map_y`, `status`.
3. **Thêm / Sửa / Xóa thiết bị iBeacon:**
   * **Thêm mới:** `POST /api/beacons`
   * **Cập nhật:** `PUT /api/beacons/{beacon_id}`
   * **Xóa:** `DELETE /api/beacons/{beacon_id}`

#### B. Màn hình `W6_FloorMap.tsx` (Bản đồ vị trí iBeacon & Khu di tích)
1. **Lấy thông tin sơ đồ mặt bằng & danh sách điểm ghim (Pin):**
   * **Endpoint:** `GET /api/sites/map`
   * **Dữ liệu trả về:** Thông tin `site` (kèm `map_image_url`: `"/static/images/floorplan_tang1.png"`) và mảng `pins` chứa tọa độ `map_x`, `map_y` của các trạm iBeacon.
2. **Cập nhật tọa độ $(X, Y)$ khi Admin click trên bản đồ:**
   * **Endpoint:** `PUT /api/beacons/{beacon_id}/coordinates`
   * **Body JSON gửi lên:**
     ```json
     {
       "map_x": 420,
       "map_y": 280,
       "trigger_distance": 2.0,
       "artifact_id": 1
     }
     ```

---

### 4.3. Phần việc của TV5: `W7_Feedbacks.tsx` & `W8_Users.tsx`

#### A. Màn hình `W7_Feedbacks.tsx` (Kiểm duyệt đánh giá & phản hồi)
1. **Lấy thống kê đánh giá & danh sách bình luận:**
   * **Endpoint:** `GET /api/feedbacks` *(Có thể lọc theo `?rating=5` hoặc `?is_visible=true`)*
   * **Dữ liệu trả về:**
     ```json
     {
       "stats": {
         "total_reviews": 3,
         "avg_rating": 4.7,
         "visible_count": 3,
         "hidden_count": 0
       },
       "items": [
         {
           "feedback_id": 1,
           "visitor_name": "Lê Hoàng Nam",
           "artifact_id": 1,
           "artifact_code": "HV-001",
           "artifact_name": "Trống đồng Ngọc Lũ",
           "rating": 5,
           "comment": "Âm thanh thuyết minh rất rõ ràng...",
           "is_visible": true,
           "created_at": "2026-03-30 10:15:00"
         }
       ]
     }
     ```
2. **Ẩn / Hiện bình luận trên ứng dụng Mobile:**
   * **Endpoint:** `PUT /api/feedbacks/{feedback_id}/visibility`
   * **Body JSON:** `{ "is_visible": false }` (hoặc `true`)
3. **Xóa vĩnh viễn bình luận vi phạm:**
   * **Endpoint:** `DELETE /api/feedbacks/{feedback_id}`

#### B. Màn hình `W8_Users.tsx` (Quản lý Ngôn ngữ & Tài khoản hệ thống)
1. **Lấy danh sách tài khoản:**
   * **Endpoint:** `GET /api/users` *(Hỗ trợ lọc `?role=ADMIN&search=...`)*
2. **Tạo tài khoản nhân viên / quản trị mới:**
   * **Endpoint:** `POST /api/users`
   * **Body JSON:**
     ```json
     {
       "full_name": "Trần Minh Tâm",
       "email": "minhtam@smartheritage.vn",
       "password": "123456",
       "role": "CONTENT_MANAGER",
       "preferred_lang": "vi",
       "is_active": true
     }
     ```
3. **Cập nhật vai trò, ngôn ngữ hoặc Khóa/Mở khóa tài khoản:**
   * **Endpoint:** `PUT /api/users/{user_id}`
   * **Body JSON:** `{ "role": "ADMIN", "preferred_lang": "en", "is_active": true }`

---

## 5. Kiểm tra trước khi tạo Pull Request vào `develop`

Trước khi chạy `git commit` và `git push`, các thành viên chạy lệnh kiểm tra biên dịch tại thư mục `admin-web/`:

```powershell
npm run build
```

Nếu lệnh chạy thành công (`built in ...s`) và không có lỗi đỏ TypeScript nào, bạn hãy đẩy nhánh lên GitHub và tạo Pull Request vào nhánh **`develop`** để Leader duyệt và gộp code!