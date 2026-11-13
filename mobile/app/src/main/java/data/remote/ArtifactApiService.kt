
package com.example.smartheritage_mobile.data.remote

import com.example.smartheritage_mobile.model.Artifact
import com.example.smartheritage_mobile.model.ResolvedBeaconResponse
import com.example.smartheritage_mobile.model.SiteMapResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ArtifactApiService {

    @GET("api/artifacts")
    suspend fun getArtifacts(
        @Query("lang") lang: String,
        @Query("status") status: String = "PUBLISHED"
    ): List<Artifact>

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
}