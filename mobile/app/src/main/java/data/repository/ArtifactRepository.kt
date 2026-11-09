package com.example.smartheritage_mobile.data.repository

import com.example.smartheritage_mobile.data.remote.RetrofitClient
import com.example.smartheritage_mobile.model.Artifact

class ArtifactRepository {

    suspend fun getArtifacts(lang: String): List<Artifact> {

        return RetrofitClient.artifactApi.getArtifacts(
            lang = lang
        )
    }
}