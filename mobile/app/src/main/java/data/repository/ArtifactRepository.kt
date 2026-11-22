package com.example.smartheritage_mobile.data.repository

import com.example.smartheritage_mobile.data.remote.RetrofitClient
import com.example.smartheritage_mobile.model.Artifact
import com.example.smartheritage_mobile.model.ArtifactDetailResponse
import com.example.smartheritage_mobile.model.ArtifactFeedbackDto
import com.example.smartheritage_mobile.model.FavoriteToggleRequestDto
import com.example.smartheritage_mobile.model.FavoriteToggleResponseDto
import com.example.smartheritage_mobile.model.FeedbackCreateRequestDto
import com.example.smartheritage_mobile.model.MyVisitLogsResponse
import com.example.smartheritage_mobile.model.ResolvedBeaconResponse
import com.example.smartheritage_mobile.model.SiteMapResponse
import com.example.smartheritage_mobile.model.VisitLogCreateRequest
import retrofit2.Response

class ArtifactRepository {

    suspend fun getArtifacts(lang: String): List<Artifact> {
        return RetrofitClient.artifactApi.getArtifacts(
            lang = lang
        )
    }

    suspend fun getArtifactDetail(artifactId: Int, lang: String): Response<ArtifactDetailResponse> {
        return RetrofitClient.artifactApi.getArtifactDetail(
            artifactId = artifactId,
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

    suspend fun recordVisitLog(request: VisitLogCreateRequest): Response<Map<String, Any>> {
        return RetrofitClient.artifactApi.recordVisitLog(request)
    }

    suspend fun toggleFavorite(request: FavoriteToggleRequestDto): Response<FavoriteToggleResponseDto> {
        return RetrofitClient.artifactApi.toggleFavorite(request)
    }

    suspend fun getMyFavoritesAndHistory(
        userId: Int? = null,
        deviceId: String = "android-guest-01",
        lang: String = "vi"
    ): Response<MyVisitLogsResponse> {
        return RetrofitClient.artifactApi.getMyFavoritesAndHistory(
            userId = userId,
            deviceId = deviceId,
            lang = lang
        )
    }

    suspend fun submitFeedback(request: FeedbackCreateRequestDto): Response<ArtifactFeedbackDto> {
        return RetrofitClient.artifactApi.submitFeedback(request)
    }
}