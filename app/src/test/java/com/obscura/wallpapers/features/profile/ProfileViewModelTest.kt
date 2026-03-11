package com.obscura.wallpapers.features.profile

import app.cash.turbine.test
import com.obscura.wallpapers.core.data.repository.AuthRepository
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userPrefsRepository: UserPrefsRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: ProfileViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mock()
        userPrefsRepository = mock()
        authRepository = mock()

        // Default mock behaviors
        whenever(userRepository.isPremiumUser).thenReturn(flowOf(false))
        whenever(userRepository.coinBalance).thenReturn(flowOf(0))
        whenever(authRepository.userName).thenReturn(flowOf(null))
        whenever(authRepository.userPhotoUrl).thenReturn(flowOf(null))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should load user data correctly when logged in`() = runTest {
        // Arrange
        whenever(userRepository.checkUserLoggedIn()).thenReturn(true)
        whenever(userRepository.isPremiumUser).thenReturn(flowOf(true))
        whenever(authRepository.userName).thenReturn(flowOf("Test User"))
        whenever(userRepository.coinBalance).thenReturn(flowOf(500))

        // Act
        viewModel = ProfileViewModel(userRepository, userPrefsRepository, authRepository)
        
        // Assert
        assertTrue(viewModel.isLoggedIn.value)
        assertTrue(viewModel.isPremiumUser.value)
        assertEquals("Test User", viewModel.username.value)
        
        viewModel.coinBalance.test {
            assertEquals(500, awaitItem())
        }
    }

    @Test
    fun `initial state should load user data correctly when not logged in`() = runTest {
        // Arrange
        whenever(userRepository.checkUserLoggedIn()).thenReturn(false)
        whenever(userRepository.isPremiumUser).thenReturn(flowOf(false))

        // Act
        viewModel = ProfileViewModel(userRepository, userPrefsRepository, authRepository)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isLoggedIn.value)
        assertFalse(viewModel.isPremiumUser.value)
        assertEquals("Obscura User", viewModel.username.value)
    }

    @Test
    fun `upgradeToPremium should update repository and state`() = runTest {
        // Arrange
        whenever(userRepository.checkUserLoggedIn()).thenReturn(true)
        viewModel = ProfileViewModel(userRepository, userPrefsRepository, authRepository)

        // Act
        viewModel.upgradeToPremium()
        advanceUntilIdle()

        // Assert
        verify(userRepository).updatePremiumStatus(true)
        assertTrue(viewModel.isPremiumUser.value)
    }

    @Test
    fun `clearUserData should clear repositories and refresh state`() = runTest {
        // Arrange
        whenever(userRepository.checkUserLoggedIn()).thenReturn(true)
        viewModel = ProfileViewModel(userRepository, userPrefsRepository, authRepository)
        advanceUntilIdle()

        // Act
        viewModel.clearUserData()
        advanceUntilIdle()

        // Assert
        verify(userRepository).clearUserData()
        verify(userPrefsRepository).clearUserSettings()
        assertFalse(viewModel.isPremiumUser.value)
    }

    @Test
    fun `checkLoginAndExecute should set needLoginAction when not logged in`() = runTest {
        // Arrange
        whenever(userRepository.checkUserLoggedIn()).thenReturn(false)
        viewModel = ProfileViewModel(userRepository, userPrefsRepository, authRepository)
        advanceUntilIdle()

        var executed = false

        // Act
        val result = viewModel.checkLoginAndExecute(ProfileViewModel.LoginAction.FAVORITES) {
            executed = true
        }

        // Assert
        assertFalse(result)
        assertFalse(executed)
        assertEquals(ProfileViewModel.LoginAction.FAVORITES, viewModel.needLoginAction.value)
    }

    @Test
    fun `checkLoginAndExecute should execute action when logged in`() = runTest {
        // Arrange
        whenever(userRepository.checkUserLoggedIn()).thenReturn(true)
        viewModel = ProfileViewModel(userRepository, userPrefsRepository, authRepository)
        advanceUntilIdle()

        var executed = false

        // Act
        val result = viewModel.checkLoginAndExecute(ProfileViewModel.LoginAction.FAVORITES) {
            executed = true
        }

        // Assert
        assertTrue(result)
        assertTrue(executed)
        assertEquals(null, viewModel.needLoginAction.value)
    }
}
