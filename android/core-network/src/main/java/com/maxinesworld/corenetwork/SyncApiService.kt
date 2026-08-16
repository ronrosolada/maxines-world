package com.maxinesworld.corenetwork

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApiService {

    @GET("/health")
    suspend fun healthCheck(): Map<String, String>

    @POST("/api/v1/sync/push")
    suspend fun pushSync(
        @Body request: SyncPushRequest,
    ): SyncPushResponse

    @GET("/api/v1/sync/pull")
    suspend fun pullSync(
        @Query("childId") childId: String,
        @Query("sinceEpochMillis") sinceEpochMillis: Long,
    ): SyncPullResponse
}
