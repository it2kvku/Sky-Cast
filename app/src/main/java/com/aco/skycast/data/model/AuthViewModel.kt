package com.aco.skycast.data.model

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aco.skycast.R.string.default_web_client_id
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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

    // Update this method in AuthViewModel.kt
    fun startGoogleSignIn(context: Context, launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(default_web_client_id)) // Use this approach instead of hardcoded ID
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        launcher.launch(googleSignInClient.signInIntent)
    }

    fun firebaseAuthWithGoogle(idToken: String) {
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                _authState.value = AuthUiState.Success(result.user)
            } catch (e: Exception) {
                _authState.value = AuthUiState.Error(e.message ?: "Google authentication failed")
            }
        }
    }
    //handleGoogleSignInResult
    fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task?.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                firebaseAuthWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            _authState.value = AuthUiState.Error(e.message ?: "Google sign-in failed")
        }
    }
    fun updateDisplayName(newDisplayName: String) {
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(newDisplayName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                    _authState.value = AuthUiState.Success(user)
                } else {
                    _authState.value = AuthUiState.Error("No user is signed in")
                }
            } catch (e: Exception) {
                _authState.value = AuthUiState.Error(e.message ?: "Failed to update display name")
            }
        }
    }
}