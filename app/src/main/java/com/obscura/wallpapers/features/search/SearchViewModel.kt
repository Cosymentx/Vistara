package com.obscura.wallpapers.features.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperCategory
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SearchViewModel"
        private const val MAX_HISTORY_SIZE = 10
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Wallpaper>>(emptyList())
    val searchResults: StateFlow<List<Wallpaper>> = _searchResults.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<WallpaperCategory>>(emptyList())
    val searchSuggestions: StateFlow<List<WallpaperCategory>> = _searchSuggestions.asStateFlow()

    private val _hotSearches = MutableStateFlow<List<WallpaperCategory>>(emptyList())
    val hotSearches: StateFlow<List<WallpaperCategory>> = _hotSearches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isPremiumUser = userRepository.isPremiumUser

    private val _purchasedIds = MutableStateFlow<Set<String>>(emptySet())
    val purchasedIds: StateFlow<Set<String>> = _purchasedIds.asStateFlow()

    init {
        loadSearchHistory()
        loadHotSearches()
        loadPurchasedWallpapers()
    }

    private fun loadPurchasedWallpapers() {
        viewModelScope.launch {
            // Check purchased wallpapers from repository
            // Note: Currently WallpaperRepository doesn't expose a Flow of all purchased IDs, 
            // but we can mark them as we load results or query specific ones.
            // For now, we'll initialize it as empty and rely on markWallpaperAsPurchased logic if needed.
        }
    }

    private fun loadHotSearches() {
        _hotSearches.value = WallpaperCategory.getAllCategories().subList(0, 5)
    }

    private fun loadSearchHistory() {
        viewModelScope.launch {
            try {
                val history = userPrefsRepository.getSearchHistory()
                _searchHistory.value = history
            } catch (_: Exception) {
            }
        }
    }

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isNotEmpty()) {
            generateSuggestions(newQuery)
        } else {
            _searchSuggestions.value = emptyList()
        }
    }

    private fun generateSuggestions(query: String) {
        if (query.length < 2) {
            _searchSuggestions.value = emptyList()
            return
        }

        val historyMatches = _searchHistory.value.filter {
            it.contains(query, ignoreCase = true)
        }

        val hotMatches = _hotSearches.value.filter {
            it.name.contains(query, ignoreCase = true)
        }

        val suggestions = (historyMatches + hotMatches).distinct().take(5)
        _searchSuggestions.value = suggestions as List<WallpaperCategory>
    }

    fun search(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val results = wallpaperRepository.searchWallpapers(query, 1, 20)
                
                // For each result, check if it's already purchased
                val resultsWithPurchaseStatus = results.map { wallpaper ->
                    if (wallpaper.requiresPurchase) {
                        val isPurchased = wallpaperRepository.isWallpaperPurchased(wallpaper.id)
                        if (isPurchased) {
                            _purchasedIds.value += wallpaper.id
                        }
                    }
                    wallpaper
                }
                
                _searchResults.value = resultsWithPurchaseStatus
                addToSearchHistory(query)
            } catch (e: Exception) {
                _error.value = e.message ?: "搜索失败，请重试"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun addToSearchHistory(query: String) {
        viewModelScope.launch {
            try {
                val currentHistory = _searchHistory.value.toMutableList()
                currentHistory.remove(query)
                currentHistory.add(0, query)
                val newHistory = currentHistory.take(MAX_HISTORY_SIZE)
                _searchHistory.value = newHistory
                userPrefsRepository.saveSearchHistory(newHistory)
            } catch (_: Exception) {
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            try {
                userPrefsRepository.clearSearchHistory()
                _searchHistory.value = emptyList()
            } catch (_: Exception) {
            }
        }
    }

    fun selectFromHistory(historyItem: WallpaperCategory) {
        _query.value = historyItem.apiValue
        search(historyItem.apiValue)
    }

    fun selectSuggestion(suggestion: String) {
        _query.value = suggestion
        search(suggestion)
    }
}
