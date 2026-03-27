package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.BookmarkedTutorial
import com.gcuf.craftoria.data.model.LearningCategory
import com.gcuf.craftoria.data.repository.LearningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearningViewModel(
    private val learningRepository: LearningRepository = LearningRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LearningState>(LearningState.Loading)
    val uiState: StateFlow<LearningState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<LearningCategory>>(emptyList())
    val categories: StateFlow<List<LearningCategory>> = _categories.asStateFlow()

    private val _bookmarks = MutableStateFlow<Set<String>>(emptySet())
    val bookmarks: StateFlow<Set<String>> = _bookmarks.asStateFlow()

    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())
    val expandedCategories: StateFlow<Set<String>> = _expandedCategories.asStateFlow()

    companion object {
        private const val TAG = "LearningViewModel"
    }

    fun loadCategories(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = LearningState.Loading

                learningRepository.observeAllCategories().collect { result ->
                    if (result.isSuccess) {
                        _categories.value = result.getOrNull() ?: emptyList()
                        // Load user bookmarks once on first success
                        if (_bookmarks.value.isEmpty()) {
                            loadBookmarks(userId)
                        }
                        _uiState.value = if (_categories.value.isEmpty()) {
                            LearningState.Empty
                        } else {
                            LearningState.Success
                        }
                    } else {
                        _uiState.value = LearningState.Error(
                            result.exceptionOrNull()?.message ?: "Failed to load resources"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to observe categories", e)
                _uiState.value = LearningState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadBookmarks(userId: String) {
        viewModelScope.launch {
            try {
                val result = learningRepository.getUserBookmarks(userId)
                if (result.isSuccess) {
                    val bookmarkedTutorials = result.getOrNull() ?: emptyList()
                    _bookmarks.value = bookmarkedTutorials.map { it.tutorialId }.toSet()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bookmarks", e)
            }
        }
    }

    fun toggleCategory(categoryId: String) {
        val current = _expandedCategories.value.toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        _expandedCategories.value = current
    }

    fun toggleBookmark(userId: String, tutorialId: String, categoryId: String) {
        viewModelScope.launch {
            try {
                val result = learningRepository.toggleBookmark(userId, tutorialId, categoryId)

                if (result.isSuccess) {
                    val isBookmarked = result.getOrNull() ?: false
                    val current = _bookmarks.value.toMutableSet()

                    if (isBookmarked) {
                        current.add(tutorialId)
                    } else {
                        current.remove(tutorialId)
                    }

                    _bookmarks.value = current
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle bookmark", e)
            }
        }
    }

    fun searchTutorials(query: String, userId: String) {
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    loadCategories(userId)
                    return@launch
                }

                _uiState.value = LearningState.Loading

                val result = learningRepository.searchTutorials(query)

                if (result.isSuccess) {
                    val filtered = result.getOrNull() ?: emptyList()
                    _categories.value = filtered

                    // Expand all categories in search results
                    _expandedCategories.value = filtered.map { it.id }.toSet()

                    _uiState.value = if (filtered.isEmpty()) {
                        LearningState.Empty
                    } else {
                        LearningState.Success
                    }
                } else {
                    _uiState.value = LearningState.Error("Search failed")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _uiState.value = LearningState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = LearningState.Success
    }
}

sealed class LearningState {
    object Loading : LearningState()
    object Success : LearningState()
    object Empty : LearningState()
    data class Error(val message: String) : LearningState()
}