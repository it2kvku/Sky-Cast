package com.aco.skycast.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aco.skycast.data.model.CitySearchData
import com.aco.skycast.data.model.WeatherViewModel
import com.aco.skycast.ui.navigation.BottomNavItem
import com.aco.skycast.utils.WeatherUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    navController: NavController? = null
) {
    val searchQuery = remember { mutableStateOf("") }
    val recentSearches = remember { mutableStateOf(listOf<String>()) }
    val isSearching = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isSearchLoading.collectAsState()
    val ipCity by viewModel.ipCity.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WeatherUtils.BackgroundColor)
            .padding(16.dp)
    ) {
        SearchBar(
            query = searchQuery.value,
            onQueryChange = { newQuery ->
                searchQuery.value = newQuery
                if (newQuery.length >= 2) {
                    isSearching.value = true
                    scope.launch {
                        delay(300) // Debounce
                        viewModel.searchCities(newQuery)
                    }
                } else {
                    isSearching.value = false
                }
            },
            onClearQuery = {
                searchQuery.value = ""
                isSearching.value = false
            },
            onBackPressed = {
                navController?.navigate(BottomNavItem.Home.route) {
                    popUpTo(BottomNavItem.Home.route) { inclusive = true }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = !isSearching.value) {
            Column {
                // Current location section
                if (ipCity.isNotEmpty()) {
                    CurrentLocationItem(cityName = ipCity) {
                        scope.launch {
                            viewModel.fetchWeather(ipCity)
                            navController?.navigate(BottomNavItem.Home.route)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Recent searches section
                RecentSearchesList(
                    recentSearches = recentSearches.value,
                    onItemClick = { cityName ->
                        scope.launch {
                            viewModel.fetchWeather(cityName)
                            navController?.navigate(BottomNavItem.Home.route)
                        }
                    }
                )
            }
        }

        if (isLoading && isSearching.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WeatherUtils.HumidityIconColor)
            }
        }

        AnimatedVisibility(visible = isSearching.value && !isLoading && searchResults.isNotEmpty()) {
            SearchResultsList(
                searchResults = searchResults,
                onResultClick = { cityResult ->
                    scope.launch {
                        viewModel.fetchWeather(cityResult.name)
                        // Add to recent searches
                        recentSearches.value = (listOf(cityResult.name) + recentSearches.value)
                            .distinct().take(5)
                        // Navigate back to home
                        navController?.navigate(BottomNavItem.Home.route)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = isSearching.value && !isLoading &&
                    searchResults.isEmpty() && searchQuery.value.isNotEmpty()
        ) {
            NoResultsMessage(searchQuery = searchQuery.value)
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBackPressed: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = WeatherUtils.CardBackgroundColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = WeatherUtils.MetricsTextColor
                )
            }

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text("Search for a city...",
                        color = WeatherUtils.MetricsTextColor.copy(alpha = 0.6f))
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = WeatherUtils.HumidityIconColor
                )
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = WeatherUtils.MetricsTextColor
                    )
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = WeatherUtils.HumidityIconColor
                    )
                }
            }
        }
    }
}

@Composable
fun CurrentLocationItem(cityName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WeatherUtils.HumidityIconColor.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Current Location",
                tint = WeatherUtils.HumidityIconColor,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = "Current Location",
                    fontSize = 14.sp,
                    color = WeatherUtils.MetricsTextColor
                )
                Text(
                    text = cityName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun RecentSearchesList(recentSearches: List<String>, onItemClick: (String) -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (recentSearches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent searches",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WeatherUtils.CardBackgroundColor)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    recentSearches.forEach { cityName ->
                        RecentSearchItem(cityName = cityName) {
                            onItemClick(cityName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSearchItem(cityName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = WeatherUtils.MetricsTextColor.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = cityName,
            fontSize = 16.sp,
            color = WeatherUtils.MetricsTextColor,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun SearchResultsList(searchResults: List<CitySearchData>, onResultClick: (CitySearchData) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(searchResults) { cityResult ->
            SearchResultItem(cityResult = cityResult) {
                onResultClick(cityResult)
            }
        }
    }
}

@Composable
fun SearchResultItem(cityResult: CitySearchData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WeatherUtils.CardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cityResult.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cityResult.country,
                    color = WeatherUtils.MetricsTextColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(WeatherUtils.HumidityIconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${cityResult.temperature.toInt()}°",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun NoResultsMessage(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No results found for",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "'$searchQuery'",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}