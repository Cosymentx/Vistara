package com.obscura.wallpapers.core.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.remote.ApiSource
import com.obscura.wallpapers.core.data.remote.service.ApiService
import com.obscura.wallpapers.core.data.remote.service.ProfileResponse
import com.obscura.wallpapers.core.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户仓库实现类
 * 管理用户数据和状态
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val apiService: ApiService,
) : UserRepository {

    companion object {
        private const val TAG = "UserRepositoryImpl"
        private val IS_PREMIUM_USER = booleanPreferencesKey("is_premium_user")
        private val PREMIUM_EXPIRY_DATE = longPreferencesKey("premium_expiry_date")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val SERVER_TOKEN = stringPreferencesKey("server_token")
        private val USER_COIN_BALANCE = intPreferencesKey("user_coin_balance")
        private val USER_UID = stringPreferencesKey("user_uid")
        private val GOOGLE_ID = stringPreferencesKey("user_id") // 兼容 AuthRepositoryImpl 的 Key

        // 用户信息相关的Key
        private val USER_NICKNAME = stringPreferencesKey("user_nickname")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_AVATAR = stringPreferencesKey("user_avatar")
        private val USER_IS_WHITELIST = stringPreferencesKey("user_is_whitelist")
        private val OPEN_THIRD = booleanPreferencesKey("open_third")
    }

    override val openThird: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[OPEN_THIRD] ?: false
    }

    override suspend fun saveOpenThird(openThird: Boolean) {
        dataStore.edit { preferences ->
            preferences[OPEN_THIRD] = openThird
        }
    }

    override val isPremiumUser: Flow<Boolean> = dataStore.data.map { preferences ->
        val isPremium = preferences[IS_PREMIUM_USER] == true
        val expiryDate = preferences[PREMIUM_EXPIRY_DATE] ?: 0L

        // 如果有过期时间，检查是否已过期
        if (expiryDate > 0) {
            isPremium && System.currentTimeMillis() < expiryDate
        } else {
            isPremium
        }
    }

    override val userNickname: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_NICKNAME]
    }

    override val userAvatar: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_AVATAR]
    }

    override val userEmail: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_EMAIL]
    }

    override val coinBalance: Flow<Int> = dataStore.data.map { preferences ->
        preferences[USER_COIN_BALANCE] ?: 0
    }

    override val userUid: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_UID]
    }

    override suspend fun getUserUid(): String? {
        val prefs = dataStore.data.first()
        val uid = prefs[USER_UID]
        Log.d(TAG, "getUserUid 最终结果: $uid (from USER_UID=${prefs[USER_UID]}, GOOGLE_ID=${prefs[GOOGLE_ID]})")
        return uid
    }

    override suspend fun saveUserUid(uid: String) {
        Log.d(TAG, "保存用户UID: $uid")
        dataStore.edit { preferences ->
            preferences[USER_UID] = uid
        }
    }

    override suspend fun checkPremiumStatus(): Boolean {
        return dataStore.data.map { preferences ->
            val isPremium = preferences[IS_PREMIUM_USER] == true
            val expiryDate = preferences[PREMIUM_EXPIRY_DATE] ?: 0L

            // 如果有过期时间，检查是否已过期
            if (expiryDate > 0) {
                isPremium && System.currentTimeMillis() < expiryDate
            } else {
                isPremium
            }
        }.first()
    }

    override suspend fun getLocalUser() {

    }

    override suspend fun updatePremiumStatus(isPremium: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_PREMIUM_USER] = isPremium

            // 如果是升级为高级用户，设置过期时间为一年后
            if (isPremium) {
                val oneYearInMillis = 365L * 24 * 60 * 60 * 1000
                preferences[PREMIUM_EXPIRY_DATE] = System.currentTimeMillis() + oneYearInMillis
            } else {
                // 如果是取消高级用户，清除过期时间
                preferences.remove(PREMIUM_EXPIRY_DATE)
            }
        }
    }

    override suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.remove(IS_PREMIUM_USER)
            preferences.remove(PREMIUM_EXPIRY_DATE)
            preferences.remove(IS_LOGGED_IN)
            preferences.remove(SERVER_TOKEN)
            preferences.remove(USER_NICKNAME)
            preferences.remove(USER_EMAIL)
            preferences.remove(USER_AVATAR)
            preferences.remove(USER_IS_WHITELIST)
            preferences.remove(USER_COIN_BALANCE)
            preferences.remove(USER_UID)
        }
    }

    /**
     * 获取缓存的用户个人资料
     */
    override suspend fun getCachedUserProfile(): ProfileResponse? {
        return dataStore.data.map { preferences ->
            val nickname = preferences[USER_NICKNAME]
            val email = preferences[USER_EMAIL]
            val avatar = preferences[USER_AVATAR]
            val isWhiteList = preferences[USER_IS_WHITELIST]
            val coins = preferences[USER_COIN_BALANCE]
            val uidStr = preferences[USER_UID]

            if (email != null) {
                ProfileResponse(
                    uid = uidStr?.toLongOrNull(),
                    nickname = nickname ?: "",
                    email = email,
                    avatar = avatar ?: "",
                    coins = coins,
                    isWhiteList = isWhiteList ?: "0"
                )
            } else {
                null
            }
        }.first()
    }

    /**
     * 缓存用户个人资料
     */
    override suspend fun cacheUserProfile(profile: ProfileResponse) {
        Log.d(TAG, "缓存用户个人资料: $profile")
        dataStore.edit { preferences ->
            profile.uid?.let { uid ->
                preferences[USER_UID] = uid.toString()
            }
            preferences[USER_NICKNAME] = profile.nickname
            preferences[USER_EMAIL] = profile.email
            preferences[USER_AVATAR] = profile.avatar
            preferences[USER_IS_WHITELIST] = profile.isWhiteList
            profile.coins?.let { coins ->
                preferences[USER_COIN_BALANCE] = coins
            }
        }
    }

    override suspend fun checkUserLoggedIn(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[IS_LOGGED_IN] == true
        }.first()
    }

    /**
     * 更新用户登录状态
     */
    override suspend fun updateLoginStatus(isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    /**
     * 保存服务器返回的token
     */
    override suspend fun saveServerToken(token: String) {
        android.util.Log.d("UserRepositoryImpl", "保存服务器token: $token")
        dataStore.edit { preferences ->
            preferences[SERVER_TOKEN] = token
        }
    }

    /**
     * 获取服务器token
     */
    override suspend fun getServerToken(): String? {
        val token = dataStore.data.map<Preferences, String?> { preferences ->
            preferences[SERVER_TOKEN]
        }.first()
        return token
    }

    override suspend fun getCoinProducts(productType: Int): ApiResult<List<com.obscura.wallpapers.core.data.remote.service.CoinProduct>> {
        return safeApiCall(ApiSource.BACKEND) {
            val response = apiService.getProducts(productType)
            if (response.isSuccess) {
                response.data?.goldGoods ?: emptyList()
            } else {
                throw Exception(response.msg)
            }
        }
    }

    override suspend fun createOrder(
        productId: Int,
        payType: Int,
        channel: String,
        subType: Int,
        anchorId: Int
    ): ApiResult<com.obscura.wallpapers.core.data.remote.service.CreateOrderResponse> {
        return safeApiCall(ApiSource.BACKEND) {
            val request = com.obscura.wallpapers.core.data.remote.service.CheckoutRequest(
                productId = productId,
                payType = payType,
                channel = channel,
                subType = subType,
                anchorId = anchorId
            )
            val response = apiService.createOrder(request)
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw Exception(response.msg)
            }
        }
    }

    /**
     * 获取用户个人资料
     */
    override suspend fun getUserProfile(): ApiResult<ProfileResponse> {
        // 检查用户是否已登录
        val isLoggedIn = checkUserLoggedIn()
        if (!isLoggedIn) {
            Log.d(TAG, "用户未登录，无法获取个人资料")
            return ApiResult.Error(
                code = 401,
                message = "用户未登录",
                source = ApiSource.BACKEND
            )
        }

        return safeApiCall(ApiSource.BACKEND) {
            val response = apiService.getProfile()
            if (response.isSuccess && response.data != null) {
                // 缓存用户信息
                cacheUserProfile(response.data)

                // 更新本地金币余额
                response.data.coins.let { coins ->
                    updateUserCoinBalance(coins ?: 0)
                }
                response.data
            } else {
                throw Exception(response.msg)
            }
        }
    }

    /**
     * 更新用户金币余额
     */
    override suspend fun updateCoinBalance(amount: Int) {
        dataStore.edit { preferences ->
            preferences[USER_COIN_BALANCE] = amount
        }
    }

    override suspend fun addCoins(amount: Int) {
        dataStore.edit { preferences ->
            val current = preferences[USER_COIN_BALANCE] ?: 0
            preferences[USER_COIN_BALANCE] = current + amount
        }
    }

    override suspend fun consumeCoins(amount: Int): Boolean {
        var success = false
        dataStore.edit { preferences ->
            val current = preferences[USER_COIN_BALANCE] ?: 0
            if (current >= amount) {
                preferences[USER_COIN_BALANCE] = current - amount
                success = true
            }
        }
        return success
    }

    /**
     * 更新用户金币余额 (内部使用)
     */
    private suspend fun updateUserCoinBalance(amount: Int) {
        updateCoinBalance(amount)
    }

    /**
     * 刷新用户个人资料
     * 从服务器获取最新的用户信息并更新本地缓存
     */
    override suspend fun refreshUserProfile() {
        // 检查用户是否已登录
        val isLoggedIn = checkUserLoggedIn()
        if (!isLoggedIn) {
            Log.d(TAG, "用户未登录，无法刷新个人资料")
            return
        }

        try {
            val result = getUserProfile()
            if (result is ApiResult.Success) {
                Log.d(TAG, "用户个人资料刷新成功: ${result.data}")
            } else if (result is ApiResult.Error) {
                Log.e(TAG, "刷新用户个人资料失败: ${result.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "刷新用户个人资料异常: ${e.message}", e)
        }
    }
}
