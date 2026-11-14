package com.example.smartheritage_mobile.model

import com.google.gson.annotations.SerializedName

data class SiteMapResponse(
    @SerializedName("site")
    val site: SiteMapInfo,

    @SerializedName("pins")
    val pins: List<MapPinDto>
)

data class SiteMapInfo(
    @SerializedName("site_id")
    val site_id: Int,

    @SerializedName("site_name")
    val site_name: String?,

    @SerializedName("name_vi")
    val name_vi: String?,

    @SerializedName("name_en")
    val name_en: String?,

    @SerializedName("floor_name")
    val floor_name: String?,

    @SerializedName("address")
    val address: String?,

    @SerializedName("map_image_url")
    val map_image_url: String?
)

data class MapPinDto(
    @SerializedName("beacon_id")
    val beacon_id: Int,

    @SerializedName("beacon_code")
    val beacon_code: String?,

    @SerializedName("major")
    val major: Int,

    @SerializedName("minor")
    val minor: Int,

    @SerializedName("trigger_distance")
    val trigger_distance: Double?,

    @SerializedName("battery_level")
    val battery_level: Int?,

    @SerializedName("beacon_status")
    val beacon_status: String?,

    @SerializedName("map_x")
    val map_x: Float?,

    @SerializedName("map_y")
    val map_y: Float?,

    @SerializedName("zone_name")
    val zone_name: String?,

    @SerializedName("artifact_id")
    val artifact_id: Int?,

    @SerializedName("artifact_code")
    val artifact_code: String?,

    @SerializedName("artifact_name")
    val artifact_name: String?,

    @SerializedName("summary")
    val summary: String?,

    @SerializedName("image_url")
    val image_url: String?,

    @SerializedName("audio_url")
    val audio_url: String?
)
