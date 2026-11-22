package com.example.smartheritage_mobile.config

data class AppStrings(
    val isEn: Boolean,
    // WelcomeScreen (M1)
    val welcomeTitle: String = if (isEn) "Heritage at Your Fingertips" else "Di sản trong tầm tay",
    val welcomeSubtitle: String = if (isEn) {
        "Explore artifacts, listen to historical stories, and save your journey."
    } else {
        "Khám phá hiện vật, lắng nghe những câu chuyện lịch sử và lưu lại hành trình của bạn."
    },
    val startJourneyLabel: String = if (isEn) "START YOUR JOURNEY" else "BẮT ĐẦU HÀNH TRÌNH",
    val startExploringBtn: String = if (isEn) "Start Exploring" else "Bắt đầu khám phá",
    val livingHistoryTitle: String = if (isEn) "History Comes Alive" else "Lịch sử sống động hơn",
    val livingHistorySub: String = if (isEn) "Every artifact tells a story." else "Mỗi hiện vật là một câu chuyện.",

    // Login & Register (M2, M3)
    val welcomeBack: String = if (isEn) "Welcome Back" else "Chào mừng trở lại",
    val loginSubtitle: String = if (isEn) "Sign in to continue your discovery journey" else "Đăng nhập để tiếp tục hành trình khám phá",
    val emailHint: String = if (isEn) "Email address" else "Địa chỉ email",
    val passwordHint: String = if (isEn) "Password" else "Mật khẩu",
    val confirmPasswordHint: String = if (isEn) "Confirm password" else "Nhập lại mật khẩu",
    val fullNameHint: String = if (isEn) "Full name" else "Họ và tên",
    val forgotPassword: String = if (isEn) "Forgot password?" else "Quên mật khẩu?",
    val loginBtn: String = if (isEn) "Sign In" else "Đăng nhập",
    val noAccount: String = if (isEn) "Don't have an account? " else "Chưa có tài khoản? ",
    val registerNow: String = if (isEn) "Sign Up" else "Đăng ký",
    val orDivider: String = if (isEn) "  OR  " else "  HOẶC  ",
    val continueGuestBtn: String = if (isEn) "Continue as Guest" else "Tiếp tục với tư cách Khách (Guest)",
    val guestSub: String = if (isEn) "Explore heritage without an account" else "Khám phá di sản không cần tài khoản",
    val beaconStationShortcut: String = if (isEn) "iBeacon Transmitter Mode (Demo)" else "Chế độ Phát sóng iBeacon (Máy trạm)",
    val createAccountTitle: String = if (isEn) "Create Account" else "Tạo tài khoản",
    val createAccountSub: String = if (isEn) "Save your discovery journey" else "Lưu lại hành trình khám phá của bạn",
    val termsNotice: String = if (isEn) "By signing up, you agree to the Terms" else "Bằng việc đăng ký, bạn đồng ý với Điều khoản",
    val alreadyHaveAccount: String = if (isEn) "Already have an account? " else "Đã có tài khoản? ",

    // HomeScreen (M4, M5)
    val youAreVisiting: String = if (isEn) "YOU ARE VISITING" else "BẠN ĐANG THAM QUAN",
    val siteName: String = if (isEn) "Vietnam National Museum of History" else "Bảo tàng Lịch sử Quốc gia",
    val scanningIbeacon: String = if (isEn) "SCANNING iBEACON" else "ĐANG QUÉT iBEACON",
    val searchingNearby: String = if (isEn) "Searching for nearby artifacts..." else "Đang dò tìm hiện vật ở gần...",
    val moveToExplore: String = if (isEn) "Move around the gallery to discover" else "Di chuyển trong khu trưng bày để khám phá",
    val featuredArtifacts: String = if (isEn) "Featured Artifacts" else "Hiện vật nổi bật",
    val seeAll: String = if (isEn) "See all" else "Xem tất cả",
    val artifactDetectedBadge: String = if (isEn) "ARTIFACT DETECTED" else "PHÁT HIỆN HIỆN VẬT",
    val laterBtn: String = if (isEn) "Later" else "Để sau",
    val viewDetailBtn: String = if (isEn) "View Details" else "Xem chi tiết",

    // Bottom Navigation
    val navHome: String = if (isEn) "Home" else "Trang chủ",
    val navMap: String = if (isEn) "Map" else "Bản đồ",
    val navSaved: String = if (isEn) "Saved" else "Đã lưu",
    val navProfile: String = if (isEn) "Profile" else "Cá nhân",

    // ArtifactDetailScreen & FeedbackDialog (M6, M8)
    val artifactDetailTitle: String = if (isEn) "Artifact Details" else "Chi tiết hiện vật",
    val audioGuideTitle: String = if (isEn) "Artifact Audio Guide" else "Thuyết minh hiện vật",
    val audioGuideVoice: String = if (isEn) "English Narration" else "Giọng đọc Tiếng Việt",
    val cachedNotice: String = if (isEn) {
        "Saved offline – You can walk away\nand keep listening"
    } else {
        "Đã lưu tạm – Bạn có thể vừa đi xa\nvừa nghe tiếp"
    },
    val historicalStoryTitle: String = if (isEn) "Historical Story" else "Câu chuyện lịch sử",
    val visitorReviewsTitle: String = if (isEn) "Visitor Reviews" else "Đánh giá của khách tham quan",
    val writeReviewBtn: String = if (isEn) "Write a review" else "Viết đánh giá",
    val recentCommentsTitle: String = if (isEn) "Recent Comments" else "Bình luận gần đây",
    val feedbackDialogTitle: String = if (isEn) "Share Your Thoughts" else "Chia sẻ cảm nhận của bạn",
    val feedbackDialogSub: String = if (isEn) "How was your visit?" else "Bạn thấy chuyến tham quan thế nào?",
    val feedbackPlaceholder: String = if (isEn) "Share your experience..." else "Chia sẻ cảm nhận của bạn...",
    val submitFeedbackBtn: String = if (isEn) "Submit Review" else "Gửi đánh giá",
    val cancelBtn: String = if (isEn) "Cancel" else "Hủy",

    // SiteMapScreen (M7)
    val siteMapTitle: String = if (isEn) "Site Map" else "Bản đồ di tích",
    val floorPlanSub: String = if (isEn) "Floor plan · Tap a pin to explore" else "Sơ đồ mặt bằng · Chạm pin để khám phá",
    val nearbyLabel: String = if (isEn) "Nearby" else "Đang ở gần",
    val viewDetailArrow: String = if (isEn) "View details →" else "Xem chi tiết →",
    val roomMainHall: String = if (isEn) "MAIN HALL" else "SẢNH CHÍNH",
    val roomZoneA: String = if (isEn) "ZONE A" else "KHU A",
    val roomGallery: String = if (isEn) "GALLERY" else "PHÒNG TRƯNG BÀY",
    val roomZoneB: String = if (isEn) "ZONE B" else "KHU B",
    val roomZoneC: String = if (isEn) "ZONE C" else "KHU C",
    val roomArtifacts: String = if (isEn) "ARTIFACT ZONE" else "KHU HIỆN VẬT",

    // SavedAndHistoryScreen (M9)
    val savedHistoryTitle: String = if (isEn) "Saved & History" else "Đã lưu & Lịch sử",
    val searchPlaceholder: String = if (isEn) "Search saved artifacts or history..." else "Tìm hiện vật đã lưu hoặc lịch sử...",
    val tabFavorites: String = if (isEn) "Favorites" else "Yêu thích",
    val tabHistory: String = if (isEn) "History" else "Lịch sử",
    val savedArtifactsHeading: String = if (isEn) "Your Saved Artifacts" else "Hiện vật bạn đã lưu",
    val emptyFavoritesText: String = if (isEn) {
        "No saved artifacts yet. Tap the heart icon on an artifact detail screen to save it!"
    } else {
        "Bạn chưa lưu hiện vật yêu thích nào. Hãy chạm biểu tượng trái tim ở màn hình Chi tiết hiện vật để lưu lại!"
    },
    val recentHistoryHeading: String = if (isEn) "Recent Visit History" else "Lịch sử tham quan gần đây",
    val passedByPrefix: String = if (isEn) "Visited: " else "Đã đi ngang qua: ",
    val durationPrefix: String = if (isEn) "View duration: " else "Thời gian xem: ",
    val secondsUnit: String = if (isEn) "sec" else "giây",

    // ProfileScreen (M10)
    val profileTitle: String = if (isEn) "Personal Account" else "Tài khoản cá nhân",
    val settingsHeader: String = if (isEn) "SETTINGS" else "CÀI ĐẶT",
    val audioLanguageTitle: String = if (isEn) "Narration & App Language" else "Ngôn ngữ thuyết minh",
    val languageDisplayName: String = if (isEn) "English" else "Tiếng Việt",
    val autoBeaconTitle: String = if (isEn) "Auto iBeacon Alerts" else "Thông báo tự động iBeacon",
    val autoBeaconOn: String = if (isEn) "Notifications enabled" else "Bật thông báo",
    val autoBeaconOff: String = if (isEn) "Notifications disabled" else "Tắt thông báo",
    val helpSupportTitle: String = if (isEn) "Help & Support" else "Trợ giúp & hỗ trợ",
    val simulatorModeTitle: String = if (isEn) "iBeacon Transmitter (Multi-Phone)" else "Phát sóng iBeacon (Test nhiều máy)",
    val simulatorModeSub: String = if (isEn) "Turn this phone into a real iBeacon station" else "Biến điện thoại này thành cục phát iBeacon thật",
    val logoutBtn: String = if (isEn) "Sign Out" else "Đăng xuất",

    // Máy chủ & tài khoản (GĐ4)
    val serverLabel: String = if (isEn) "Server" else "Máy chủ",
    val serverAuto: String = if (isEn) "automatic" else "tự động",
    val serverDialogTitle: String = if (isEn) "Server address" else "Địa chỉ máy chủ",
    val serverDialogHint: String = if (isEn) {
        "IP of the computer running the backend, e.g. 192.168.1.5:8000. Leave empty for automatic (USB adb reverse or emulator)."
    } else {
        "IP máy tính chạy backend, ví dụ 192.168.1.5:8000. Để trống = tự động (cắm USB adb reverse hoặc máy ảo)."
    },
    val serverTestBtn: String = if (isEn) "Test connection" else "Kiểm tra kết nối",
    val serverSaveBtn: String = if (isEn) "Save" else "Lưu",
    val serverOk: String = if (isEn) "Connected to the server." else "Kết nối máy chủ thành công.",
    val serverFail: String = if (isEn) {
        "Cannot connect. Check the address, Wi-Fi and firewall (port 8000)."
    } else {
        "Không kết nối được. Kiểm tra địa chỉ, Wi-Fi và tường lửa (cổng 8000)."
    },
    val serverInvalid: String = if (isEn) "Invalid address." else "Địa chỉ không hợp lệ.",
    val networkError: String = if (isEn) {
        "Cannot reach the server. Check the server address below."
    } else {
        "Không kết nối được máy chủ. Kiểm tra địa chỉ máy chủ bên dưới."
    },
    val guestName: String = if (isEn) "Guest" else "Khách tham quan",
    val guestHint: String = if (isEn) "Not signed in" else "Chưa đăng nhập",
)

fun getAppStrings(lang: String): AppStrings {
    return AppStrings(isEn = lang.equals("en", ignoreCase = true))
}
