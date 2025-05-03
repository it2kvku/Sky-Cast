package com.aco.skycast.ui.screens.auth

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aco.skycast.data.model.AuthUiState
import com.aco.skycast.data.model.AuthViewModel

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSignUpSuccess: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    val displayName = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val acceptedTerms = remember { mutableStateOf(true) }
    val passwordVisible = remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Check authentication state
    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                val user = (authState as AuthUiState.Success).user
                if (user != null) {
                    onSignUpSuccess()
                }
            }
            is AuthUiState.Error -> {
                errorMessage = (authState as AuthUiState.Error).message
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
            text = "Tạo Tài Khoản",
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
            value = displayName.value,
            onValueChange = { displayName.value = it },
            label = { Text("Tên hiển thị") },
            modifier = Modifier.fillMaxWidth()
        )

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
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmPassword.value,
            onValueChange = { confirmPassword.value = it },
            label = { Text("Nhập lại mật khẩu") },
            visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible.value)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Checkbox(checked = acceptedTerms.value, onCheckedChange = { acceptedTerms.value = it })
            Text(text = "Tôi đồng ý với Điều khoản & Chính sách bảo mật", fontSize = 14.sp)
        }

        Button(
            onClick = {
                showError = false
                if (displayName.value.isBlank() || email.value.isBlank() || password.value.isBlank()) {
                    errorMessage = "Vui lòng điền đầy đủ thông tin"
                    showError = true
                    return@Button
                }

                if (password.value != confirmPassword.value) {
                    errorMessage = "Mật khẩu không khớp"
                    showError = true
                    return@Button
                }

                if (!acceptedTerms.value) {
                    errorMessage = "Vui lòng đồng ý với điều khoản"
                    showError = true
                    return@Button
                }

                authViewModel.signUpWithEmail(email.value, password.value, displayName.value)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = acceptedTerms.value,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            if (authState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text(text = "Đăng ký bằng Email", color = Color.White)
            }
        }

        Text(text = "hoặc", modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

        OutlinedButton(
            onClick = { /* Handle Google sign-in */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đăng nhập bằng Google")
        }

        OutlinedButton(
            onClick = { /* Handle Facebook sign-in */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.AccountBox, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đăng nhập bằng Facebook")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đã có tài khoản?")
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Đăng nhập",
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onLoginClick)
            )
        }
    }
}