package com.example.smartheritage_mobile.model

import com.example.smartheritage_mobile.data.local.Session
import com.google.gson.annotations.SerializedName

data class VisitLogCreateRequest(
    @SerializedName("user_id")
    val user_id: Int? = null,

    @SerializedName("device_id")
    val device_id: String = Session.deviceId,

    @SerializedName("artifact_id")
    val artifact_id: Int,

    @SerializedName("beacon_id")
    val beacon_id: Int? = null,

    @SerializedName("lang_used")
    val lang_used: String = "vi",

    @SerializedName("duration_seconds")
    val duration_seconds: Int = 30,

    @SerializedName("is_favorite")
    val is_favorite: Boolean = false
)

data class FavoriteToggleRequestDto(
    @SerializedName("user_id")
    val user_id: Int? = null,

    @SerializedName("device_id")
    val device_id: String = Session.deviceId,

    @SerializedName("artifact_id")
    val artifact_id: Int
)

data class FavoriteToggleResponseDto(
    @SerializedName("artifact_id")
    val artifact_id: Int,

    @SerializedName("is_favorite")
    val is_favorite: Boolean
)

data class VisitLogItemDto(
    @SerializedName("log_id")
    val log_id: Int? = null,

    @SerializedName("artifact_id")
    val artifact_id: Int,

    @SerializedName("artifact_code")
    val artifact_code: String? = null,

    @SerializedName("zone_name")
    val zone_name: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("summary")
    val summary: String? = null,

    @SerializedName("image_url")
    val image_url: String? = null,

    @SerializedName("duration_seconds")
    val duration_seconds: Int? = 60,

    @SerializedName("is_favorite")
    val is_favorite: Boolean? = false,

    @SerializedName("visited_at")
    val visited_at: String? = null
)

data class MyVisitLogsResponse(
    @SerializedName("favorites")
    val favorites: List<VisitLogItemDto> = emptyList(),

    @SerializedName("history")
    val history: List<VisitLogItemDto> = emptyList()
)

data class FeedbackCreateRequestDto(
    @SerializedName("user_id")
    val user_id: Int? = null,

    @SerializedName("visitor_name")
    val visitor_name: String = Session.fullName ?: "Khách tham quan",

    @SerializedName("artifact_id")
    val artifact_id: Int,

    @SerializedName("rating")
    val rating: Int,

    @SerializedName("comment")
    val comment: String? = null
)
