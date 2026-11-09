
package com.example.smartheritage_mobile.data.remote

import com.example.smartheritage_mobile.model.Artifact
import retrofit2.http.GET
import retrofit2.http.Query

interface ArtifactApiService {

    @GET("api/artifacts")
    suspend fun getArtifacts(
        @Query("lang") lang: String,
        @Query("status") status: String = "PUBLISHED"
    ): List<Artifact>
}