package com.aco.skycast.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aco.skycast.data.model.AuthViewModel
import com.aco.skycast.data.model.PreferencesViewModel
import com.aco.skycast.utils.WeatherUtils
import kotlinx.coroutines.delay

@Composable
fun UserScreen(authViewModel: AuthViewModel, onSignOut: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WeatherUtils.LightBackground)
            .verticalScroll(scrollState)
    ) {
        // User header section
        UserProfileHeader(authViewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Weather preferences section
        WeatherAlertPreferences()

        Spacer(modifier = Modifier.height(16.dp))

        // App information section
        SettingsSection(
            title = "App Information",
            items = listOf(
                SettingsItem("About", Icons.Outlined.Info) {},
                SettingsItem("Help & Support", Icons.Outlined.Support) {},
                SettingsItem("Privacy Policy", Icons.Outlined.PrivacyTip) {}
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign out button
        SignOutButton(onSignOut = onSignOut)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun UserProfileHeader(authViewModel: AuthViewModel) {
    val currentUser = authViewModel.getCurrentUser()
    var showDialog by remember { mutableStateOf(false) }
    var newDisplayName by remember { mutableStateOf("") }

    // Show dialog if state is true
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edit Profile") },
            text = {
                TextField(
                    value = newDisplayName,
                    onValueChange = { newDisplayName = it },
                    label = { Text("New Username") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    authViewModel.updateDisplayName(newDisplayName)
                    showDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WeatherUtils.BlueSolidColor,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentUser?.displayName ?: "Guest User",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = currentUser?.email ?: "No email available",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    // Initialize with current name if available
                    newDisplayName = currentUser?.displayName ?: ""
                    showDialog = true
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(36.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Text("Edit Profile", fontSize = 14.sp)
            }
        }
    }
}
@Composable
fun SettingsSection(
    title: String,
    items: List<SettingsItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = WeatherUtils.TemperatureHighColor,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = WeatherUtils.CardBackgroundColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsItemRow(item)
                    if (index < items.size - 1) {
                        Divider(
                            color = WeatherUtils.DividerColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItemRow(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = WeatherUtils.BlueSolidColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.title,
            fontSize = 16.sp,
            color = WeatherUtils.TemperatureHighColor,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = WeatherUtils.MetricsTextColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun WeatherAlertPreferences(viewModel: PreferencesViewModel = viewModel()) {
    val severeAlertsChecked by viewModel.severeAlerts.collectAsState()
    val dailyForecastChecked by viewModel.dailyForecast.collectAsState()
    val precipitationChecked by viewModel.precipitationAlerts.collectAsState()
    var showFeedback by remember { mutableStateOf<String?>(null) }

    // Show feedback if needed
    showFeedback?.let { message ->
        LaunchedEffect(message) {
            delay(2000)
            showFeedback = null
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showFeedback = null }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(message)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Weather Alerts",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = WeatherUtils.TemperatureHighColor,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = WeatherUtils.CardBackgroundColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                PreferenceToggleRowWithTest(
                    title = "Severe Weather Alerts",
                    checked = severeAlertsChecked,
                    onCheckedChange = {
                        viewModel.updateSevereAlerts(it)
                        showFeedback = "Severe weather alerts ${if(it) "enabled" else "disabled"}"
                    },
                    onTest = {
                        viewModel.testSevereWeatherAlert()
                        showFeedback = "Test notification sent"
                    },
                    enabled = severeAlertsChecked
                )

                Divider(
                    color = WeatherUtils.DividerColor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                PreferenceToggleRowWithTest(
                    title = "Daily Forecast Notifications",
                    checked = dailyForecastChecked,
                    onCheckedChange = {
                        viewModel.updateDailyForecast(it)
                        showFeedback = "Daily forecast ${if(it) "enabled" else "disabled"}"
                    },
                    onTest = {
                        viewModel.testDailyForecastAlert()
                        showFeedback = "Test notification sent"
                    },
                    enabled = dailyForecastChecked
                )

                Divider(
                    color = WeatherUtils.DividerColor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                PreferenceToggleRowWithTest(
                    title = "Precipitation Alerts",
                    checked = precipitationChecked,
                    onCheckedChange = {
                        viewModel.updatePrecipitationAlerts(it)
                        showFeedback = "Precipitation alerts ${if(it) "enabled" else "disabled"}"
                    },
                    onTest = {
                        viewModel.testPrecipitationAlert()
                        showFeedback = "Test notification sent"
                    },
                    enabled = precipitationChecked
                )
            }
        }
    }
}

@Composable
fun PreferenceToggleRowWithTest(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTest: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = WeatherUtils.TemperatureHighColor,
            modifier = Modifier.weight(1f)
        )

        OutlinedButton(
            onClick = onTest,
            enabled = enabled,
            modifier = Modifier
                .padding(end = 8.dp)
                .height(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = WeatherUtils.BlueSolidColor
            ),
            border = BorderStroke(1.dp, WeatherUtils.BlueSolidColor)
        ) {
            Text("Test", fontSize = 12.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WeatherUtils.BlueSolidColor,
                checkedTrackColor = WeatherUtils.HumidityIconColor,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
fun PreferenceToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = WeatherUtils.TemperatureHighColor,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WeatherUtils.BlueSolidColor,
                checkedTrackColor = WeatherUtils.HumidityIconColor,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
fun SignOutButton(onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Sign Out",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class SettingsItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
