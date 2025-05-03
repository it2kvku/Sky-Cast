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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aco.skycast.data.model.CitySearchData
import com.aco.skycast.data.model.WeatherViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable


private val BlueSolidColor = Color(0xFF1976D2)

@Composable
fun SearchScreen(viewModel: WeatherViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var recentSearches by remember { mutableStateOf(listOf<String>()) }
    var searchResults by remember { mutableStateOf<List<CitySearchData>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { newQuery ->
                searchQuery = newQuery
                if (newQuery.isNotBlank()) {
                    isSearching = true
                    // Mock search results for demonstration
                    searchResults = listOf(
                        CitySearchData("New York", "USA", 40.7128, -74.0060, 25.0),
                        CitySearchData("London", "UK", 51.5074, -0.1278, 18.0),
                        CitySearchData("Paris", "France", 48.8566, 2.3522, 28.0)
                    )
                } else {
                    searchResults = emptyList()
                    isSearching = false
                }
            },
            onClearQuery = {
                searchQuery = ""
                searchResults = emptyList()
                isSearching = false
            },
            onBackPressed = { /* Handle back press if needed */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = searchQuery.isEmpty()) {
            Column {
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (recentSearches.isEmpty()) {
                    Text(
                        text = "No recent searches",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    recentSearches.forEach { cityName ->
                        RecentSearchItem(cityName = cityName) {
                            searchQuery = cityName
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = isSearching && searchResults.isNotEmpty()) {
            LazyColumn {
                items(searchResults) { cityResult ->
                    SearchResultItem(cityResult = cityResult) {
                        scope.launch {
                            viewModel.fetchWeather(cityResult.name)
                            recentSearches = (listOf(cityResult.name) + recentSearches).distinct().take(5)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = isSearching && searchResults.isEmpty() && searchQuery.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No results found for '$searchQuery'",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
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
        color = Color.LightGray.copy(alpha = 0.2f),
        shadowElevation = 2.dp
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
                    tint = Color.DarkGray
                )
            }

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search for a city...") },
                modifier = Modifier.weight(1f),
                singleLine = true,

            )

            if (query.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color.DarkGray
                    )
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun RecentSearchItem(cityName: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cityName,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SearchResultItem(cityResult: CitySearchData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BlueSolidColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${cityResult.name}, ${cityResult.country}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${cityResult.temperature.toInt()}°",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class CitySearchData(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val temperature: Double = 0.0
)

