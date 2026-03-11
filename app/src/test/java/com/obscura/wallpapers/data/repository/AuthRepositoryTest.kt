package com.obscura.wallpapers.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.obscura.wallpapers.core.data.remote.service.ApiService
import com.obscura.wallpapers.core.data.remote.service.LoginRequest
import com.obscura.wallpapers.core.data.remote.service.LoginResponse
import com.obscura.wallpapers.core.data.remote.service.ApiResponse
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.obscura.wallpapers.core.data.repository.AuthRepositoryImpl
import com.obscura.wallpapers.core.data.repository.UserRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@Suppress("DEPRECATION", "UNCHECKED_CAST")
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var apiService: ApiService
    private lateinit var userRepository: UserRepository
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        context = mock()
        dataStore = mock()
        apiService = mock()
        userRepository = mock()

        // Mock DataStore
        val preferences: Preferences = mock()
        whenever(dataStore.data).thenReturn(flowOf(preferences))

        authRepository = AuthRepositoryImpl(context, dataStore, userRepository, apiService)
    }

    @Test
    fun `handleSignInResult success`() = runTest {
        // 准备测试数据
        val account: GoogleSignInAccount = mock {
            on { id } doReturn "test_id"
            on { displayName } doReturn "Test User"
            on { email } doReturn "test@example.com"
            on { photoUrl } doReturn null
            on { idToken } doReturn "test_token"
        }

        val task: Task<GoogleSignInAccount> = mock {
            on { getResult(Exception::class.java) } doReturn account
        }

        val testUid = 12345L
        val loginResponse = LoginResponse(accessToken = "server_token", isPremium = true, uid = testUid)
        whenever(apiService.login(any())).thenReturn(ApiResponse(code = 0, message = "Success", data = loginResponse))

        // 执行测试
        val result = authRepository.handleSignInResult(task)

        // 验证结果
        assert(result.isSuccess)
        verify(userRepository).updateLoginStatus(true)
        verify(userRepository).saveServerToken("server_token")
        verify(userRepository).saveUserUid(testUid.toString())
        verify(userRepository).updatePremiumStatus(true)
        
        // 验证请求参数
        verify(apiService).login(eq(LoginRequest(
            nickname = "Test User",
            email = "test@example.com",
            avatar = "",
            googleToken = "test_token",
            authId = "test_id",
            authType = 1,
            authToken = "test_token",
            skipUpdateInfo = true
        )))
    }

    @Test
    fun `handleSignInResult failure`() = runTest {
        // 准备测试数据
        val account: GoogleSignInAccount = mock {
            on { id } doReturn "test_id"
            on { displayName } doReturn "Test User"
            on { email } doReturn "test@example.com"
            on { photoUrl } doReturn null
            on { idToken } doReturn "test_token"
        }

        val task: Task<GoogleSignInAccount> = mock {
            on { getResult(Exception::class.java) } doReturn account
        }

        whenever(apiService.login(any())).thenReturn(ApiResponse(code = 400, message = "Bad Request", data = null))

        // 执行测试
        val result = authRepository.handleSignInResult(task)

        // 验证结果
        assert(!result.isSuccess)
        
        // 验证请求参数
        verify(apiService).login(eq(LoginRequest(
            nickname = "Test User",
            email = "test@example.com",
            avatar = "",
            googleToken = "test_token",
            authId = "test_id",
            authType = 1,
            authToken = "test_token",
            skipUpdateInfo = true
        )))
    }
}
