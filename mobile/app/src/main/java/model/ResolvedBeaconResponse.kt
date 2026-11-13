package com.example.smartheritage_mobile.model

import com.google.gson.annotations.SerializedName

data class ResolvedBeaconResponse(
    @SerializedName("beacon_id")
    val beacon_id: Int,

    @SerializedName("beacon_code")
    val beacon_code: String?,

    @SerializedName("uuid")
    val uuid: String,

    @SerializedName("major")
    val major: Int,

    @SerializedName("minor")
    val minor: Int,

    @SerializedName("trigger_distance")
    val trigger_distance: Double?,

    @SerializedName("artifact_id")
    val artifact_id: Int,

    @SerializedName("artifact_code")
    val artifact_code: String?,

    @SerializedName("zone_name")
    val zone_name: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("summary")
    val summary: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("image_url")
    val image_url: String?,

    @SerializedName("audio_url")
    val audio_url: String?,

    @SerializedName("audio_duration_sec")
    val audio_duration_sec: Int?,

    @SerializedName("map_x")
    val map_x: Float?,

    @SerializedName("map_y")
    val map_y: Float?
)
