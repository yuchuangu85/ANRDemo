package com.codemx.anrdemo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 验证底部导航能在 ANR 与 性能 两个 Tab 间切换。需在设备/模拟器上运行。 */
@RunWith(AndroidJUnit4::class)
class PerfNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun switchingToPerfTabShowsPerfScreen() {
        // 默认在 ANR 页
        composeRule.onNodeWithText("ANR Demo").assertIsDisplayed()

        // 点击底部「性能」Tab
        composeRule.onNodeWithText("性能").performClick()

        // 性能页标题出现
        composeRule.onNodeWithText("性能测试").assertIsDisplayed()
        composeRule.onNodeWithText("实时指标 (JankStats)").assertIsDisplayed()
    }
}
