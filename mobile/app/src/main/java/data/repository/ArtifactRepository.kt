package com.example.smartheritage_mobile.data.repository

import com.example.smartheritage_mobile.data.remote.RetrofitClient
import com.example.smartheritage_mobile.model.Artifact
import com.example.smartheritage_mobile.model.ResolvedBeaconResponse
import com.example.smartheritage_mobile.model.SiteMapResponse
import retrofit2.Response

class ArtifactRepository {

    suspend fun getArtifacts(lang: String): List<Artifact> {
        return RetrofitClient.artifactApi.getArtifacts(
            lang = lang
        )
    }

    suspend fun resolveBeacon(major: Int, minor: Int, lang: String): Response<ResolvedBeaconResponse> {
        return RetrofitClient.artifactApi.resolveBeacon(
            major = major,
            minor = minor,
            lang = lang
        )
    }

    suspend fun getSiteMap(siteId: Int = 1, lang: String): Response<SiteMapResponse> {
        return RetrofitClient.artifactApi.getSiteMap(
            siteId = siteId,
            lang = lang
        )
    }
}