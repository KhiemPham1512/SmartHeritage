

package com.example.smartheritage_mobile.model

import com.google.gson.annotations.SerializedName

data class Artifact(

    @SerializedName("artifact_id")
    val artifactId: Int,

    @SerializedName("artifact_code")
    val artifactCode: String,

    @SerializedName("zone_name")
    val zoneName: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("summary")
    val summary: String,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("status")
    val status: String
)