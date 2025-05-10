package com.aco.skycast.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aco.skycast.data.model.IpLocationResponse
import com.aco.skycast.data.repository.IpLocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class IpLocationViewModel : ViewModel() {
    private val repository = IpLocationRepository()

    private val _locationState = MutableStateFlow<LocationUiState>(LocationUiState.Loading)
    val locationState: StateFlow<LocationUiState> = _locationState

    init {
        fetchIpLocation()
    }

    fun fetchIpLocation() {
        viewModelScope.launch {
            _locationState.value = LocationUiState.Loading
            repository.getIpLocation()
                .onSuccess { location ->
                    _locationState.value = LocationUiState.Success(location)
                }
                .onFailure { e ->
                    _locationState.value = LocationUiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}

sealed class LocationUiState {
    object Loading : LocationUiState()
    data class Success(val data: IpLocationResponse) : LocationUiState()
    data class Error(val message: String) : LocationUiState()
}