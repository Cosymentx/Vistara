package com.obscura.wallpapers.features.test

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.billing.BillingRepository
import com.obscura.wallpapers.core.data.remote.service.ApiService
import com.obscura.wallpapers.core.data.remote.service.CoinProduct
import com.obscura.wallpapers.core.data.remote.service.LoginRequest
import com.obscura.wallpapers.core.data.remote.service.PayChannel
import com.obscura.wallpapers.core.data.repository.AuthRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val billingRepository: BillingRepository,
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "TestViewModel"
    }

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isCoinTestEnabled = MutableStateFlow(false)
    val isCoinTestEnabled: StateFlow<Boolean> = _isCoinTestEnabled.asStateFlow()

    private val _currentCoinBalance = MutableStateFlow(0)
    val currentCoinBalance: StateFlow<Int> = _currentCoinBalance.asStateFlow()

    private val _operationResult = MutableStateFlow<String?>(null)
    val operationResult: StateFlow<String?> = _operationResult.asStateFlow()

    private val _isLoginLoading = MutableStateFlow(false)
    val isLoginLoading: StateFlow<Boolean> = _isLoginLoading.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private val _showPaymentPreview = MutableStateFlow(false)
    val showPaymentPreview: StateFlow<Boolean> = _showPaymentPreview.asStateFlow()

    private val _mockProduct = MutableStateFlow<CoinProduct?>(null)
    val mockProduct: StateFlow<CoinProduct?> = _mockProduct.asStateFlow()

    init {
        checkPremiumStatus()
        checkLoginStatus()
        checkCoinBalance()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val isLoggedIn = userRepository.checkUserLoggedIn()
                _isLoggedIn.value = isLoggedIn
                Log.d(TAG, "Login status: $isLoggedIn")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking login status: ${e.message}")
                _operationResult.value =
                    context.getString(R.string.check_login_status_failed, e.message)
            }
        }
    }

    private fun checkPremiumStatus() {
        viewModelScope.launch {
            try {
                userRepository.isPremiumUser.collect { isPremium ->
                    _isPremiumUser.value = isPremium
                    Log.d(TAG, "Premium status updated: $isPremium")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing premium status: ${e.message}")
                _operationResult.value =
                    context.getString(R.string.check_premium_status_failed, e.message)
            }
        }
    }

    fun enablePremiumUser() {
        viewModelScope.launch {
            try {
                userRepository.updatePremiumStatus(true)
                billingRepository.updateLocalPremiumStatus(true)
                // _isPremiumUser will be updated by the collect in checkPremiumStatus
                _operationResult.value = context.getString(R.string.set_premium_success)
                Log.d(TAG, "User set to premium in both repos")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting premium status: ${e.message}")
                _operationResult.value = context.getString(R.string.set_premium_failed, e.message)
            }
        }
    }

    fun disablePremiumUser() {
        viewModelScope.launch {
            try {
                userRepository.updatePremiumStatus(false)
                billingRepository.updateLocalPremiumStatus(false)
                // _isPremiumUser will be updated by the collect in checkPremiumStatus
                _operationResult.value = context.getString(R.string.disable_premium_success)
                Log.d(TAG, "Premium status disabled in both repos")
            } catch (e: Exception) {
                Log.e(TAG, "Error disabling premium status: ${e.message}")
                _operationResult.value =
                    context.getString(R.string.disable_premium_failed, e.message)
            }
        }
    }

    fun simulateLogin() {
        viewModelScope.launch {
            try {
                userRepository.updateLoginStatus(true)
                _isLoggedIn.value = true
                _operationResult.value = context.getString(R.string.simulate_login_success)
                Log.d(TAG, "Simulated login successful")
            } catch (e: Exception) {
                Log.e(TAG, "Error simulating login: ${e.message}")
                _operationResult.value =
                    context.getString(R.string.simulate_login_failed, e.message)
            }
        }
    }

    fun simulateLogout() {
        viewModelScope.launch {
            try {
                userRepository.updateLoginStatus(false)
                _isLoggedIn.value = false
                _operationResult.value = context.getString(R.string.simulate_logout_success)
                Log.d(TAG, "Simulated logout successful")
            } catch (e: Exception) {
                Log.e(TAG, "Error simulating logout: ${e.message}")
                _operationResult.value =
                    context.getString(R.string.simulate_logout_failed, e.message)
            }
        }
    }

    private fun checkCoinBalance() {
        viewModelScope.launch {
            try {
                userRepository.coinBalance.collect { balance ->
                    _currentCoinBalance.value = balance
                    Log.d(TAG, "Coin balance: $balance")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking coin balance: ${e.message}")
                _operationResult.value = context.getString(R.string.common_unknown_error)
            }
        }
    }

    fun toggleCoinTest() {
        val newState = !_isCoinTestEnabled.value
        _isCoinTestEnabled.value = newState

        viewModelScope.launch {
            try {
                if (newState) {
                    val currentBalance = _currentCoinBalance.value
                    userRepository.updateCoinBalance(currentBalance + 200)
                    _operationResult.value = context.getString(R.string.test_mode_enabled)
                } else {
                    userRepository.updateCoinBalance(0)
                    _operationResult.value = context.getString(R.string.test_mode_disabled)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling coin test: ${e.message}")
                _operationResult.value = context.getString(R.string.test_failed, e.message)
            }
        }
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }

    fun showLoginDialog() {
        _showLoginDialog.value = true
    }

    fun hideLoginDialog() {
        _showLoginDialog.value = false
    }

    fun showPaymentPreview() {
        _mockProduct.value = CoinProduct(
            id = 999,
            name = "Test 1000 Coins",
            showPrice = "$9.99",
            sku = "com.obscura.coins.1000",
            channels = listOf(
                PayChannel(channel = "1", name = "Google Play", icon = "ic_play_store"),
                PayChannel(channel = "2", name = "Visa/Mastercard", icon = "https://cdn-icons-png.flaticon.com/512/349/349221.png"),
                PayChannel(channel = "3", name = "PayPal", icon = "https://cdn-icons-png.flaticon.com/512/174/174861.png")
            )
        )
        _showPaymentPreview.value = true
    }

    fun hidePaymentPreview() {
        _showPaymentPreview.value = false
    }

    fun loginWithEmail(email: String) {
        viewModelScope.launch {
            try {
                _isLoginLoading.value = true

                val randomNickname = generateRandomNickname()
                val randomToken = generateRandomToken()

                Log.d(TAG, "随机生成nickname: $randomNickname, token: $randomToken")

                val loginRequest = LoginRequest(
                    nickname = randomNickname,
                    email = email,
                    avatar = "https://api.dicebear.com/7.x/micah/png?seed=${email}",
                    googleToken = randomToken,
                    authId = "mock_auth_id_${System.currentTimeMillis()}",
                    authType = 1,
                    authToken = randomToken,
                )

                val result = apiService.login(loginRequest)
                Log.d(TAG, "登录请求结果: $result")

                if (result.isSuccess) {
                    val loginResponse = result.data
                    userRepository.saveUserUid(loginResponse?.uid.toString())
                    userRepository.saveServerToken(loginResponse?.accessToken ?: "")
                    userRepository.updateLoginStatus(true)
                    _isLoggedIn.value = true
                    saveUserInfo(
                        userId = "${loginResponse?.uid}",
                        userName = randomNickname,
                        userEmail = email,
                        userPhotoUrl = "https://api.dicebear.com/7.x/micah/png?seed=${email}"
                    )
                    loginResponse?.isPremium?.let { isPremium ->
                        userRepository.updatePremiumStatus(isPremium)
                        _isPremiumUser.value = isPremium
                    }
                    _operationResult.value = context.getString(R.string.auth_login_success)
                    hideLoginDialog()
                    Log.d(TAG, "Login successful")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during login: ${e.message}")
                _operationResult.value = context.getString(R.string.auth_login_failed)
            } finally {
                _isLoginLoading.value = false
            }
        }
    }

    private fun generateRandomNickname(): String {
        val adjectives = listOf(
            "快乐的", "聪明的", "勇敢的", "可爱的", "友善的",
            "活泼的", "机智的", "温柔的", "善良的", "幽默的"
        )
        val nouns =
            listOf("熊猫", "老虎", "狮子", "猫咪", "狗狗", "兔子", "松鼠", "大象", "长颈鹿", "猴子")

        val randomAdjective = adjectives.random()
        val randomNoun = nouns.random()
        val randomNumber = (1000..9999).random()

        return "$randomAdjective$randomNoun$randomNumber"
    }

    private fun generateRandomToken(): String {
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..32).map { charPool.random() }.joinToString("")
    }

    private suspend fun saveUserInfo(
        userId: String, userName: String, userEmail: String, userPhotoUrl: String
    ) {
        try {
            authRepository.saveUserInfo(userId, userName, userEmail, userPhotoUrl)
            Log.d(TAG, "用户信息保存成功: $userName, $userEmail")
        } catch (e: Exception) {
            Log.e(TAG, "保存用户信息失败: ${e.message}")
        }
    }
}
