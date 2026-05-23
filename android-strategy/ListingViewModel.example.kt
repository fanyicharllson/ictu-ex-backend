package com.fanyiadrien.ictu_ex.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanyiadrien.ictu_ex.data.remote.api.CreateListingRequest
import com.fanyiadrien.ictu_ex.data.remote.api.Listing
import com.fanyiadrien.ictu_ex.data.remote.api.ListingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * USAGE EXAMPLE — ListingViewModel
 *
 * Drop this into:  feature/home/ListingViewModel.kt
 */

sealed class ListingUiState {
    object Idle                              : ListingUiState()
    object Loading                           : ListingUiState()
    data class ListLoaded(val items: List<Listing>) : ListingUiState()
    data class DetailLoaded(val item: Listing)      : ListingUiState()
    data class Error(val message: String)           : ListingUiState()
}

@HiltViewModel
class ListingViewModel @Inject constructor(
    private val listingService: ListingService  // ← interface only
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListingUiState>(ListingUiState.Idle)
    val uiState: StateFlow<ListingUiState> = _uiState

    fun loadAllListings() {
        viewModelScope.launch {
            _uiState.value = ListingUiState.Loading
            listingService.getAllListings()
                .onSuccess { _uiState.value = ListingUiState.ListLoaded(it) }
                .onFailure { _uiState.value = ListingUiState.Error(it.message ?: "Failed to load listings") }
        }
    }

    fun loadListing(id: String) {
        viewModelScope.launch {
            _uiState.value = ListingUiState.Loading
            listingService.getListingById(id)
                .onSuccess { _uiState.value = ListingUiState.DetailLoaded(it) }
                .onFailure { _uiState.value = ListingUiState.Error(it.message ?: "Listing not found") }
        }
    }

    fun search(title: String? = null, category: String? = null) {
        viewModelScope.launch {
            _uiState.value = ListingUiState.Loading
            listingService.searchListings(title, category)
                .onSuccess { _uiState.value = ListingUiState.ListLoaded(it) }
                .onFailure { _uiState.value = ListingUiState.Error(it.message ?: "Search failed") }
        }
    }

    fun createListing(request: CreateListingRequest) {
        viewModelScope.launch {
            _uiState.value = ListingUiState.Loading
            listingService.createListing(request)
                .onSuccess { _uiState.value = ListingUiState.DetailLoaded(it) }
                .onFailure { _uiState.value = ListingUiState.Error(it.message ?: "Failed to create listing") }
        }
    }

    fun deleteListing(id: String) {
        viewModelScope.launch {
            listingService.deleteListing(id)
                .onSuccess { loadAllListings() }
                .onFailure { _uiState.value = ListingUiState.Error(it.message ?: "Failed to delete listing") }
        }
    }
}
