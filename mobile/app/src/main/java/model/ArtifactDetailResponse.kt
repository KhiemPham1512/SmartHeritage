package com.example.smartheritage_mobile.model

import com.google.gson.annotations.SerializedName

data class ArtifactDetailResponse(
    @SerializedName("artifact_id")
    val artifact_id: Int,

    @SerializedName("site_id")
    val site_id: Int? = 1,

    @SerializedName("artifact_code")
    val artifact_code: String? = null,

    @SerializedName("zone_name")
    val zone_name: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("summary")
    val summary: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("name_vi")
    val name_vi: String? = null,

    @SerializedName("name_en")
    val name_en: String? = null,

    @SerializedName("summary_vi")
    val summary_vi: String? = null,

    @SerializedName("summary_en")
    val summary_en: String? = null,

    @SerializedName("description_vi")
    val description_vi: String? = null,

    @SerializedName("description_en")
    val description_en: String? = null,

    @SerializedName("image_url")
    val image_url: String? = null,

    @SerializedName("audio_url")
    val audio_url: String? = null,

    @SerializedName("audio_vi_url")
    val audio_vi_url: String? = null,

    @SerializedName("audio_en_url")
    val audio_en_url: String? = null,

    @SerializedName("audio_duration_sec")
    val audio_duration_sec: Int? = 225,

    @SerializedName("translated_langs")
    val translated_langs: String? = "VI,EN",

    @SerializedName("map_x")
    val map_x: Float? = null,

    @SerializedName("map_y")
    val map_y: Float? = null,

    @SerializedName("status")
    val status: String? = "PUBLISHED",

    @SerializedName("beacon_id")
    val beacon_id: Int? = null,

    @SerializedName("beacon_code")
    val beacon_code: String? = null,

    @SerializedName("major")
    val major: Int? = null,

    @SerializedName("minor")
    val minor: Int? = null,

    @SerializedName("trigger_distance")
    val trigger_distance: Double? = null,

    @SerializedName("feedbacks")
    val feedbacks: List<ArtifactFeedbackDto>? = emptyList()
)

data class ArtifactFeedbackDto(
    @SerializedName("feedback_id")
    val feedback_id: Int? = null,

    @SerializedName("visitor_name")
    val visitor_name: String? = null,

    @SerializedName("rating")
    val rating: Int? = 5,

    @SerializedName("comment")
    val comment: String? = null,

    @SerializedName("created_at")
    val created_at: String? = null
)
