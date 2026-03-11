package com.obscura.wallpapers.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.remote.ApiSource
import com.obscura.wallpapers.core.data.remote.service.ApiService
import com.obscura.wallpapers.core.data.remote.service.ProfileResponse
import com.obscura.wallpapers.core.data.remote.service.UserInfo
import com.obscura.wallpapers.core.data.remote.service.ApiResponse
import com.obscura.wallpapers.core.data.repository.UserRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var apiService: ApiService
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { temporaryFolder.newFile("test.preferences_pb") }
        )
        apiService = mock()
        userRepository = UserRepositoryImpl(dataStore, apiService)
    }

    @Test
    fun `saveUserUid should update dataStore`() = runTest {
        val testUid = "user123"
        userRepository.saveUserUid(testUid)
        
        assertEquals(testUid, userRepository.userUid.first())
        assertEquals(testUid, userRepository.getUserUid())
    }

    @Test
    fun `updatePremiumStatus true should set premium and expiry`() = runTest {
        userRepository.updatePremiumStatus(true)
        
        assertTrue(userRepository.isPremiumUser.first())
        assertTrue(userRepository.checkPremiumStatus())
    }

    @Test
    fun `updatePremiumStatus false should remove premium`() = runTest {
        userRepository.updatePremiumStatus(true)
        userRepository.updatePremiumStatus(false)
        
        assertFalse(userRepository.isPremiumUser.first())
    }

    @Test
    fun `clearUserData should remove all user info`() = runTest {
        userRepository.saveUserUid("123")
        userRepository.updatePremiumStatus(true)
        userRepository.updateLoginStatus(true)
        
        userRepository.clearUserData()
        
        assertNull(userRepository.userUid.first())
        assertFalse(userRepository.isPremiumUser.first())
        assertFalse(userRepository.checkUserLoggedIn())
    }

    @Test
    fun `getUserProfile success should cache profile`() = runTest {
        // Arrange
        userRepository.updateLoginStatus(true)
        val userInfo = UserInfo(id = "123", nickname = "Test", email = "test@test.com", isVip = true)
        val profileResponse = ProfileResponse(user = userInfo)
        val apiResponse = ApiResponse(code = 0, data = profileResponse)
        
        whenever(apiService.getProfile()).thenReturn(apiResponse)

        // Act
        val result = userRepository.getUserProfile()

        // Assert
        assertTrue(result is ApiResult.Success)
        val cached = userRepository.getCachedUserProfile()
        assertEquals("123", cached?.user?.id)
        assertEquals("Test", cached?.user?.nickname)
        assertTrue(userRepository.isPremiumUser.first())
    }

    @Test
    fun `getUserProfile not logged in should return error`() = runTest {
        userRepository.updateLoginStatus(false)
        
        val result = userRepository.getUserProfile()
        
        assertTrue(result is ApiResult.Error)
        assertEquals(401, (result as ApiResult.Error).code)
    }

    @Test
    fun `consumeCoins should decrease balance if enough`() = runTest {
        userRepository.updateCoinBalance(100)
        
        val success = userRepository.consumeCoins(40)
        
        assertTrue(success)
        assertEquals(60, userRepository.coinBalance.first())
    }

    @Test
    fun `consumeCoins should return false if not enough balance`() = runTest {
        userRepository.updateCoinBalance(30)
        
        val success = userRepository.consumeCoins(40)
        
        assertFalse(success)
        assertEquals(30, userRepository.coinBalance.first())
    }
}
