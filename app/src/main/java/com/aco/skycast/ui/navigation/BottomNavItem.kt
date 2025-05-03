package com.aco.skycast.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aco.skycast.R

enum class BottomNavItem(
    val route: String,
    @StringRes val title: Int,
    @DrawableRes val icon: Int
) {
    Home("home", R.string.home, R.drawable.ic_home),
    Search("search", R.string.search, R.drawable.ic_search),
    ChatBot("chatbot", R.string.chatbot, R.drawable.ic_chat),
    Tomorrow("tomorrow", R.string.tomorrow, R.drawable.ic_tomorrow),
    SevenDay("sevenday", R.string.sevenday, R.drawable.ic_calendar),
    UserSettings("settings", R.string.settings, R.drawable.ic_settings)
}