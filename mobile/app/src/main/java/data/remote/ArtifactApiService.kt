
package com.example.smartheritage_mobile.data.remote

import com.example.smartheritage_mobile.data.local.Session
import com.example.smartheritage_mobile.model.Artifact
import com.example.smartheritage_mobile.model.ArtifactDetailResponse
import com.example.smartheritage_mobile.model.ArtifactFeedbackDto
import com.example.smartheritage_mobile.model.AuthResponseDto
import com.example.smartheritage_mobile.model.FavoriteToggleRequestDto
import com.example.smartheritage_mobile.model.FavoriteToggleResponseDto
import com.example.smartheritage_mobile.model.FeedbackCreateRequestDto
import com.example.smartheritage_mobile.model.LoginRequestDto
import com.example.smartheritage_mobile.model.MyVisitLogsResponse
import com.example.smartheritage_mobile.model.ProfileUpdateDto
import com.example.smartheritage_mobile.model.RegisterRequestDto
import com.example.smartheritage_mobile.model.ResolvedBeaconResponse
import com.example.smartheritage_mobile.model.SiteMapResponse
import com.example.smartheritage_mobile.model.VisitLogCreateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ArtifactApiService {

    @GET("api/artifacts")
    suspend fun getArtifacts(
        @Query("lang") lang: String,
        @Query("status") status: String = "PUBLISHED"
    ): List<Artifact>

    @GET("api/artifacts/{artifact_id}")
    suspend fun getArtifactDetail(
        @Path("artifact_id") artifactId: Int,
        @Query("lang") lang: String
    ): Response<ArtifactDetailResponse>

    @GET("api/beacons/resolve")
    suspend fun resolveBeacon(
        @Query("major") major: Int,
        @Query("minor") minor: Int,
        @Query("lang") lang: String
    ): Response<ResolvedBeaconResponse>

    @GET("api/sites/map")
    suspend fun getSiteMap(
        @Query("site_id") siteId: Int = 1,
        @Query("lang") lang: String
    ): Response<SiteMapResponse>

    @POST("api/visit-logs")
    suspend fun recordVisitLog(
        @Body request: VisitLogCreateRequest
    ): Response<Map<String, Any>>

    @POST("api/favorites/toggle")
    suspend fun toggleFavorite(
        @Body request: FavoriteToggleRequestDto
    ): Response<FavoriteToggleResponseDto>

    @GET("api/visit-logs/me")
    suspend fun getMyFavoritesAndHistory(
        @Query("user_id") userId: Int? = null,
        @Query("device_id") deviceId: String = Session.deviceId,
        @Query("lang") lang: String = "vi"
    ): Response<MyVisitLogsResponse>

    @POST("api/feedbacks")
    suspend fun submitFeedback(
        @Body request: FeedbackCreateRequestDto
    ): Response<ArtifactFeedbackDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @PUT("api/users/me")
    suspend fun updateProfile(@Body request: ProfileUpdateDto): Response<Map<String, Any>>
}