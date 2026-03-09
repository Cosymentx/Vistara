package com.obscura.wallpapers.core.data.remote.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("/api/v1/user/green/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @GET("/system/user/getUserInfo")
    suspend fun getProfile(): ApiResponse<ProfileResponse>

    @POST("/system/order/add")
    suspend fun createOrder(@Body request: CreateOrderRequest): ApiResponse<CreateOrderResponse>

    @GET("/api/v1/user/goods/gold/list")
    suspend fun getProducts(
        @retrofit2.http.Query("product_type") productQueryType: Int,
        @retrofit2.http.Query("country_id") countryId: Int? = 0
    ): ApiResponse<ProductListResponse>

    @GET("/system/method/getPayMethod/{itemName}")
    suspend fun getPaymentMethods(@Path("itemName") itemName: String): ApiResponse<List<PaymentMethod>>

    @GET("/system/order/list")
    suspend fun getOrders(): ApiPageResponse<CoinTransaction>
}

@Serializable
data class ApiPageResponse<T>(
    val code: Int,
    val msg: String? = null,
    val message: String? = null,
    val rows: List<T>? = null
) {
    val isSuccess: Boolean
        get() = code == 200 || code == 0

    val apiMsg: String
        get() = message ?: msg ?: ""

    fun apiOk(): Boolean = isSuccess

    fun apiRowsOrEmpty(): List<T> = rows ?: emptyList()
}

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val msg: String? = null,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = code == 200 || code == 0

    val apiMsg: String
        get() = message ?: msg ?: ""

    fun apiOk(): Boolean = isSuccess

    fun apiDataOrNull(): T? = data
}

@Serializable
data class LoginRequest(
    @SerialName("nick_name") val nickname: String,
    @SerialName("email") val email: String,
    @SerialName("avatar") val avatar: String,
    @SerialName("google_token") val googleToken: String,
    @SerialName("auth_id") val authId: String,
    @SerialName("auth_type") val authType: Int,
    @SerialName("auth_token") val authToken: String,
    @SerialName("skip_update_info") val skipUpdateInfo: Boolean = true
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("app_id") val appId: Int? = null,
    @SerialName("is_check") val isCheck: Boolean? = false,
    @SerialName("open_third") val openThird: Boolean? = false,//是否展示三方支付
    val uid: Long? = null,
    val isPremium: Boolean? = false
) {
    fun apiIsPremium(): Boolean = isPremium == true || isCheck == true
}

@Serializable
data class ProfileResponse(
    val uid: Long? = null,
    val nickname: String,
    val email: String,
    val avatar: String,
    val expireTime: String? = null,
    val coins: Int? = null,
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
    val coinsNum: Int,
    val payMethodId: Int
) {
    val isGooglePay: Boolean get() = payMethodId == 1
    fun apiIsGooglePay(): Boolean = payMethodId == 1
}

@Serializable
data class ProductListResponse(
    val goldGoods: List<CoinProduct>? = null,
    val bonus_purchase_cards: List<String>? = null
)

@Serializable
data class CoinProduct(
    @SerialName("coin") val coinStr: String? = null,
    val id: String? = null,
    val sku: String? = null,
    val name: String? = null,
    val title: String? = null,
    val price: Int? = null,
    @SerialName("show_initial_price") val showInitialPrice: String? = null,
    @SerialName("show_price") val showPrice: String? = null,
    val description: String? = null,
    val channels: List<PayChannel>? = null
) {
    val coins: Int get() = coinStr?.toIntOrNull() ?: 0
}

@Serializable
data class PayChannel(
    val channel: String? = null,
    val name: String? = null,
    val icon: String? = null,
    val payType: Int? = null,
    @SerialName("show_price") val showPrice: String? = null,
    @SerialName("show_initial_price") val showInitialPrice: String? = null,
    val currency: String? = null,
    val price: Int? = null,
    @SerialName("third_channel_id") val thirdChannelId: Int? = null,
    @SerialName("payUrl") val payUrl: String? = null
)

@Serializable
data class CoinTransaction(
    val id: String,
    @SerialName("order_no") val orderNo: String? = null,
    val amount: String? = null,
    val status: Int? = null,
    @SerialName("create_time") val createTime: String? = null
)

@Serializable
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
