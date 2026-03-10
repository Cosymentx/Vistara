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

    @GET("/api/v1/user/info")
    suspend fun getProfile(): ApiResponse<ProfileResponse>

    @POST("/api/v1/pay/order/top_up/create")
    suspend fun createOrder(@Body request: CheckoutRequest): ApiResponse<CreateOrderResponse>

    @GET("/api/v1/user/goods/gold/list")
    suspend fun getProducts(
        @retrofit2.http.Query("product_type") productQueryType: Int,
        @retrofit2.http.Query("country_id") countryId: Int? = 100
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
    val accessToken: String? = null,
    val user: UserInfo? = null
)

@Serializable
data class UserInfo(
    val id: String? = null,
    val nickname: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val balance: String? = "0",
    @SerialName("is_vip") val isVip: Boolean? = false,
    @SerialName("user_vip") val userVip: UserVip? = null,
    val userCountry: UserCountry? = null,
    @SerialName("isRead") val isRead: String? = "0"
) {
    val uid: Long? get() = id?.toLongOrNull()
    val coins: Int get() = balance?.toIntOrNull() ?: 0
    val isPremium: Boolean get() = isVip == true || userVip?.is_vip == true
}

@Serializable
data class UserVip(
    val expire_at: Long? = 0,
    val is_vip: Boolean? = false,
    val level: Int? = 0,
    val title: String? = ""
)

@Serializable
data class UserCountry(
    val id: String? = null,
    val code: String? = null,
    val title: String? = null,
    val currency: String? = null,
    val symbol: String? = null
)

@Serializable
data class CheckoutRequest(
    @SerialName("product_id") val productId: Int,
    @SerialName("pay_type") val payType: Int,
    @SerialName("sub_type") val subType: Int = 0,
    @SerialName("channel") val channel: String,
    @SerialName("anchor_id") val anchorId: Int = 0,
    @SerialName("country_id") val countryId: Int = 0
)

@Serializable
data class CreateOrderResponse(
    @SerialName("payOrderId") val id: String? = null,
    @SerialName("pay_type") val payMethodId: Int? = null,
    @SerialName("url") val payUrl: String? = null,
    @SerialName("product_id") val priceId: Int? = null,
    @SerialName("pay_record") val payRecord: PayRecord? = null
) {
    val isGooglePay: Boolean get() = payMethodId == 1
    fun apiIsGooglePay(): Boolean = payMethodId == 1
}

@Serializable
data class PayRecord(
    @SerialName("Gold") val gold: Int? = null,
    @SerialName("PayStatus") val payStatus: Int? = null,
    @SerialName("Sku") val sku: String? = null,
    @SerialName("PayUrl") val payUrl: String? = null,
    @SerialName("PayOrderId") val payOrderId: String? = null
)

@Serializable
data class ProductListResponse(
    val goldGoods: List<CoinProduct>? = null,
    val bonus_purchase_cards: List<String>? = null
)

@Serializable
data class CoinProduct(
    @SerialName("coin") val coinStr: String? = null,
    @SerialName("product_id") val id: Int? = null,
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
