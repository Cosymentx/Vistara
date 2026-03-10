package com.obscura.wallpapers.core.data.repository

import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.remote.service.ProfileResponse
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val isPremiumUser: Flow<Boolean>
    suspend fun checkPremiumStatus(): Boolean
    suspend fun updatePremiumStatus(isPremium: Boolean)
    suspend fun getLocalUser()
    suspend fun clearUserData()
    suspend fun checkUserLoggedIn(): Boolean
    suspend fun updateLoginStatus(isLoggedIn: Boolean)
    suspend fun saveServerToken(token: String)
    suspend fun getServerToken(): String?

    /**
     * 保存是否开启三方支付
     */
    suspend fun saveOpenThird(openThird: Boolean)

    /**
     * 获取是否开启三方支付
     */
    val openThird: Flow<Boolean>

    /**
     * 获取金币产品列表
     * @param productType 1普通商品。2vip 商品 3首充商品
     */
    suspend fun getCoinProducts(productType: Int = 1): ApiResult<List<com.obscura.wallpapers.core.data.remote.service.CoinProduct>>

    /**
     * 创建订单
     */
    suspend fun createOrder(
        productId: Int,
        payType: Int,
        channel: String,
        subType: Int = 0,
        anchorId: Int = 0
    ): ApiResult<com.obscura.wallpapers.core.data.remote.service.CreateOrderResponse>

    /**
     * 获取用户UID
     */
    val userUid: Flow<String?>

    /**
     * 获取用户UID (挂起函数)
     */
    suspend fun getUserUid(): String?

    /**
     * 保存用户UID
     */
    suspend fun saveUserUid(uid: String)

    /**
     * 获取用户个人资料
     * @return 用户个人资料的API结果
     */
    suspend fun getUserProfile(): ApiResult<ProfileResponse>

    /**
     * 刷新用户个人资料
     * 从服务器获取最新的用户信息并更新本地缓存
     */
    suspend fun refreshUserProfile()

    /**
     * 获取缓存的用户个人资料
     * @return 缓存的用户个人资料，如果没有缓存则返回null
     */
    suspend fun getCachedUserProfile(): ProfileResponse?

    /**
     * 缓存用户个人资料
     * @param profile 用户个人资料
     */
    suspend fun cacheUserProfile(profile: ProfileResponse)

    /**
     * 获取用户昵称
     * @return 用户昵称的Flow
     */
    val userNickname: Flow<String?>

    /**
     * 获取用户头像
     * @return 用户头像URL的Flow
     */
    val userAvatar: Flow<String?>

    /**
     * 获取用户邮箱
     * @return 用户邮箱的Flow
     */
    val userEmail: Flow<String?>

    /**
     * 获取用户金币余额
     * @return 用户金币余额的Flow
     */
    val coinBalance: Flow<Int>

    /**
     * 获取用户国家ID
     * @return 用户国家ID的Flow
     */
    val countryId: Flow<Int>

    /**
     * 更新用户金币余额
     * @param amount 新s的余额
     */
    suspend fun updateCoinBalance(amount: Int)

    /**
     * 增加用户金币余额
     * @param amount 增加的数量
     */
    suspend fun addCoins(amount: Int)

    /**
     * 减少用户金币余额
     * @param amount 减少的数量
     * @return 是否扣费成功（余额不足返回false）
     */
    suspend fun consumeCoins(amount: Int): Boolean
}
