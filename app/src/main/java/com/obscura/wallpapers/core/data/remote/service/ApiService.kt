package com.obscura.wallpapers.core.data.remote.service

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("google/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @GET("/system/user/getUserInfo")
    suspend fun getProfile(): ApiResponse<ProfileResponse>
}

@Serializable
data class ApiPageResponse<T>(
    val code: Int, val msg: String, val rows: List<T>?
) {
    val isSuccess: Boolean
        get() = code == 200

    fun apiOk(): Boolean {
        println("apiOk")
        return code == 200
    }

    fun apiRowsOrEmpty(): List<T> = rows ?: emptyList()
}

@Serializable
data class ApiResponse<T>(
    val code: Int, val msg: String, val data: T?
) {
    val isSuccess: Boolean
        get() = code == 200

    fun apiOk(): Boolean {
        println("apiOk")
        return code == 200
    }

    fun apiDataOrNull(): T? = data
}

@Serializable
data class LoginRequest(
    val nickname: String, val email: String, val avatar: String, val token: String
)

@Serializable
data class LoginResponse(
    val token: String, val isPremium: Boolean? = false
) {
    fun apiIsPremium(): Boolean = isPremium == true
}

@Serializable
data class ProfileResponse(
    val nickname: String,
    val email: String,
    val avatar: String,
    val expireTime: String? = null,
    val diamond: Int?,
    val isWhiteList: String
) {
    val isPremium: Boolean get() = isWhitelisted
    val isWhitelisted: Boolean get() = isWhiteList == "1"
    fun apiIsPremium(): Boolean = isWhitelisted
    fun apiIsWhitelisted(): Boolean = isWhiteList == "1"
}

@Serializable
data class CreateOrderRequest(
    val priceId: String, val paymentMethodId: String
)

@Serializable
data class CreateOrderResponse(
    val id: String,
    val status: String,
    val payUrl: String? = null,
    val priceId: String,
    val diamondNum: Int,
    val payMethodId: Int
) {
    val isGooglePay: Boolean get() = payMethodId == 1
    fun apiIsGooglePay(): Boolean = payMethodId == 1
}

data class PaymentMethod(
    val id: String,
    val name: String,
    val price: String,
    val dollarPrice: String,
    val currency: String,
    val productId: String,
    val payMethodId: Int,
    val payTypeMessage: String,
    val imageUrl: String? = null
) {
    fun apiIsGooglePay(): Boolean = payMethodId == 1
}
