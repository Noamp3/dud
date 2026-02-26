package com.smartboiler.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * SmartThings REST API v1 service.
 *
 * Docs: https://developer-preview.smartthings.com/docs/api/public
 */
interface SmartThingsApiService {

    @GET("devices")
    suspend fun listDevices(
        @Header("Authorization") auth: String,
    ): SmartThingsDevicesResponse

    @GET("devices/{deviceId}/status")
    suspend fun getDeviceStatus(
        @Header("Authorization") auth: String,
        @Path("deviceId") deviceId: String,
    ): SmartThingsStatusResponse

    @POST("devices/{deviceId}/commands")
    suspend fun executeCommand(
        @Header("Authorization") auth: String,
        @Path("deviceId") deviceId: String,
        @Body body: SmartThingsCommandRequest,
    ): SmartThingsCommandResponse
}

// --- Response Models ---

data class SmartThingsDevicesResponse(
    val items: List<SmartThingsDevice>,
)

data class SmartThingsDevice(
    val deviceId: String,
    val name: String?,
    val label: String?,
    val roomId: String?,
    val components: List<SmartThingsComponent>?,
)

data class SmartThingsComponent(
    val id: String,
    val capabilities: List<SmartThingsCapability>,
)

data class SmartThingsCapability(
    val id: String,
    val version: Int?,
)

data class SmartThingsStatusResponse(
    val components: Map<String, Map<String, Map<String, SmartThingsAttributeValue>>>?,
)

data class SmartThingsAttributeValue(
    val value: Any?,
)

// --- Command Models ---

data class SmartThingsCommandRequest(
    val commands: List<SmartThingsCommand>,
)

data class SmartThingsCommand(
    val component: String = "main",
    val capability: String,
    val command: String,
    val arguments: List<Any>? = null,
)

data class SmartThingsCommandResponse(
    val results: List<SmartThingsCommandResult>?,
)

data class SmartThingsCommandResult(
    val id: String?,
    val status: String?,
)
