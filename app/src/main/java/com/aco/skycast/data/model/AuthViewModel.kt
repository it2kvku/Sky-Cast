package com.aco.skycast.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Loading : AuthUiState()
    data class Success(val user: FirebaseUser?) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Success(auth.currentUser))
    val authState: StateFlow<AuthUiState> = _authState

    fun signUpWithEmail(email: String, password: String, displayName: String) {
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                result.user?.updateProfile(profileUpdates)?.await()
                _authState.value = AuthUiState.Success(result.user)
            } catch (e: Exception) {
                _authState.value = AuthUiState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthUiState.Success(result.user)
            } catch (e: Exception) {
                _authState.value = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthUiState.Success(null)
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}