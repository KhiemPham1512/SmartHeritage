package com.example.smartheritage_mobile.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.smartheritage_mobile.model.ArtifactDetailResponse
import com.example.smartheritage_mobile.model.ArtifactFeedbackDto
import com.example.smartheritage_mobile.model.ResolvedBeaconResponse

data class CachedArtifact(
    val artifactId: Int,
    val artifactCode: String,
    val zoneName: String,
    val nameVi: String,
    val nameEn: String,
    val summaryVi: String,
    val summaryEn: String,
    val descriptionVi: String,
    val descriptionEn: String,
    val imageUrl: String,
    val audioViUrl: String,
    val audioEnUrl: String,
    val audioDurationSec: Int,
    val mapX: Float,
    val mapY: Float,
    val isFavorite: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDetailResponse(lang: String, feedbacks: List<ArtifactFeedbackDto>): ArtifactDetailResponse {
        val isEn = lang.equals("en", ignoreCase = true)
        return ArtifactDetailResponse(
            artifact_id = artifactId,
            artifact_code = artifactCode,
            zone_name = zoneName,
            name = if (isEn && nameEn.isNotBlank()) nameEn else nameVi,
            summary = if (isEn && summaryEn.isNotBlank()) summaryEn else summaryVi,
            description = if (isEn && descriptionEn.isNotBlank()) descriptionEn else descriptionVi,
            name_vi = nameVi,
            name_en = nameEn,
            summary_vi = summaryVi,
            summary_en = summaryEn,
            description_vi = descriptionVi,
            description_en = descriptionEn,
            image_url = imageUrl,
            audio_url = if (isEn && audioEnUrl.isNotBlank()) audioEnUrl else audioViUrl,
            audio_vi_url = audioViUrl,
            audio_en_url = audioEnUrl,
            audio_duration_sec = audioDurationSec,
            map_x = mapX,
            map_y = mapY,
            feedbacks = feedbacks
        )
    }
}

/**
 * Bộ nhớ đệm CSDL cục bộ (bảng CachedArtifact) phục vụ cột mốc M5:
 * Lưu tạm hiện vật vừa quét iBeacon hoặc vừa mở để khi khách đi xa mất sóng BLE
 * hoặc mất mạng vẫn tiếp tục nghe thuyết minh và đọc nội dung trọn vẹn.
 */
class LocalArtifactCache private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CACHED_ARTIFACT (
                artifact_id INTEGER PRIMARY KEY,
                artifact_code TEXT NOT NULL,
                zone_name TEXT NOT NULL,
                name_vi TEXT NOT NULL,
                name_en TEXT NOT NULL,
                summary_vi TEXT NOT NULL,
                summary_en TEXT NOT NULL,
                description_vi TEXT NOT NULL,
                description_en TEXT NOT NULL,
                image_url TEXT NOT NULL,
                audio_vi_url TEXT NOT NULL,
                audio_en_url TEXT NOT NULL,
                audio_duration_sec INTEGER NOT NULL,
                map_x REAL NOT NULL,
                map_y REAL NOT NULL,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                cached_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        seedDefaultArtifacts(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_ARTIFACT")
        onCreate(db)
    }

    private fun seedDefaultArtifacts(db: SQLiteDatabase) {
        defaultSeeds.forEach { item ->
            insertOrUpdateInternal(db, item)
        }
    }

    private fun insertOrUpdateInternal(db: SQLiteDatabase, item: CachedArtifact) {
        val values = ContentValues().apply {
            put("artifact_id", item.artifactId)
            put("artifact_code", item.artifactCode)
            put("zone_name", item.zoneName)
            put("name_vi", item.nameVi)
            put("name_en", item.nameEn)
            put("summary_vi", item.summaryVi)
            put("summary_en", item.summaryEn)
            put("description_vi", item.descriptionVi)
            put("description_en", item.descriptionEn)
            put("image_url", item.imageUrl)
            put("audio_vi_url", item.audioViUrl)
            put("audio_en_url", item.audioEnUrl)
            put("audio_duration_sec", item.audioDurationSec)
            put("map_x", item.mapX)
            put("map_y", item.mapY)
            put("is_favorite", if (item.isFavorite) 1 else 0)
            put("cached_at", item.cachedAt)
        }
        db.insertWithOnConflict(
            TABLE_CACHED_ARTIFACT,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun saveFromResolvedBeacon(resolved: ResolvedBeaconResponse) {
        val existing = getCachedArtifactById(resolved.artifact_id)
        val fallback = defaultSeeds.find { it.artifactId == resolved.artifact_id }
        val merged = CachedArtifact(
            artifactId = resolved.artifact_id,
            artifactCode = resolved.artifact_code ?: existing?.artifactCode ?: fallback?.artifactCode ?: "HV-00${resolved.artifact_id}",
            zoneName = resolved.zone_name ?: existing?.zoneName ?: fallback?.zoneName ?: "Khu trưng bày chính",
            nameVi = resolved.name ?: existing?.nameVi ?: fallback?.nameVi ?: "Hiện vật lịch sử",
            nameEn = existing?.nameEn ?: fallback?.nameEn ?: resolved.name ?: "Historical Artifact",
            summaryVi = resolved.summary ?: existing?.summaryVi ?: fallback?.summaryVi ?: "",
            summaryEn = existing?.summaryEn ?: fallback?.summaryEn ?: resolved.summary ?: "",
            descriptionVi = resolved.description ?: existing?.descriptionVi ?: fallback?.descriptionVi ?: "",
            descriptionEn = existing?.descriptionEn ?: fallback?.descriptionEn ?: resolved.description ?: "",
            imageUrl = resolved.image_url ?: existing?.imageUrl ?: fallback?.imageUrl ?: "/static/images/hv_001.jpg",
            audioViUrl = resolved.audio_url ?: existing?.audioViUrl ?: fallback?.audioViUrl ?: "/static/audio/ngoc-lu-thuyet-minh-vi.mp3",
            audioEnUrl = existing?.audioEnUrl ?: fallback?.audioEnUrl ?: resolved.audio_url ?: "/static/audio/ngoc-lu-thuyet-minh-en.mp3",
            audioDurationSec = resolved.audio_duration_sec ?: existing?.audioDurationSec ?: fallback?.audioDurationSec ?: 225,
            mapX = resolved.map_x ?: existing?.mapX ?: fallback?.mapX ?: 340f,
            mapY = resolved.map_y ?: existing?.mapY ?: fallback?.mapY ?: 215f,
            isFavorite = existing?.isFavorite ?: false,
            cachedAt = System.currentTimeMillis()
        )
        insertOrUpdateInternal(writableDatabase, merged)
        setLastDetectedArtifactId(resolved.artifact_id)
    }

    fun saveFromDetail(detail: ArtifactDetailResponse) {
        val existing = getCachedArtifactById(detail.artifact_id)
        val fallback = defaultSeeds.find { it.artifactId == detail.artifact_id }
        val merged = CachedArtifact(
            artifactId = detail.artifact_id,
            artifactCode = detail.artifact_code ?: existing?.artifactCode ?: fallback?.artifactCode ?: "HV-00${detail.artifact_id}",
            zoneName = detail.zone_name ?: existing?.zoneName ?: fallback?.zoneName ?: "Khu trưng bày chính",
            nameVi = detail.name_vi ?: detail.name ?: existing?.nameVi ?: fallback?.nameVi ?: "Hiện vật lịch sử",
            nameEn = detail.name_en ?: detail.name ?: existing?.nameEn ?: fallback?.nameEn ?: "Historical Artifact",
            summaryVi = detail.summary_vi ?: detail.summary ?: existing?.summaryVi ?: fallback?.summaryVi ?: "",
            summaryEn = detail.summary_en ?: detail.summary ?: existing?.summaryEn ?: fallback?.summaryEn ?: "",
            descriptionVi = detail.description_vi ?: detail.description ?: existing?.descriptionVi ?: fallback?.descriptionVi ?: "",
            descriptionEn = detail.description_en ?: detail.description ?: existing?.descriptionEn ?: fallback?.descriptionEn ?: "",
            imageUrl = detail.image_url ?: existing?.imageUrl ?: fallback?.imageUrl ?: "/static/images/hv_001.jpg",
            audioViUrl = detail.audio_vi_url ?: detail.audio_url ?: existing?.audioViUrl ?: fallback?.audioViUrl ?: "/static/audio/ngoc-lu-thuyet-minh-vi.mp3",
            audioEnUrl = detail.audio_en_url ?: detail.audio_url ?: existing?.audioEnUrl ?: fallback?.audioEnUrl ?: "/static/audio/ngoc-lu-thuyet-minh-en.mp3",
            audioDurationSec = detail.audio_duration_sec ?: existing?.audioDurationSec ?: fallback?.audioDurationSec ?: 225,
            mapX = detail.map_x ?: existing?.mapX ?: fallback?.mapX ?: 340f,
            mapY = detail.map_y ?: existing?.mapY ?: fallback?.mapY ?: 215f,
            isFavorite = existing?.isFavorite ?: false,
            cachedAt = System.currentTimeMillis()
        )
        insertOrUpdateInternal(writableDatabase, merged)
    }

    fun getCachedArtifactById(artifactId: Int): CachedArtifact? {
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_CACHED_ARTIFACT WHERE artifact_id = ?",
            arrayOf(artifactId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return CachedArtifact(
                    artifactId = cursor.getInt(cursor.getColumnIndexOrThrow("artifact_id")),
                    artifactCode = cursor.getString(cursor.getColumnIndexOrThrow("artifact_code")),
                    zoneName = cursor.getString(cursor.getColumnIndexOrThrow("zone_name")),
                    nameVi = cursor.getString(cursor.getColumnIndexOrThrow("name_vi")),
                    nameEn = cursor.getString(cursor.getColumnIndexOrThrow("name_en")),
                    summaryVi = cursor.getString(cursor.getColumnIndexOrThrow("summary_vi")),
                    summaryEn = cursor.getString(cursor.getColumnIndexOrThrow("summary_en")),
                    descriptionVi = cursor.getString(cursor.getColumnIndexOrThrow("description_vi")),
                    descriptionEn = cursor.getString(cursor.getColumnIndexOrThrow("description_en")),
                    imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("image_url")),
                    audioViUrl = cursor.getString(cursor.getColumnIndexOrThrow("audio_vi_url")),
                    audioEnUrl = cursor.getString(cursor.getColumnIndexOrThrow("audio_en_url")),
                    audioDurationSec = cursor.getInt(cursor.getColumnIndexOrThrow("audio_duration_sec")),
                    mapX = cursor.getFloat(cursor.getColumnIndexOrThrow("map_x")),
                    mapY = cursor.getFloat(cursor.getColumnIndexOrThrow("map_y")),
                    isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")) == 1,
                    cachedAt = cursor.getLong(cursor.getColumnIndexOrThrow("cached_at"))
                )
            }
        }
        return defaultSeeds.find { it.artifactId == artifactId } ?: defaultSeeds.first()
    }

    fun getFallbackDetailResponse(artifactId: Int, lang: String): ArtifactDetailResponse {
        val cached = getCachedArtifactById(artifactId) ?: defaultSeeds.first()
        val defaultFeedbacks = listOf(
            ArtifactFeedbackDto(
                feedback_id = 1,
                visitor_name = "Nguyễn Minh",
                rating = 5,
                comment = "Nội dung thuyết minh rất dễ hiểu.",
                created_at = "2026-11-16"
            ),
            ArtifactFeedbackDto(
                feedback_id = 2,
                visitor_name = "Trần Bảo Ngọc",
                rating = 5,
                comment = "Thuyết minh tự động rất nhạy, đi tới gần là máy tự hiện thông tin hiện vật rất tiện lợi.",
                created_at = "2026-11-16"
            )
        )
        return cached.toDetailResponse(lang, defaultFeedbacks)
    }

    fun isFavorite(artifactId: Int): Boolean {
        return getCachedArtifactById(artifactId)?.isFavorite ?: false
    }

    fun toggleFavoriteLocal(artifactId: Int): Boolean {
        val current = getCachedArtifactById(artifactId) ?: return false
        val newFav = !current.isFavorite
        insertOrUpdateInternal(writableDatabase, current.copy(isFavorite = newFav))
        return newFav
    }

    fun setLastDetectedArtifactId(artifactId: Int) {
        prefs.edit().putInt(KEY_LAST_DETECTED_ARTIFACT_ID, artifactId).apply()
    }

    fun getLastDetectedArtifactId(): Int {
        return prefs.getInt(KEY_LAST_DETECTED_ARTIFACT_ID, 1)
    }

    fun getPreferredLanguage(): String {
        return prefs.getString(KEY_PREFERRED_LANG, "vi") ?: "vi"
    }

    fun setPreferredLanguage(lang: String) {
        prefs.edit().putString(KEY_PREFERRED_LANG, lang.lowercase()).apply()
    }

    companion object {
        private const val DATABASE_NAME = "smart_heritage_cache.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_CACHED_ARTIFACT = "CachedArtifact"
        private const val PREFS_NAME = "smart_heritage_prefs"
        private const val KEY_LAST_DETECTED_ARTIFACT_ID = "last_detected_artifact_id"
        private const val KEY_PREFERRED_LANG = "preferred_lang"

        @Volatile
        private var INSTANCE: LocalArtifactCache? = null

        fun getInstance(context: Context): LocalArtifactCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalArtifactCache(context).also { INSTANCE = it }
            }
        }

        val defaultSeeds = listOf(
            CachedArtifact(
                artifactId = 1,
                artifactCode = "HV-001",
                zoneName = "Sảnh chính · Khu Đông Sơn",
                nameVi = "Trống đồng Ngọc Lũ",
                nameEn = "Ngoc Lu Bronze Drum",
                summaryVi = "Dấu ấn văn hóa Đông Sơn rực rỡ cách đây hơn 2.500 năm.",
                summaryEn = "The pinnacle of ancient Dong Son bronze casting culture.",
                descriptionVi = "Hiện vật phản ánh đời sống văn hóa, kỹ thuật chế tác và nghệ thuật trang trí của cư dân Việt cổ. Trống đồng Ngọc Lũ là một trong những chiếc trống đồng cổ nhất và đẹp nhất thuộc nền văn hóa Đông Sơn (niên đại khoảng 2.500 năm trước). Mặt trống đúc nổi ngôi sao 14 cánh cùng các họa tiết chim Lạc và cảnh sinh hoạt của cư dân Việt cổ.",
                descriptionEn = "The Ngoc Lu Bronze Drum is the most iconic artifact of the Dong Son culture (dating back 2,500 years). Its tympanum features a 14-pointed star surrounded by Lac birds and scenes of ancient communal life.",
                imageUrl = "/static/images/hv_001.jpg",
                audioViUrl = "/static/audio/ngoc-lu-thuyet-minh-vi.mp3",
                audioEnUrl = "/static/audio/ngoc-lu-thuyet-minh-en.mp3",
                audioDurationSec = 250,
                mapX = 340f,
                mapY = 215f
            ),
            CachedArtifact(
                artifactId = 2,
                artifactCode = "HV-002",
                zoneName = "Khu A · Phòng Gốm Cổ",
                nameVi = "Bình gốm Chu Đậu",
                nameEn = "Chu Dau Ceramic Vase",
                summaryVi = "Tuyệt tác gốm hoa lam Việt Nam thế kỷ XV với nét vẽ thủ công tinh xảo.",
                summaryEn = "A 15th-century Vietnamese blue-and-white ceramic masterpiece.",
                descriptionVi = "Bình gốm hoa lam Chu Đậu (Hải Dương) có niên đại thế kỷ XV, nổi tiếng với lớp men trắng ngà trong trẻo và họa tiết hoa cúc, chim phượng vẽ bằng màu xanh coban tinh xảo, phản ánh kỹ thuật chế tác gốm sứ đỉnh cao của người Việt xưa.",
                descriptionEn = "Dating from the 15th century, Chu Dau blue-and-white ceramics are celebrated for their ivory glaze and intricate cobalt-blue floral motifs.",
                imageUrl = "/static/images/hv_002.jpg",
                audioViUrl = "/static/audio/chu-dau-vi.mp3",
                audioEnUrl = "/static/audio/chu-dau-en.mp3",
                audioDurationSec = 180,
                mapX = 520f,
                mapY = 180f
            ),
            CachedArtifact(
                artifactId = 3,
                artifactCode = "HV-003",
                zoneName = "Phòng trưng bày · Khu Chăm Pa",
                nameVi = "Tượng Apsara",
                nameEn = "Champa Apsara Sandstone Relief",
                summaryVi = "Phù điêu vũ nữ Apsara bằng đá sa thạch tiêu biểu cho nghệ thuật điêu khắc Chăm Pa.",
                summaryEn = "Sandstone relief of an Apsara celestial dancer from the Champa Kingdom.",
                descriptionVi = "Tượng vũ nữ Apsara thuộc phong cách nghệ thuật Trà Kiệu (thế kỷ X), thể hiện đường nét uyển chuyển và nghệ thuật chạm khắc đá sa thạch đỉnh cao của vương quốc Chăm Pa cổ đại.",
                descriptionEn = "Carved in the 10th-century Tra Kieu style, this sandstone Apsara dancer embodies the grace and sculptural mastery of the Champa civilization.",
                imageUrl = "/static/images/hv_003.jpg",
                audioViUrl = "/static/audio/apsara-vi.mp3",
                audioEnUrl = "/static/audio/apsara-en.mp3",
                audioDurationSec = 195,
                mapX = 280f,
                mapY = 420f
            ),
            CachedArtifact(
                artifactId = 4,
                artifactCode = "HV-004",
                zoneName = "Khu B · Trưng bày Đông Sơn",
                nameVi = "Thạp đồng Đào Thịnh",
                nameEn = "Dao Thinh Bronze Jar",
                summaryVi = "Bảo vật quốc gia phản ánh tín ngưỡng phồn thực và kỹ thuật đúc đồng Đông Sơn.",
                summaryEn = "National treasure reflecting fertility beliefs and Dong Son bronze mastery.",
                descriptionVi = "Thạp đồng Đào Thịnh có kích thước đồ sộ và nắp đúc tượng các cặp đôi nam nữ, phản ánh đời sống tâm linh và ước vọng sinh sôi nảy nở của cư dân nông nghiệp lúa nước thời kỳ Đông Sơn.",
                descriptionEn = "The monumental Dao Thinh bronze jar features figurines on its lid, symbolizing fertility rituals of ancient wet-rice agricultural communities.",
                imageUrl = "/static/images/hv_004.jpg",
                audioViUrl = "/static/audio/dao-thinh-vi.mp3",
                audioEnUrl = "/static/audio/dao-thinh-en.mp3",
                audioDurationSec = 210,
                mapX = 410f,
                mapY = 310f
            ),
            CachedArtifact(
                artifactId = 5,
                artifactCode = "HV-005",
                zoneName = "Khu hiện vật · Phòng Triều Nguyễn",
                nameVi = "Ấn vàng Triều Nguyễn",
                nameEn = "Nguyen Dynasty Imperial Golden Seal",
                summaryVi = "Bảo vật hoàng cung biểu trưng cho quyền lực tối cao của các vị vua nhà Nguyễn.",
                summaryEn = "Imperial golden seal symbolizing the supreme authority of the Nguyen Emperors.",
                descriptionVi = "Chiếc ấn vàng đúc dưới triều vua Minh Mạng năm 1823 với núm ấn hình rồng uốn lượn, được sử dụng trong các đại lễ và văn kiện trọng đại của triều đình Huế.",
                descriptionEn = "Cast in 1823 during Emperor Minh Mang reign, this dragon-knobbed golden seal was used for the most solemn imperial decrees of the Hue Court.",
                imageUrl = "/static/images/hv_005.jpg",
                audioViUrl = "/static/audio/an-vang-vi.mp3",
                audioEnUrl = "/static/audio/an-vang-en.mp3",
                audioDurationSec = 240,
                mapX = 610f,
                mapY = 450f
            ),
            CachedArtifact(
                artifactId = 6,
                artifactCode = "HV-006",
                zoneName = "Khu C · Sân Thế Miếu",
                nameVi = "Cửu đỉnh Huế",
                nameEn = "Nine Dynastic Urns of Hue",
                summaryVi = "Bộ 9 đỉnh đồng đồ sộ đúc năm 1835 biểu tượng cho quyền lực và non sông Việt Nam.",
                summaryEn = "Nine monumental bronze urns cast in 1835 symbolizing the unified territory of Vietnam.",
                descriptionVi = "Cửu đỉnh của nhà Nguyễn là chín chiếc đỉnh bằng đồng đặt trước sân Thế Miếu trong Hoàng thành Huế, được vua Minh Mạng ra lệnh đúc vào năm 1835 và hoàn thành năm 1837. Trên mỗi đỉnh chạm khắc 17 bức họa nổi về núi sông, biển đảo và sản vật tiêu biểu của đất nước.",
                descriptionEn = "The Nine Dynastic Urns of the Nguyen Dynasty were cast between 1835 and 1837 under Emperor Minh Mang, featuring 153 relief carvings of Vietnamese mountains, seas, and natural treasures.",
                imageUrl = "/static/images/hv_006.jpg",
                audioViUrl = "/static/audio/an-vang-vi.mp3",
                audioEnUrl = "/static/audio/an-vang-en.mp3",
                audioDurationSec = 215,
                mapX = 240f,
                mapY = 520f
            )
        )
    }
}
