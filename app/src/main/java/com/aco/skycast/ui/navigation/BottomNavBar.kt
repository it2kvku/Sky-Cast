package com.aco.skycast.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(
    navController: NavHostController,
    items: List<BottomNavItem> = BottomNavItem.values().toList()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val navBarColor = Color(0xFF7CC1F0)

    // Track hovered item for swipe effect
    var hoveredItemIndex by remember { mutableIntStateOf(-1) }

    // Store item positions
    val itemPositions = remember { mutableListOf<Float>().apply { addAll(List(items.size) { 0f }) } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        // Main background surface with the blue color
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = navBarColor.copy(alpha = 0.8f), // Increased opacity
            shadowElevation = 8.dp,
            tonalElevation = 0.dp, // Reduced to prevent darkening
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            // Empty box to fill the surface
            Box(modifier = Modifier.fillMaxWidth())
        }

        // Nav items row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val touchX = offset.x
                            val itemWidth = size.width / items.size
                            hoveredItemIndex = (touchX / itemWidth).toInt().coerceIn(0, items.size - 1)
                        },
                        onDragEnd = { hoveredItemIndex = -1 },
                        onDragCancel = { hoveredItemIndex = -1 },
                        onHorizontalDrag = { change, _ ->
                            val touchX = change.position.x
                            val itemWidth = size.width / items.size
                            hoveredItemIndex = (touchX / itemWidth).toInt().coerceIn(0, items.size - 1)
                        }
                    )
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = currentRoute == item.route
                val isHovered = index == hoveredItemIndex

                // Animations for smooth transitions
                val scale by animateFloatAsState(
                    targetValue = when {
                        selected -> 1.2f
                        isHovered -> 1.1f
                        else -> 1f
                    },
                    animationSpec = tween(durationMillis = 300)
                )

                val iconSize by animateDpAsState(
                    targetValue = if (selected) 26.dp else 22.dp,
                    animationSpec = tween(durationMillis = 300)
                )

                // Using darker colors for better contrast against light blue
                val itemColor by animateColorAsState(
                    targetValue = when {
                        selected -> Color.White
                        isHovered -> Color.White.copy(alpha = 0.9f)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    animationSpec = tween(durationMillis = 300)
                )

                val bgAlpha by animateFloatAsState(
                    targetValue = when {
                        selected -> 0.3f
                        isHovered -> 0.2f
                        else -> 0f
                    },
                    animationSpec = tween(durationMillis = 300)
                )

                // Item container with background
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray.copy(alpha = bgAlpha)) // Darker background for better contrast
                        .padding(vertical = 6.dp)
                        .onGloballyPositioned { coordinates ->
                            if (index < itemPositions.size) {
                                itemPositions[index] = coordinates.positionInParent().x
                            }
                        }
                        .clickable(
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                ) {
                    // Nav item content
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Icon with badge if needed
                        Box(contentAlignment = Alignment.Center) {
                            if (item == BottomNavItem.ChatBot) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = Color.Red)
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = item.icon),
                                        contentDescription = stringResource(id = item.title),
                                        modifier = Modifier
                                            .size(iconSize)
                                            .scale(scale),
                                        tint = itemColor
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(id = item.icon),
                                    contentDescription = stringResource(id = item.title),
                                    modifier = Modifier
                                        .size(iconSize)
                                        .scale(scale),
                                    tint = itemColor
                                )
                            }
                        }

                        // Label text
                        Text(
                            text = stringResource(id = item.title),
                            color = itemColor,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}