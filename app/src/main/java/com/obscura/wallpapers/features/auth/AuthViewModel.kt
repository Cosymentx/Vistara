package com.obscura.wallpapers.features.auth

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.repository.AuthRepository
import com.obscura.wallpapers.core.common.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val context: StringProvider
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userPhotoUrl = MutableStateFlow<String?>(null)
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _loginResult = MutableStateFlow<LoginResult?>(null)
    val loginResult: StateFlow<LoginResult?> = _loginResult.asStateFlow()

    init {
        checkLoginStatus()
        loadUserInfo()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val isLoggedIn = authRepository.checkUserLoggedIn()
                _isLoggedIn.value = isLoggedIn
                Log.d(TAG, "User login status: $isLoggedIn")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking login status", e)
                _isLoggedIn.value = false
            }
        }
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                _userName.value = authRepository.userName.first()
                _userPhotoUrl.value = authRepository.userPhotoUrl.first()
                _userEmail.value = authRepository.userEmail.first()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user info", e)
            }
        }
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val success = authRepository.handleSignInResult(task)
                if (success.isSuccess) {
                    _isLoggedIn.value = true
                    loadUserInfo()
                    _loginResult.value = LoginResult.Success(context.getString(R.string.login_success))
                } else {
                    _loginResult.value = LoginResult.Error(context.getString(R.string.login_failed))
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed", e)
                _loginResult.value = LoginResult.Error(context.getString(R.string.login_failed_with_status, e.statusCode))
            } catch (e: Exception) {
                Log.e(TAG, "Error handling sign in result", e)
                _loginResult.value = LoginResult.Error(context.getString(R.string.login_failed_with_message, e.message.orEmpty()))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _isLoggedIn.value = false
                _userName.value = null
                _userPhotoUrl.value = null
                _userEmail.value = null
                _loginResult.value = LoginResult.Success(context.getString(R.string.logout_success))
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out", e)
                _loginResult.value = LoginResult.Error(context.getString(R.string.logout_failed, e.message.orEmpty()))
            }
        }
    }

    fun clearLoginResult() {
        _loginResult.value = null
    }

    sealed class LoginResult {
        data class Success(val message: String) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}
