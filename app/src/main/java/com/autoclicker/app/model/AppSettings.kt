package com.autoclicker.app.model

data class AppSettings(
    val clickMode: ClickMode = ClickMode.FIXED,
    val fixedDelay: Long = 1000L,           // ms - chế độ 1
    val randomDelayMin: Long = 100L,        // ms - chế độ 2
    val randomDelayMax: Long = 1000L,       // ms - chế độ 2
    val clickX: Float = 500f,
    val clickY: Float = 1000f,
    val swipePoints: List<SwipePoint> = emptyList(),
    val isSwipeMode: Boolean = false,
    val showTouchIndicator: Boolean = true
)
