# FILE: backend/app/schemas.py
from typing import Optional
from pydantic import BaseModel, Field


# =========================================================================
# M1: SCHEMAS CHO TÀI KHOẢN, ĐĂNG NHẬP & QUẢN LÝ NGƯỜI DÙNG (W1, W8, Mobile 1, 2, 3, 10)
# =========================================================================
class LoginRequest(BaseModel):
    email: str = Field(..., examples=["ngocanh@smartheritage.vn"])
    password: str = Field(..., examples=["123456"])


class RegisterRequest(BaseModel):
    full_name: str = Field(..., examples=["Nguyễn Văn B"])
    email: str = Field(..., examples=["nguyenvanb@email.com"])
    password: str = Field(..., min_length=6, examples=["123456"])
    preferred_lang: str = Field("vi", examples=["vi"])


class UserProfileUpdate(BaseModel):
    full_name: Optional[str] = None
    preferred_lang: Optional[str] = Field(None, description="vi, en, ko, ja, zh")
    auto_beacon_notify: Optional[bool] = None
    avatar_url: Optional[str] = None


class AdminUserCreate(BaseModel):
    email: str
    password: str = "123456"
    full_name: str
    role: str = Field("CONTENT_MANAGER", description="ADMIN, CONTENT_MANAGER, VISITOR")
    preferred_lang: str = "vi"
    is_active: bool = True


class AdminUserUpdate(BaseModel):
    full_name: Optional[str] = None
    role: Optional[str] = None
    preferred_lang: Optional[str] = None
    is_active: Optional[bool] = None


# =========================================================================
# M2: SCHEMAS CHO QUẢN LÝ HIỆN VẬT & ĐA NGÔN NGỮ (W3, W4, Mobile 4, 5, 6, 7)
# =========================================================================
class ArtifactCreate(BaseModel):
    site_id: int = 1
    artifact_code: str = Field(..., examples=["HV-006"])
    zone_name: str = Field("Khu trưng bày Đông Sơn - Tầng 1")
    name_vi: str = Field(..., examples=["Mũi giáo đồng Đông Sơn"])
    name_en: str = Field(..., examples=["Dong Son Bronze Spearhead"])
    summary_vi: str = Field(..., examples=["Vũ khí bằng đồng thau tiêu biểu của cư dân Lạc Việt."])
    summary_en: str = Field(..., examples=["Typical bronze weapon of the ancient Lac Viet people."])
    description_vi: str
    description_en: str
    image_url: Optional[str] = "/static/images/hv_001.jpg"
    gallery_json: Optional[str] = None
    audio_vi_url: Optional[str] = "/static/audio/ngoc-lu-thuyet-minh-vi.mp3"
    audio_en_url: Optional[str] = "/static/audio/ngoc-lu-thuyet-minh-en.mp3"
    audio_duration_sec: int = 180
    translated_langs: str = "VI,EN"
    map_x: float = 350.0
    map_y: float = 250.0
    status: str = Field("PUBLISHED", description="PUBLISHED, DRAFT, HIDDEN")


class ArtifactUpdate(BaseModel):
    zone_name: Optional[str] = None
    name_vi: Optional[str] = None
    name_en: Optional[str] = None
    summary_vi: Optional[str] = None
    summary_en: Optional[str] = None
    description_vi: Optional[str] = None
    description_en: Optional[str] = None
    image_url: Optional[str] = None
    gallery_json: Optional[str] = None
    audio_vi_url: Optional[str] = None
    audio_en_url: Optional[str] = None
    audio_duration_sec: Optional[int] = None
    translated_langs: Optional[str] = None
    map_x: Optional[float] = None
    map_y: Optional[float] = None
    status: Optional[str] = None


# =========================================================================
# M3: SCHEMAS CHO QUẢN LÝ THIẾT BỊ IBEACON & BẢN ĐỒ DI TÍCH (W5, W6, Mobile 7)
# =========================================================================
class BeaconCreate(BaseModel):
    beacon_code: str = Field(..., examples=["BC-006"])
    artifact_id: Optional[int] = Field(None, description="ID hiện vật được gán (nếu có)")
    zone_name: str = Field("Tầng 1 - Phòng Đông Sơn")
    uuid: str = Field("FDA50693-A4E2-4FB1-AFCF-C6EB07647825")
    major: int = Field(1, examples=[1])
    minor: int = Field(..., examples=[106])
    tx_power: int = -59
    trigger_distance: float = Field(2.0, description="Bán kính phát sóng (m)")
    battery_level: int = Field(100, ge=0, le=100)
    map_x: float = 340.0
    map_y: float = 215.0
    status: str = Field("ACTIVE", description="ACTIVE, LOW_BATTERY, LOST_SIGNAL")


class BeaconUpdate(BaseModel):
    beacon_code: Optional[str] = None
    artifact_id: Optional[int] = None
    zone_name: Optional[str] = None
    uuid: Optional[str] = None
    major: Optional[int] = None
    minor: Optional[int] = None
    tx_power: Optional[int] = None
    trigger_distance: Optional[float] = None
    battery_level: Optional[int] = None
    map_x: Optional[float] = None
    map_y: Optional[float] = None
    status: Optional[str] = None


class BeaconCoordinateUpdate(BaseModel):
    artifact_id: Optional[int] = None
    zone_name: Optional[str] = None
    trigger_distance: Optional[float] = 2.0
    map_x: float = Field(..., examples=[340.0])
    map_y: float = Field(..., examples=[215.0])


class SiteMapUpdate(BaseModel):
    floor_name: Optional[str] = Field(None, examples=["Tầng 1 - Khu trưng bày chính"])
    map_image_url: str = Field(..., examples=["/static/images/floorplan_tang1.png"])


# =========================================================================
# M6 & M7: SCHEMAS CHO LỊCH SỬ, YÊU THÍCH, ĐÁNH GIÁ & KIỂM DUYỆT (W2, W7, Mobile 8, 9)
# =========================================================================
class VisitLogCreate(BaseModel):
    user_id: Optional[int] = None
    device_id: Optional[str] = Field("android-guest-01", description="Định danh máy khi dùng chế độ Guest")
    artifact_id: int = Field(..., examples=[1])
    beacon_id: Optional[int] = Field(1, examples=[1])
    lang_used: str = Field("vi", examples=["vi"])
    duration_seconds: int = Field(60, ge=1, examples=[120])
    is_favorite: bool = False


class FavoriteToggleRequest(BaseModel):
    user_id: Optional[int] = None
    device_id: Optional[str] = Field("android-guest-01")
    artifact_id: int = Field(..., examples=[1])


class FeedbackCreate(BaseModel):
    user_id: Optional[int] = None
    visitor_name: str = Field("Khách tham quan", examples=["Nguyễn Văn A"])
    artifact_id: int = Field(..., examples=[1])
    rating: int = Field(..., ge=1, le=5, examples=[5])
    comment: Optional[str] = Field(None, examples=["Thuyết minh rất hay và dễ hiểu!"])


class FeedbackVisibilityUpdate(BaseModel):
    is_visible: bool = Field(..., description="True = Hiển thị (Duyệt), False = Đã ẩn")