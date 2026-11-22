package com.example.smartheritage_mobile.config

/** Chuyển ngôn ngữ trong danh sách Admin đang bật ở W8 (thuần Kotlin để kiểm thử bằng JUnit). */
object LanguageCycle {
    val DEFAULT = listOf("vi", "en")

    /** Ngôn ngữ kế tiếp trong vòng; hiện tại không nằm trong danh sách (vd. vừa bị tắt) → quay về ngôn ngữ đầu tiên. */
    fun next(current: String, enabled: List<String>): String {
        val list = enabled.ifEmpty { DEFAULT }
        val index = list.indexOf(current.lowercase())
        return list[(index + 1) % list.size]
    }

    /** Giao diện app chỉ có tiếng Việt và tiếng Anh: ngôn ngữ khác dùng chữ giao diện tiếng Anh (nội dung hiện vật vẫn theo ngôn ngữ đã chọn). */
    fun isEnglishUi(lang: String): Boolean = !lang.equals("vi", ignoreCase = true)
}
