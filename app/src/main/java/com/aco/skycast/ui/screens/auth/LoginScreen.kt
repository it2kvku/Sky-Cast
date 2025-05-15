package com.aco.skycast.ui.screens.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aco.skycast.data.model.AuthUiState
import com.aco.skycast.data.model.AuthViewModel
import androidx.compose.ui.res.painterResource
import com.aco.skycast.R

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Google Sign-In result handler
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google sign-in result: ${result.resultCode}")
        authViewModel.handleGoogleSignInResult(result.data)
    }
    
    // Check authentication state
    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                val user = (authState as AuthUiState.Success).user
                if (user != null) {
                    Log.d(TAG, "Login successful for user: ${user.displayName}")
                    onLoginSuccess()
                }
            }
            is AuthUiState.Error -> {
                errorMessage = (authState as AuthUiState.Error).message
                Log.e(TAG, "Auth error: $errorMessage")
                showError = true
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = "Weather Logo",
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(64.dp)
        )

        Text(
            text = "Đăng Nhập",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D47A1),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (showError) {
            Text(
                text = errorMessage,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Mật khẩu") },
            visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible.value)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Quên mật khẩu?",
            color = Color(0xFF1976D2),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.End)
                .clickable(onClick = { /* Handle forgot password */ })
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                showError = false
                if (email.value.isBlank() || password.value.isBlank()) {
                    errorMessage = "Vui lòng điền đầy đủ thông tin"
                    showError = true
                    return@Button
                }

                authViewModel.loginWithEmail(email.value, password.value)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            if (authState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text("Đăng nhập", color = Color.White)
            }
        }

        Text(
            text = "hoặc",
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Google Sign-In Button
        OutlinedButton(
            onClick = {
                showError = false
                Log.d(TAG, "Starting Google sign-in flow")
                authViewModel.startGoogleSignIn(context, googleSignInLauncher)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified // Important to show Google's colors
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đăng nhập bằng Google", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("Chưa có tài khoản?")
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Đăng ký",
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onSignUpClick)
            )
        }
    }
}
