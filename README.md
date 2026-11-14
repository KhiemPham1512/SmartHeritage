# 🏛️ Smart Heritage

> Hệ thống du lịch thông minh ứng dụng công nghệ **iBeacon (Bluetooth Low Energy)** giúp tự động nhận diện khoảng cách của khách tham quan với hiện vật lịch sử, phát thuyết minh đa ngôn ngữ và hỗ trợ ban quản lý số hóa, thống kê dữ liệu di tích.

---

## 1. Vấn đề và hướng giải quyết

Tại nhiều bảo tàng và khu di tích lịch sử ở Việt Nam, việc tiếp cận thông tin hiện vật vẫn còn nhiều rào cản:

| Vấn đề | Hậu quả |
| :--- | :--- |
| Bảng thông tin tĩnh và chỉ có tiếng Việt | Khách tham quan (đặc biệt là khách quốc tế và giới trẻ) tiếp nhận thụ động, khó hiểu sâu câu chuyện lịch sử |
| Quét mã QR thủ công ở từng hiện vật | Làm đứt mạch cảm xúc tham quan, bất tiện khi thiếu sáng hoặc đông người |
| Ban quản lý thiếu công cụ số hóa và đo lường | Không biết hiện vật nào được quan tâm nhất, khách dùng ngôn ngữ nào hay thời gian dừng chân bao lâu |

**Smart Heritage loại bỏ thao tác quét mã thủ công.** Thay vào đó, luồng trải nghiệm tự động của hệ thống là:

```text
Approach  -->  Detect (BLE)  -->  Resolve  -->  Experience  -->  Cache  -->  Analyze
Tiếp cận       Bắt sóng iBeacon   Tra cứu       Nghe/Xem         Lưu tạm     Thống kê
hiện vật       dưới 2.0 mét       đa ngữ        thuyết minh      xem tiếp    lượt tương tác
```

### Luồng tự động nhận diện iBeacon và thuyết minh (Điểm đặc trưng)

```mermaid
flowchart TD
    A[Thiết bị iBeacon đặt cạnh hiện vật<br/>phát sóng BLE: UUID, Major, Minor] --> B[Khách tham quan cầm điện thoại<br/>bước vào bán kính <= 2.0m]
    B --> C{Kiểm tra chống lặp<br/>Cooldown 5-10 phút?}
    C -- Đã bật gần đây --> D[Bỏ qua thông báo lặp<br/>tránh làm phiền khách]
    C -- Hiện vật mới --> E[Gọi FastAPI kèm mã Major/Minor<br/>và ngôn ngữ đã chọn VI/EN]
    E --> F[Trượt Popup hiện vật<br/>& Phát Audio Guide thuyết minh]
    F --> G[Lưu tạm vào bộ nhớ máy<br/>Room Database]
    G --> H{Khách di chuyển<br/>ra khỏi vùng sóng?}
    H -- Có --> I[Vẫn tiếp tục nghe/đọc<br/>không bị ngắt quãng]
    H -- Xem xong --> J[Gửi log thời gian tương tác<br/>về Dashboard quản trị]
```

> Hệ thống **tự động chống lặp thông báo (Duplicate Prevention)**: Khi khách đứng yên ngắm một hiện vật, ứng dụng chỉ bật gợi ý một lần duy nhất và chỉ kích hoạt lại khi khách bước sang vùng phủ sóng của hiện vật khác.

---

## 2. Tính năng (Phạm vi MVP trong 1 tháng)

| Nhóm | Chức năng |
| :--- | :--- |
| **Tài khoản & Đa ngữ** | Đăng ký / đăng nhập bằng email + password, chế độ **Khách vãng lai (Guest)** không cần đăng nhập, cập nhật hồ sơ, chọn ngôn ngữ thuyết minh (**Tiếng Việt / Tiếng Anh**), phân quyền `ADMIN` / `CONTENT_MANAGER` / `VISITOR` |
| **Nhận diện iBeacon** | Quét tín hiệu Bluetooth Low Energy (BLE) liên tục, ước lượng khoảng cách từ chỉ số RSSI, tự động bật **Bottom Sheet Popup** trong vòng $< 2\text{ s}$ khi cách hiện vật $\le 2.0\text{ m}$, chặn thông báo trùng lặp |
| **Đa phương tiện & Offline** | Xem mô tả ngắn (`summary`), câu chuyện lịch sử chi tiết (`description`), bộ sưu tập ảnh, video và trình phát **Audio Guide (`.mp3`)** theo ngôn ngữ đã chọn; tự động lưu đệm (Cache) để khách rời khỏi vùng sóng iBeacon vẫn nghe/đọc tiếp trọn vẹn |
| **Bản đồ di tích** | Hiển thị sơ đồ mặt bằng khu di tích với các điểm ghim (Pin) vị trí hiện vật; làm nổi bật vị trí hiện vật khách đang đứng gần; chạm vào ghim để xem thông tin |
| **Yêu thích & Lịch sử** | Lưu hiện vật yêu thích (Favorites), xem lại nhật ký các hiện vật đã đi qua (Visit History) hỗ trợ cả tài khoản đăng nhập và chế độ Guest |
| **Đánh giá & Phản hồi** | Chấm điểm $1\text{--}5$ sao và gửi nhận xét cho từng hiện vật; Admin kiểm duyệt (hiển thị / ẩn / xóa) phản hồi trên Web (`W7`) |
| **Quản trị CMS & iBeacon** | Quản lý khu di tích, thêm/sửa/xóa hiện vật đa ngôn ngữ (`HV-001`...), tải lên file ảnh/MP3 (`W3`, `W4`); đăng ký mã iBeacon (`BC-001`..., `UUID`, `Major`, `Minor`), gán iBeacon vào hiện vật và chấm tọa độ $(X, Y)$ lên sơ đồ (`W5`, `W6`) |
| **Thống kê & Báo cáo** | Dashboard (`W2`) trực quan hóa tổng lượt khách, biểu đồ cột Top 10 hiện vật phổ biến, biểu đồ đường lượt tương tác theo ngày, biểu đồ tròn Donut tỷ lệ ngôn ngữ sử dụng và xuất báo cáo **Excel/CSV (`.csv`)** / **PDF** |

---

## 3. Kiến trúc tổng quan và cách các công nghệ kết nối

```mermaid
flowchart LR
    subgraph Hardware["Hạ tầng tại Di tích"]
        BLE["Thiết bị iBeacon /<br/>App Beacon Simulator"]
    end

    subgraph Clients["Giao diện Người dùng"]
        APP["Android App<br/>Kotlin + Jetpack Compose<br/>+ Room Cache"]
        WEB["Admin Web Portal<br/>React + TypeScript<br/>+ Vite"]
    end

    subgraph Backend["Server Trung tâm"]
        API["FastAPI (Python 3.12)<br/>RESTful API + JWT Auth<br/>+ Media Static Server"]
    end

    subgraph DataLayer["Lưu trữ Dữ liệu"]
        DB[("SQL Server<br/>SmartHeritageDB")]
        FILES["Thư mục Static Media<br/>(Ảnh, Audio MP3, Sơ đồ)"]
    end

    BLE -. "1. Phát sóng Bluetooth 2.4GHz<br/>(UUID, Major, Minor, RSSI)" .-> APP
    APP -- "2. Gửi Major/Minor + Lang<br/>(HTTP REST JSON)" --> API
    APP -- "3. Stream trực tiếp MP3 / Ảnh" --> FILES
    APP -- "4. Gửi Log tương tác & Đánh giá" --> API

    WEB -- "5. Quản lý Hiện vật, Gán Beacon<br/>& Xem Thống kê (REST + JWT)" --> API
    API -- "6. Truy vấn SQLAlchemy / pyodbc" --> DB
    API -- "7. Lưu / Đọc file đa phương tiện" --> FILES
```

### Quy tắc kết nối giữa các thành phần

> **iBeacon chỉ phát mã số qua Bluetooth** $\rightarrow$ **App Android bắt mã số đó hỏi FastAPI** $\rightarrow$ **FastAPI tra bảng SQL Server trả về nội dung và link thuyết minh MP3.**

| Việc cần làm | Luồng kết nối | Giải thích kỹ thuật |
| :--- | :--- | :--- |
| Phát tín hiệu vị trí hiện vật | `iBeacon` $\rightarrow$ `Android App` (qua sóng Bluetooth BLE) | Cục iBeacon chỉ phát quảng bá 1 chiều (`UUID`, `Major`, `Minor`), không cần mạng Internet |
| Biết cục iBeacon đó là hiện vật gì | `Android App` $\rightarrow$ `FastAPI` (`GET /api/beacons/resolve`) | App gửi `Major`, `Minor` và ngôn ngữ (`vi`/`en`) lên Server để lấy đúng bài viết, ảnh và link MP3 |
| Nghe thuyết minh & xem ảnh | `Android App` $\rightarrow$ `FastAPI Static Media` (`/static/...`) | Tải ảnh và phát trực tiếp (stream) file `.mp3` qua trình phát `ExoPlayer` |
| Xem tiếp khi đã đi xa hiện vật | `Android App` $\rightarrow$ `Room Database` (Bộ nhớ trong điện thoại) | Nội dung vừa mở được lưu tạm trên máy, mất sóng Bluetooth vẫn nghe tiếp đến hết bài |
| Ghi nhận lượt khách & thời gian xem | `Android App` $\rightarrow$ `FastAPI` (`POST /api/visit-logs`) | Khi khách rời hiện vật, App gửi số giây đã xem lên Server để cộng vào biểu đồ thống kê |
| Thêm hiện vật, tải MP3 & gán Beacon | `Admin Web` $\rightarrow$ `FastAPI` $\rightarrow$ `SQL Server` | Giúp ban quản lý cập nhật nội dung mới từ trình duyệt mà không cần sửa code ứng dụng di động |
| Xem biểu đồ & xuất báo cáo Excel/CSV | `Admin Web` $\rightarrow$ `FastAPI` (`GET /api/analytics/dashboard`) | Server tổng hợp dữ liệu bằng các câu lệnh `GROUP BY` từ SQL Server trả về cho Web hiển thị |

---

## 4. Công nghệ sử dụng & Phiên bản cần thiết

### Phần mềm nền tảng và Phiên bản bắt buộc

| Phần mềm / Công cụ | Phiên bản cần thiết | Thành viên cần cài | Lưu ý khi cài đặt |
| :--- | :--- | :--- | :--- |
| **Python** | **Python 3.12.x (64-bit)** *(Khuyên dùng `3.12.10`)* | TV1, TV3, TV4, TV5 | **Không dùng Python 3.14 hoặc 3.15** (vì chưa có sẵn gói wheel `.whl` cho `pyodbc` và `pydantic-core` trên Windows). Khi cài nhớ tích chọn **"Add python.exe to PATH"**. |
| **SQL Server & SSMS** | **SQL Server 2019 / 2022** (Developer hoặc Express) + **SSMS 19/20** | Cả 5 thành viên | Đi kèm sẵn `ODBC Driver 17/18 for SQL Server` để kết nối với Backend. |
| **Node.js** | **Node.js 20.x LTS** (hoặc `18.x / 22.x LTS`) | TV1, TV3, TV4, TV5 | Dùng để chạy cổng quản trị React + TypeScript + Vite (`admin-web/`). |
| **Android Studio** | **Android Studio Koala / Ladybug (2024.x+)** + **JDK 17** | TV2, TV1 | Cần 1 điện thoại Android thật để quét Bluetooth và 1 điện thoại phụ cài app **Beacon Simulator**. |
| **Git & VS Code** | Bản mới nhất | Cả 5 thành viên | Quản lý nhánh theo mô hình `main` - `develop` - `feature/*`. |

### Backend (`backend/requirements.txt`)

| Mục đích | Công nghệ & Phiên bản |
| :--- | :--- |
| Framework RESTful API | `fastapi==0.115.0` + `uvicorn[standard]==0.30.6` (Tự động sinh Swagger UI tại `/docs`) |
| Kết nối Cơ sở dữ liệu | `sqlalchemy==2.0.35` + `pyodbc==5.1.0` |
| Kiểm tra dữ liệu & Biến môi trường | `pydantic==2.9.2` + `python-dotenv==1.0.1` |
| Xác thực & Phân quyền | `HTTPBearer JWT (HS256)` + `SHA-256 Password Hashing` + `RBAC` (`app/auth.py`) |
| Upload & Phục vụ file Ảnh / MP3 | `python-multipart==0.0.12` + `FastAPI StaticFiles` |
| Xuất báo cáo thống kê | `StreamingResponse CSV (UTF-8-SIG BOM)` tương thích trực tiếp với Microsoft Excel |

### Cơ sở dữ liệu (`SmartHeritageDB` - Trọng tâm học phần Hệ quản trị CSDL)

| Mục đích | Kỹ thuật Hệ quản trị CSDL áp dụng |
| :--- | :--- |
| Kiến trúc 3 mức trừu tượng | Mức Vật lý (`Clustered / Non-Clustered Index`), Mức Luận lý (`6 bảng + Constraints`), Mức Khung nhìn (`vw_PublicArtifacts_VI_EN`, `vw_BeaconHealthMonitor`) |
| Quản lý Giao tác & Khóa (ACID) | `BEGIN TRAN ... COMMIT / ROLLBACK TRAN`, kiểm tra biến hệ thống `@@ERROR`, `@@ROWCOUNT`, `@@TRANCOUNT` |
| Chống 4 vấn đề truy xuất đồng thời | `READ COMMITTED` (chống *Dirty Read*), `REPEATABLE READ` (chống *Non-Repeatable Read*), `SERIALIZABLE` (chống *Phantom Read*), và `WITH (UPDLOCK)` (nâng cấp khóa `ULock` chống *Lost Update*) |
| Lập trình T-SQL nâng cao | Vòng lặp `WHILE + IF NOT EXISTS` cấp mã tự động (`sp_GenerateNextArtifactCode`), Con trỏ `CURSOR + @@FETCH_STATUS` (`sp_AuditBeaconHealth_Cursor`), cấu trúc rẽ nhánh `CASE ... WHEN` |

### Admin Web (`admin-web/`)

| Mục đích | Công nghệ |
| :--- | :--- |
| Framework & Build Tool | `React 19` + `Vite` + `TypeScript 5.7` |
| Kết nối RESTful API | Module `src/api.ts` (`apiFetch<T>`) tự động đính kèm `Bearer JWT Token` và xử lý lỗi tập trung |
| Kiến trúc Cô lập Module (File Isolation) | Tách biệt 8 màn hình độc lập trong `src/pages/W1_Login.tsx` $\rightarrow$ `W8_Users.tsx`, tích hợp tài nguyên đồ họa vector `.svg` chuẩn Figma trong `public/` |

---

## 5. Thiết kế cơ sở dữ liệu

Schema gồm **6 bảng cốt lõi**, **3 Non-Clustered Indexes**, **2 Views** và **7 Stored Procedures** trong SQL Server (`database/init_schema.sql`), đồng bộ 100% với thiết kế Figma Web (`W1`–`W8`) và Mobile (`1`–`10`).

```mermaid
erDiagram
    SITES ||--o{ ARTIFACTS : "chứa"
    ARTIFACTS ||--o| BEACONS : "được gắn"
    USERS ||--o{ VISIT_LOGS : "tạo lịch sử"
    ARTIFACTS ||--o{ VISIT_LOGS : "được xem"
    BEACONS ||--o{ VISIT_LOGS : "kích hoạt"
    USERS ||--o{ FEEDBACKS : "đánh giá"
    ARTIFACTS ||--o{ FEEDBACKS : "nhận đánh giá"

    USERS {
        int user_id PK
        varchar email UK
        varchar password_hash
        nvarchar full_name
        varchar role "ADMIN | CONTENT_MANAGER | VISITOR"
        varchar preferred_lang "vi | en | ko | ja | zh"
        bit auto_beacon_notify
        bit is_active
    }
    SITES {
        int site_id PK
        nvarchar name_vi
        nvarchar name_en
        nvarchar floor_name
        varchar map_image_url
    }
    ARTIFACTS {
        int artifact_id PK
        int site_id FK
        varchar artifact_code UK "HV-001.."
        nvarchar zone_name
        nvarchar name_vi
        nvarchar name_en
        nvarchar summary_vi
        nvarchar summary_en
        nvarchar description_vi
        nvarchar description_en
        varchar image_url
        varchar audio_vi_url
        varchar audio_en_url
        int audio_duration_sec
        float map_x
        float map_y
        varchar status "PUBLISHED | DRAFT | HIDDEN"
    }
    BEACONS {
        int beacon_id PK
        varchar beacon_code UK "BC-001.."
        int artifact_id FK
        varchar uuid
        int major
        int minor
        float trigger_distance
        int battery_level
        datetime2 last_seen_at
        varchar status "ACTIVE | LOW_BATTERY | LOST_SIGNAL"
    }
    VISIT_LOGS {
        int log_id PK
        int user_id FK
        varchar device_id
        int artifact_id FK
        int beacon_id FK
        varchar lang_used
        int duration_seconds
        bit is_favorite
        datetime2 visited_at
    }
    FEEDBACKS {
        int feedback_id PK
        int user_id FK
        nvarchar visitor_name
        int artifact_id FK
        tinyint rating "1..5"
        nvarchar comment
        bit is_visible
        datetime2 created_at
    }
```

---

## 6. Cơ chế kỹ thuật & Bảo mật

* **Lọc nhiễu tín hiệu BLE & Chống lặp thông báo:** Sóng Bluetooth rất dễ dao động; ứng dụng kết hợp kiểm tra ngưỡng khoảng cách kích hoạt (`trigger_distance` $\le 2.0\text{ m}$) và biến khóa thời gian (`Cooldown Timer`) để không bật thông báo lặp lại làm phiền khách khi họ vẫn đứng tại cùng một hiện vật.
* **Xem liên tục ngoài vùng phủ sóng:** Ngay khi hiện vật được kích hoạt, nội dung văn bản và đường dẫn âm thanh được lưu vào `Room Database` trên điện thoại, giúp khách vừa đi dạo sang khu vực khác vừa nghe hết bài thuyết minh.
* **Đảm bảo tính Nhất quán & Cô lập giao tác (ACID):** Các thao tác gán iBeacon, cập nhật tọa độ sơ đồ và cộng dồn thời gian tham quan được bảo vệ bằng giao tác `BEGIN TRAN ... COMMIT TRAN` kết hợp kỹ thuật nâng cấp khóa `WITH (UPDLOCK)` và các mức cô lập `READ COMMITTED`, `REPEATABLE READ`, `SERIALIZABLE`.
* **Bảo mật & Phân quyền (RBAC):** Xác thực người dùng bằng `HTTPBearer` JWT Token (`HS256`) và phân quyền 3 cấp độ: `ADMIN`, `CONTENT_MANAGER`, `VISITOR`.

---

## 7. Giao diện (Thiết kế Figma)

Thiết kế gồm **10 màn hình Mobile (`1`–`10`)** và **8 màn hình Web Admin (`W1`–`W8`)**:

| Nhóm màn hình | Nội dung & Phân công |
| :--- | :--- |
| **Mobile: Khởi động & Tài khoản (`1`–`3`)** | `1.` Chào mừng và chọn ngôn ngữ, `2.` Đăng nhập (hoặc *Tiếp tục với tư cách Khách*), `3.` Tạo tài khoản *(TV2 phụ trách)* |
| **Mobile: Trang chủ & Rada BLE (`4`–`5`)** | `4.` Trang chủ và Rada dò iBeacon, `5.` Popup tự động trượt lên khi tới gần hiện vật *(TV2 phụ trách)* |
| **Mobile: Trải nghiệm Hiện vật (`6`, `8`)** | `6.` Chi tiết hiện vật và Trình phát Audio Guide MP3, `8.` Hộp thoại Chia sẻ cảm nhận (chấm 1–5 sao) *(TV2 phụ trách)* |
| **Mobile: Bản đồ & Cá nhân (`7`, `9`, `10`)** | `7.` Bản đồ tương tác sơ đồ di tích, `9.` Yêu thích và Lịch sử tham quan, `10.` Hồ sơ cá nhân & công tắc thông báo iBeacon *(TV2 phụ trách)* |
| **Admin Web: Đăng nhập & Thống kê (`W1`, `W2`)** | `W1.` Đăng nhập quản trị, `W2.` Dashboard Thống kê, biểu đồ Top 10 hiện vật, lượt khách theo ngày, ngôn ngữ & xuất báo cáo Excel/PDF *(TV1 - Leader đã hoàn thành)* |
| **Admin Web: Quản trị Hiện vật CMS (`W3`, `W4`)** | `W3.` Quản lý danh sách hiện vật & bộ lọc, `W4.` Thêm/sửa hiện vật đa ngôn ngữ & tải file Ảnh + MP3 *(TV3 phụ trách)* |
| **Admin Web: Hạ tầng iBeacon & Bản đồ (`W5`, `W6`)** | `W5.` Quản lý thiết bị iBeacon (`BC-001`...), `W6.` Bản đồ vị trí iBeacon & chấm tọa độ khu di tích *(TV4 phụ trách)* |
| **Admin Web: Kiểm duyệt & Tài khoản (`W7`, `W8`)** | `W7.` Kiểm duyệt đánh giá & phản hồi (Ẩn/Hiện/Xóa), `W8.` Quản lý ngôn ngữ & tài khoản hệ thống *(TV5 phụ trách)* |

---

## 8. Cấu trúc repository (Cập nhật theo tiến độ thực tế)

```text
SmartHeritage/
├── admin-web/                              # Cổng quản trị Web (React + Vite + TypeScript)
│   ├── public/                             # Tài nguyên hình ảnh & biểu tượng SVG chuẩn Figma
│   │   ├── smart-heritage-background.png   # Hình nền kiến trúc mái đền màn hình W1
│   │   ├── logo_cong_dinh_tron.svg         # Logo cổng đình tròn vàng kim
│   │   ├── logo_cong_dinh_tron_do.svg      # Logo cổng đình tròn đỏ đô
│   │   ├── logo_bao_tang.svg               # Biểu tượng thẻ Bảo tàng Di sản Việt
│   │   ├── thong_ke.svg                    # Icon menu W2 (Thống kê & báo cáo)
│   │   ├── quan_ly_hien_vat.svg            # Icon menu W3-W4 (Quản lý hiện vật)
│   │   ├── thiet_bi_ibeacon.svg            # Icon menu W5 (Thiết bị iBeacon)
│   │   ├── ban_do.svg                      # Icon menu W6 (Bản đồ khu di tích)
│   │   ├── danh_gia_phan_hoi.svg           # Icon menu W7 (Đánh giá & phản hồi)
│   │   └── ngon_ngu.svg                    # Icon menu W8 (Ngôn ngữ & tài khoản)
│   ├── src/
│   │   ├── pages/                          # Các màn hình tách biệt độc lập (Chống xung đột Git)
│   │   │   ├── W1_Login.tsx                # [TV1 - Hoàn thành] Màn hình Đăng nhập quản trị
│   │   │   ├── W2_Dashboard.tsx            # [TV1 - Hoàn thành] Màn hình Thống kê & Xuất báo cáo
│   │   │   ├── W3_Artifacts.tsx            # [TV3 phụ trách] Màn hình Danh sách hiện vật
│   │   │   ├── W4_ArtifactForm.tsx         # [TV3 phụ trách] Màn hình Thêm/Sửa hiện vật & Upload
│   │   │   ├── W5_Beacons.tsx              # [TV4 phụ trách] Màn hình Quản lý thiết bị iBeacon
│   │   │   ├── W6_FloorMap.tsx             # [TV4 phụ trách] Màn hình Bản đồ & Tọa độ iBeacon
│   │   │   ├── W7_Feedbacks.tsx            # [TV5 phụ trách] Màn hình Kiểm duyệt đánh giá
│   │   │   └── W8_Users.tsx                # [TV5 phụ trách] Màn hình Quản lý tài khoản & Ngôn ngữ
│   │   ├── api.ts                          # [TV1] Cấu hình apiFetch & tự động đính kèm JWT Token
│   │   ├── App.tsx                         # [TV1] Khung Sidebar & Header điều hướng chung W1 -> W8
│   │   ├── index.css                       # [TV1] Định dạng CSS toàn màn hình & khử răng cưa chữ
│   │   └── main.tsx                        # Điểm khởi chạy ứng dụng React DOM
│   ├── package.json                        # Khai báo các gói thư viện và script chạy Web
│   └── README.md                           # Tài liệu hướng dẫn code W3-W8 cho TV3, TV4, TV5
├── backend/                                # Server Python FastAPI (Đã hoàn thành 100% M0 -> M7)
│   ├── app/
│   │   ├── routers/
│   │   │   ├── auth_router.py              # [M1] API Đăng nhập, Đăng ký, Guest, Hồ sơ & Quản lý Users
│   │   │   ├── artifact_router.py          # [M2] API Quản lý Hiện vật, Thống kê thẻ W3 & Upload Ảnh/MP3
│   │   │   ├── beacon_router.py            # [M3] API Quản lý iBeacon, Thống kê thẻ W5 & Sơ đồ tọa độ
│   │   │   └── analytics_router.py         # [M6-M7] API Lịch sử, Yêu thích, Đánh giá, Kiểm duyệt & CSV
│   │   ├── auth.py                         # [M1] Xử lý băm mật khẩu & tạo/kiểm tra HTTPBearer JWT Token
│   │   ├── database.py                     # [M0] Tự động dò ODBC Driver & kết nối SQL Server
│   │   ├── main.py                         # [M0-M7] Khởi tạo FastAPI, CORS, Static Media & Gắn 4 Routers
│   │   └── schemas.py                      # [M1-M7] Khuôn mẫu dữ liệu Pydantic cho toàn bộ phân hệ
│   ├── static/
│   │   ├── audio/                          # [M0] 10 file thuyết minh âm thanh mẫu (.mp3)
│   │   └── images/                         # [M0] 5 ảnh hiện vật (hv_001..005.jpg) & sơ đồ mặt bằng
│   ├── .env.example                        # [M0] File mẫu cấu hình kết nối SQL Server
│   ├── requirements.txt                    # [M0] Danh sách thư viện Python chuẩn của Backend
│   └── seed_media.py                       # Script tự động sinh bộ file ảnh & âm thanh mẫu
├── database/
│   └── init_schema.sql                     # [M0] Script khởi tạo SmartHeritageDB (6 bảng, 3 Index, 2 View, 7 SP)
├── mobile/                                 # [TV2 phụ trách] Ứng dụng Android (Kotlin + Jetpack Compose)
├── .gitignore                              # Chặn .venv, __pycache__, .env, node_modules, dist, build
└── README.md                               # Tài liệu tổng quan và hướng dẫn cài đặt dự án
```

---

## 9. Hướng dẫn cài đặt và chạy dự án (Chuẩn hóa cho mọi máy trong nhóm)

Bất kỳ thành viên nào sau khi `git clone` dự án (hoặc `git pull origin develop`) về máy chỉ cần thực hiện đúng **3 bước** dưới đây:

### Bước 1: Khởi tạo Cơ sở dữ liệu trên SQL Server (SSMS)

1. Mở **SQL Server Management Studio (SSMS)** và kết nối vào Server của máy bạn.
   * *Ghi nhớ tên **Server name** ở màn hình đăng nhập SSMS (ví dụ: `.\SQLEXPRESS`, `localhost`, hoặc `TEN_MAY\SQLEXPRESS`).*
2. Chọn **File** $\rightarrow$ **Open** $\rightarrow$ **File...**, mở file **`database/init_schema.sql`** trong thư mục dự án.
3. Nhấn **`F5` (Execute)** để tạo CSDL `SmartHeritageDB` (bao gồm 6 bảng, 3 Indexes, 2 Views, 7 Stored Procedures xử lý giao tác đồng thời và dữ liệu mẫu).

### Bước 2: Thiết lập Môi trường ảo Python 3.12 & Khởi chạy Backend FastAPI (Terminal 1)

> **Yêu cầu:** Máy đã cài đặt **Python 3.12 (64-bit)** từ [python.org/downloads/windows](https://www.python.org/downloads/windows/) (có tích chọn *"Add python.exe to PATH"*).

Mở cửa sổ Terminal thứ nhất trong VS Code tại thư mục gốc dự án và chạy lần lượt:

```powershell
# 1. Di chuyển vào thư mục backend
cd backend

# 2. Tạo môi trường ảo chuẩn Windows bằng Python 3.12
py -3.12 -m venv .venv

# 3. Cấp quyền chạy script trên PowerShell (nếu dùng CMD thì bỏ qua dòng này)
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

# 4. Kích hoạt môi trường ảo (Nếu dùng CMD thì gõ: .venv\Scripts\activate)
.\.venv\Scripts\Activate.ps1

# 5. Cài đặt các thư viện trong requirements.txt
pip install -r requirements.txt

# 6. Tạo bộ ảnh và âm thanh mẫu vào thư mục static/ (nếu chưa có)
python seed_media.py
```

Tiếp theo, tạo file **`backend/.env`** (copy từ `backend/.env.example`) và điền đúng tên `DB_SERVER` của máy bạn:

```env
USE_WINDOWS_AUTH=true
DB_SERVER=.\SQLEXPRESS
DB_NAME=SmartHeritageDB
```

Sau đó khởi chạy Server FastAPI (giữ cửa sổ Terminal này chạy ngầm):

```powershell
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

* Kiểm tra Swagger UI tại: **`http://localhost:8000/docs`**
* Kiểm tra kết nối Database tại: **`http://localhost:8000/api/health-db`**

### Bước 3: Cài đặt & Khởi chạy Cổng Quản trị `admin-web` (Terminal 2)

> **Yêu cầu:** Máy đã cài đặt **Node.js (LTS)** từ [nodejs.org](https://nodejs.org/). Giữ nguyên Terminal 1 đang chạy FastAPI ở cổng `8000` để Web có thể lấy dữ liệu.

Mở một **cửa sổ Terminal thứ hai** trong VS Code (bấm dấu `+` ở góc phải khung Terminal) và chạy:

```powershell
# 1. Di chuyển vào thư mục admin-web
cd admin-web

# 2. Cài đặt các gói thư viện Node.js
npm install

# 3. Khởi chạy Vite Dev Server
npm run dev
```

* Mở trình duyệt truy cập địa chỉ: **`http://localhost:5173/`**
* Đăng nhập tại màn hình **W1** bằng tài khoản Admin mẫu:
  * **Email:** `ngocanh@smartheritage.vn`
  * **Mật khẩu:** `123456`
* Sau khi đăng nhập, hệ thống chuyển vào màn hình **W2 (Thống kê & báo cáo)** và thanh Sidebar điều hướng sang các màn hình **`W3` $\rightarrow$ `W8`**.
* **Đối với TV3, TV4, TV5:** Xem chi tiết quy tắc code và mẫu câu lệnh gọi API tại file **`admin-web/README.md`**.

---

## 10. Lộ trình triển khai (30 ngày)

| Cột mốc | Thời gian | Nội dung công việc | Trạng thái |
| :--- | :--- | :--- | :--- |
| **M0** | Ngày 1–3 | Khởi tạo repo, thiết kế Figma, tạo CSDL SQL Server (`init_schema.sql`), bộ file mẫu (`seed_media.py`) & khung FastAPI | Đã hoàn thành |
| **M1** | Ngày 4–6 | API Đăng ký, đăng nhập, chế độ Khách (Guest), chọn ngôn ngữ, quản lý người dùng (`auth.py`, `auth_router.py`) & Màn hình Mobile `1–3` | Đã hoàn thành |
| **M2** | Ngày 7–10 | API Quản trị hiện vật đa ngôn ngữ, upload ảnh/MP3 (`artifact_router.py`) & Giao diện Web `W3`, `W4` | Backend xong / TV3 đang ghép UI |
| **M3** | Ngày 11–13 | API Hạ tầng thiết bị iBeacon (`UUID`, `Major`, `Minor`), gán hiện vật, cấu hình bán kính & tọa độ sơ đồ (`beacon_router.py`) & Giao diện Web `W5`, `W6` | Backend xong / TV4 đang ghép UI |
| **M4** | Ngày 14–18 | Nhận diện iBeacon tự động trên Mobile: quét sóng BLE, lọc khoảng cách $\le 2.0\text{ m}$, chống lặp thông báo & tự bật Popup (`Màn 4–5`) | TV2 đang triển khai |
| **M5** | Ngày 19–22 | Trải nghiệm Đa phương tiện & Bản đồ trên Mobile: Trình phát Audio Guide `ExoPlayer`, lưu tạm `Room DB` & sơ đồ di tích (`Màn 6–7`) | TV2 đang triển khai |
| **M6** | Ngày 23–25 | Tương tác khách tham quan: API Yêu thích (Favorites), Lịch sử tham quan (Visit History), chấm sao & đánh giá (`analytics_router.py`) | Backend xong / TV2 ghép Mobile |
| **M7** | Ngày 26–28 | Thống kê & Quản trị nâng cao: Khung `admin-web`, màn hình `W1` (Login), `W2` (Dashboard & Xuất CSV/PDF), `W7` (Duyệt đánh giá) & `W8` (Tài khoản) | Backend + W1, W2 xong / TV5 ghép W7, W8 |
| **M8** | Ngày 29–30 | Kiểm thử tích hợp toàn hệ thống Mobile + Web + Backend, hoàn thiện báo cáo & quay video demo bảo vệ đồ án | Chuẩn bị |

---

## 11. Phân công nhiệm vụ các thành viên

| Thành viên | Vai trò chính | Phạm vi Module & Tệp tin phụ trách |
| :--- | :--- | :--- |
| **Phạm Gia Khiêm** (Leader - TV1) | Database Architect, Backend Developer & Web Lead | Thiết kế `database/init_schema.sql` (6 bảng, 3 Index, 2 View, 7 SP), phát triển toàn bộ Backend FastAPI (`M0` $\rightarrow$ `M7`), dựng khung `admin-web/` (`api.ts`, `App.tsx`) và hoàn thiện màn hình **`W1` (`W1_Login.tsx`)**, **`W2` (`W2_Dashboard.tsx`)** |
| **Nguyễn Tuấn Khang** (TV2) | Mobile Android Developer | Phát triển độc lập ứng dụng Android trong thư mục `mobile/` (Kotlin, Jetpack Compose, quét sóng BLE iBeacon, `ExoPlayer` Audio Guide, `Room Database` cho 10 màn hình Mobile `1`–`10`) |
| **Minh Tâm** (TV3) | Web Admin CMS Developer (`W3`, `W4`) | Lập trình cụm Quản trị Hiện vật đa ngôn ngữ & Upload file Ảnh/MP3 tại 2 tệp: `admin-web/src/pages/W3_Artifacts.tsx` và `admin-web/src/pages/W4_ArtifactForm.tsx` |
| **Thành viên 4** (TV4) | Web Admin iBeacon & Map Developer (`W5`, `W6`) | Lập trình cụm Quản lý Thiết bị iBeacon & Bản đồ chấm tọa độ $(X, Y)$ khu di tích tại 2 tệp: `admin-web/src/pages/W5_Beacons.tsx` và `admin-web/src/pages/W6_FloorMap.tsx` |
| **Thành viên 5** (TV5) | Web Admin Moderation & Users Developer (`W7`, `W8`) | Lập trình cụm Kiểm duyệt Đánh giá & Quản lý Tài khoản/Ngôn ngữ tại 2 tệp: `admin-web/src/pages/W7_Feedbacks.tsx` và `admin-web/src/pages/W8_Users.tsx`; đồng thời phụ trách thiết kế Figma & dữ liệu mẫu |