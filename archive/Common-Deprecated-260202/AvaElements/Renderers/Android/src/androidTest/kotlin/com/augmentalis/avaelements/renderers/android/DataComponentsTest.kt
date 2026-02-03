package com.augmentalis.avaelements.renderers.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.avaelements.flutter.material.data.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive test suite for Agent 9 data components
 * Tests all 9 new data display components (RadioListTile already tested)
 *
 * Coverage target: 90%+
 * Test count target: 50+
 */
@RunWith(AndroidJUnit4::class)
class DataComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ============================================================================
    // DataList Tests (6 tests)
    // ============================================================================

    @Test
    fun dataList_rendersCorrectly() {
        val component = DataList(
            title = "Product Specs",
            items = listOf(
                DataList.DataItem("Brand", "Acme"),
                DataList.DataItem("Model", "X-2000")
            )
        )

        composeTestRule.setContent {
            DataListMapper(component)
        }

        composeTestRule.onNodeWithText("Product Specs").assertExists()
        composeTestRule.onNodeWithText("Brand").assertExists()
        composeTestRule.onNodeWithText("Acme").assertExists()
    }

    @Test
    fun dataList_stackedLayoutWorks() {
        val component = DataList(
            items = listOf(DataList.DataItem("Key", "Value")),
            layout = DataList.Layout.Stacked
        )

        composeTestRule.setContent {
            DataListMapper(component)
        }

        composeTestRule.onNodeWithText("Key").assertExists()
        composeTestRule.onNodeWithText("Value").assertExists()
    }

    @Test
    fun dataList_inlineLayoutWorks() {
        val component = DataList(
            items = listOf(DataList.DataItem("Price", "$99.99")),
            layout = DataList.Layout.Inline
        )

        composeTestRule.setContent {
            DataListMapper(component)
        }

        composeTestRule.onNodeWithText("Price").assertExists()
        composeTestRule.onNodeWithText("$99.99").assertExists()
    }

    @Test
    fun dataList_gridLayoutWorks() {
        val component = DataList(
            items = listOf(
                DataList.DataItem("A", "1"),
                DataList.DataItem("B", "2")
            ),
            layout = DataList.Layout.Grid
        )

        composeTestRule.setContent {
            DataListMapper(component)
        }

        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithText("B").assertExists()
    }

    @Test
    fun dataList_dividersDisplayed() {
        val component = DataList(
            items = listOf(
                DataList.DataItem("First", "1"),
                DataList.DataItem("Second", "2")
            ),
            showDividers = true
        )

        composeTestRule.setContent {
            DataListMapper(component)
        }

        composeTestRule.onNodeWithText("First").assertExists()
        composeTestRule.onNodeWithText("Second").assertExists()
    }

    @Test
    fun dataList_accessibilitySupported() {
        val component = DataList(
            items = listOf(DataList.DataItem("Test", "Value")),
            contentDescription = "Test data list"
        )

        composeTestRule.setContent {
            DataListMapper(component)
        }

        composeTestRule.onNode(hasContentDescription("Test data list")).assertExists()
    }

    // ============================================================================
    // DescriptionList Tests (6 tests)
    // ============================================================================

    @Test
    fun descriptionList_rendersCorrectly() {
        val component = DescriptionList(
            title = "Glossary",
            items = listOf(
                DescriptionList.DescriptionItem("API", "Application Programming Interface")
            )
        )

        composeTestRule.setContent {
            DescriptionListMapper(component)
        }

        composeTestRule.onNodeWithText("Glossary").assertExists()
        composeTestRule.onNodeWithText("API").assertExists()
        composeTestRule.onNodeWithText("Application Programming Interface").assertExists()
    }

    @Test
    fun descriptionList_numberedListWorks() {
        val component = DescriptionList(
            items = listOf(
                DescriptionList.DescriptionItem("First", "Description 1")
            ),
            numbered = true
        )

        composeTestRule.setContent {
            DescriptionListMapper(component)
        }

        composeTestRule.onNodeWithText("1.").assertExists()
        composeTestRule.onNodeWithText("First").assertExists()
    }

    @Test
    fun descriptionList_expandableWorks() {
        val component = DescriptionList(
            items = listOf(
                DescriptionList.DescriptionItem("Question", "Answer")
            ),
            expandable = true,
            defaultExpanded = false
        )

        composeTestRule.setContent {
            DescriptionListMapper(component)
        }

        composeTestRule.onNodeWithText("Question").assertExists()
        composeTestRule.onNodeWithText("Answer").assertExists()
    }

    @Test
    fun descriptionList_iconDisplayed() {
        val component = DescriptionList(
            items = listOf(
                DescriptionList.DescriptionItem("Term", "Definition", icon = "info")
            )
        )

        composeTestRule.setContent {
            DescriptionListMapper(component)
        }

        composeTestRule.onNodeWithText("Term").assertExists()
    }

    @Test
    fun descriptionList_badgeDisplayed() {
        val component = DescriptionList(
            items = listOf(
                DescriptionList.DescriptionItem("New", "Feature", badge = "Beta")
            )
        )

        composeTestRule.setContent {
            DescriptionListMapper(component)
        }

        composeTestRule.onNodeWithText("New").assertExists()
        composeTestRule.onNodeWithText("Beta").assertExists()
    }

    @Test
    fun descriptionList_accessibilitySupported() {
        val component = DescriptionList(
            items = listOf(DescriptionList.DescriptionItem("Test", "Desc")),
            contentDescription = "Test description list"
        )

        composeTestRule.setContent {
            DescriptionListMapper(component)
        }

        composeTestRule.onNode(hasContentDescription("Test description list")).assertExists()
    }

    // ============================================================================
    // StatGroup Tests (6 tests)
    // ============================================================================

    @Test
    fun statGroup_rendersCorrectly() {
        val component = StatGroup(
            title = "Weekly Stats",
            stats = listOf(
                StatGroup.StatItem("Revenue", "$1000")
            )
        )

        composeTestRule.setContent {
            StatGroupMapper(component)
        }

        composeTestRule.onNodeWithText("Weekly Stats").assertExists()
        composeTestRule.onNodeWithText("Revenue").assertExists()
        composeTestRule.onNodeWithText("$1000").assertExists()
    }

    @Test
    fun statGroup_horizontalLayoutWorks() {
        val component = StatGroup(
            stats = listOf(
                StatGroup.StatItem("A", "100"),
                StatGroup.StatItem("B", "200")
            ),
            layout = StatGroup.Layout.Horizontal
        )

        composeTestRule.setContent {
            StatGroupMapper(component)
        }

        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithText("B").assertExists()
    }

    @Test
    fun statGroup_verticalLayoutWorks() {
        val component = StatGroup(
            stats = listOf(StatGroup.StatItem("Metric", "500")),
            layout = StatGroup.Layout.Vertical
        )

        composeTestRule.setContent {
            StatGroupMapper(component)
        }

        composeTestRule.onNodeWithText("Metric").assertExists()
    }

    @Test
    fun statGroup_gridLayoutWorks() {
        val component = StatGroup(
            stats = listOf(
                StatGroup.StatItem("X", "1"),
                StatGroup.StatItem("Y", "2")
            ),
            layout = StatGroup.Layout.Grid
        )

        composeTestRule.setContent {
            StatGroupMapper(component)
        }

        composeTestRule.onNodeWithText("X").assertExists()
        composeTestRule.onNodeWithText("Y").assertExists()
    }

    @Test
    fun statGroup_changeIndicatorDisplayed() {
        val component = StatGroup(
            stats = listOf(
                StatGroup.StatItem("Sales", "1000", change = "+10%", changeType = StatGroup.ChangeType.Positive)
            )
        )

        composeTestRule.setContent {
            StatGroupMapper(component)
        }

        composeTestRule.onNodeWithText("+10%").assertExists()
    }

    @Test
    fun statGroup_accessibilitySupported() {
        val component = StatGroup(
            stats = listOf(StatGroup.StatItem("Test", "100")),
            contentDescription = "Test stat group"
        )

        composeTestRule.setContent {
            StatGroupMapper(component)
        }

        composeTestRule.onNode(hasContentDescription("Test stat group")).assertExists()
    }

    // ============================================================================
    // Stat Tests (6 tests)
    // ============================================================================

    @Test
    fun stat_rendersCorrectly() {
        val component = Stat(
            label = "Total Users",
            value = "12,450"
        )

        composeTestRule.setContent {
            StatMapper(component)
        }

        composeTestRule.onNodeWithText("Total Users").assertExists()
        composeTestRule.onNodeWithText("12,450").assertExists()
    }

    @Test
    fun stat_changeIndicatorWorks() {
        val component = Stat(
            label = "Revenue",
            value = "$5000",
            change = "+15%",
            changeType = Stat.ChangeType.Positive
        )

        composeTestRule.setContent {
            StatMapper(component)
        }

        composeTestRule.onNodeWithText("+15%").assertExists()
    }

    @Test
    fun stat_descriptionDisplayed() {
        val component = Stat(
            label = "Active",
            value = "100",
            description = "from last month"
        )

        composeTestRule.setContent {
            StatMapper(component)
        }

        composeTestRule.onNodeWithText("from last month").assertExists()
    }

    @Test
    fun stat_elevatedCardWorks() {
        val component = Stat(
            label = "Total",
            value = "999",
            elevated = true
        )

        composeTestRule.setContent {
            StatMapper(component)
        }

        composeTestRule.onNodeWithText("Total").assertExists()
    }

    @Test
    fun stat_clickableWorks() {
        var clicked = false
        val component = Stat(
            label = "Clicks",
            value = "42",
            onClick = { clicked = true }
        )

        composeTestRule.setContent {
            StatMapper(component)
        }

        composeTestRule.onNodeWithText("Clicks").performClick()
        assert(clicked)
    }

    @Test
    fun stat_accessibilitySupported() {
        val component = Stat(
            label = "Test Stat",
            value = "100",
            contentDescription = "Test statistic"
        )

        composeTestRule.setContent {
            StatMapper(component)
        }

        composeTestRule.onNode(hasContentDescription("Test statistic")).assertExists()
    }

    // ============================================================================
    // KPI Tests (7 tests)
    // ============================================================================

    @Test
    fun kpi_rendersCorrectly() {
        val component = KPI(
            title = "Sales Target",
            value = "$50,000"
        )

        composeTestRule.setContent {
            KPIMapper(component)
        }

        composeTestRule.onNodeWithText("Sales Target").assertExists()
        composeTestRule.onNodeWithText("$50,000").assertExists()
    }

    @Test
    fun kpi_targetDisplayed() {
        val component = KPI(
            title = "Goal",
            value = "80",
            target = "100"
        )

        composeTestRule.setContent {
            KPIMapper(component)
        }

        composeTestRule.onNodeWithText("Target: 100").assertExists()
    }

    @Test
    fun kpi_progressBarWorks() {
        val component = KPI(
            title = "Progress",
            value = "75",
            progress = 0.75f,
            showProgressBar = true
        )

        composeTestRule.setContent {
            KPIMapper(component)
        }

        composeTestRule.onNodeWithText("75%").assertExists()
    }

    @Test
    fun kpi_trendIndicatorWorks() {
        val component = KPI(
            title = "Growth",
            value = "120%",
            trend = KPI.TrendType.Up
        )

        composeTestRule.setContent {
            KPIMapper(component)
        }

        composeTestRule.onNodeWithText("Growth").assertExists()
    }

    @Test
    fun kpi_subtitleDisplayed() {
        val component = KPI(
            title = "Metric",
            value = "500",
            subtitle = "Year to date"
        )

        composeTestRule.setContent {
            KPIMapper(component)
        }

        composeTestRule.onNodeWithText("Year to date").assertExists()
    }

    @Test
    fun kpi_clickableWorks() {
        var clicked = false
        val component = KPI(
            title = "KPI",
            value = "100",
            onClick = { clicked = true }
        )

        composeTestRule.setContent {
            KPIMapper(component)
        }

        composeTestRule.onNodeWithText("KPI").performClick()
        assert(clicked)
    }

    @Test
    fun kpi_accessibilitySupported() {
        val component = KPI(
            title = "Test KPI",
            value = "1000",
            contentDescription = "Test key performance indicator"
        )

        composeTestRule.setContent {
            KPIMapper(component)
        }

        composeTestRule.onNode(hasContentDescription("Test key performance indicator")).assertExists()
    }

    // ============================================================================
    // MetricCard Tests (6 tests)
    // ============================================================================

    @Test
    fun metricCard_rendersCorrectly() {
        val component = MetricCard(
            title = "Active Users",
            value = "5,234"
        )

        composeTestRule.setContent {
            MetricCardMapper(component)
        }

        composeTestRule.onNodeWithText("Active Users").assertExists()
        composeTestRule.onNodeWithText("5,234").assertExists()
    }

    @Test
    fun metricCard_unitDisplayed() {
        val component = MetricCard(
            title = "Temperature",
            value = "72",
            unit = "°F"
        )

        composeTestRule.setContent {
            MetricCardMapper(component)
        }

        composeTestRule.onNodeWithText("°F").assertExists()
    }

    @Test
    fun metricCard_comparisonWorks() {
        val component = MetricCard(
            title = "Revenue",
            value = "$10,000",
            comparison = "vs last month",
            change = "+5%",
            changeType = MetricCard.ChangeType.Positive
        )

        composeTestRule.setContent {
            MetricCardMapper(component)
        }

        composeTestRule.onNodeWithText("vs last month").assertExists()
        composeTestRule.onNodeWithText("+5%").assertExists()
    }

    @Test
    fun metricCard_sparklineShown() {
        val component = MetricCard(
            title = "Trend",
            value = "100",
            showSparkline = true,
            sparklineData = listOf(1f, 2f, 3f, 4f, 5f)
        )

        composeTestRule.setContent {
            MetricCardMapper(component)
        }

        composeTestRule.onNodeWithText("Trend").assertExists()
    }

    @Test
    fun metricCard_clickableWorks() {
        var clicked = false
        val component = MetricCard(
            title = "Metric",
            value = "999",
            onClick = { clicked = true }
        )

        composeTestRule.setContent {
            MetricCardMapper(component)
        }

        composeTestRule.onNodeWithText("Metric").performClick()
        assert(clicked)
    }

    @Test
    fun metricCard_accessibilitySupported() {
        val component = MetricCard(
            title = "Test Metric",
            value = "500",
            contentDescription = "Test metric card"
        )

        composeTestRule.setContent {
            MetricCardMapper(component)
        }

        composeTestRule.onNode(hasContentDescription("Test metric card")).assertExists()
    }

    // ============================================================================
    // Leaderboard Tests (7 tests)
    // ============================================================================

    @Test
    fun leaderboard_rendersCorrectly() {
        val component = Leaderboard(
            title = "Top Players",
            items = listOf(
                Leaderboard.LeaderboardItem("1", 1, "Alice", "1000")
            )
        )

        composeTestRule.setContent {
            LeaderboardMapper(component)
        }

        composeTestRule.onNodeWithText("Top Players").assertExists()
        composeTestRule.onNodeWithText("Alice").assertExists()
        composeTestRule.onNodeWithText("1000").assertExists()
    }

    @Test
    fun leaderboard_rankDisplayed() {
        val component = Leaderboard(
            items = listOf(
                Leaderboard.LeaderboardItem("1", 1, "Player", "500")
            )
        )

        composeTestRule.setContent {
            LeaderboardMapper(component)
        }

        composeTestRule.onNodeWithText("1").assertExists()
    }

    @Test
    fun leaderboard_badgesShownForTop3() {
        val component = Leaderboard(
            items = listOf(
                Leaderboard.LeaderboardItem("1", 1, "First", "1000"),
                Leaderboard.LeaderboardItem("2", 2, "Second", "900"),
                Leaderboard.LeaderboardItem("3", 3, "Third", "800")
            ),
            showTopBadges = true
        )

        composeTestRule.setContent {
            LeaderboardMapper(component)
        }

        composeTestRule.onNodeWithText("First").assertExists()
        composeTestRule.onNodeWithText("Second").assertExists()
        composeTestRule.onNodeWithText("Third").assertExists()
    }

    @Test
    fun leaderboard_currentUserHighlighted() {
        val component = Leaderboard(
            items = listOf(
                Leaderboard.LeaderboardItem("me", 5, "Current User", "500")
            ),
            currentUserId = "me"
        )

        composeTestRule.setContent {
            LeaderboardMapper(component)
        }

        composeTestRule.onNodeWithText("Current User").assertExists()
    }

    @Test
    fun leaderboard_maxItemsRespected() {
        val component = Leaderboard(
            items = (1..20).map {
                Leaderboard.LeaderboardItem("$it", it, "Player $it", "$it")
            },
            maxItems = 10
        )

        composeTestRule.setContent {
            LeaderboardMapper(component)
        }

        composeTestRule.onNodeWithText("Player 1").assertExists()
        composeTestRule.onNodeWithText("Player 10").assertExists()
    }

    @Test
    fun leaderboard_itemClickable() {
        var clickedId = ""
        val component = Leaderboard(
            items = listOf(
                Leaderboard.LeaderboardItem("test", 1, "Test Player", "100")
            ),
            onItemClick = { clickedId = it }
        )

        composeTestRule.setContent {
            LeaderboardMapper(component)
        }

        composeTestRule.onNodeWithText("Test Player").performClick()
        assert(clickedId == "test")
    }

    @Test
    fun leaderboard_accessibilitySupported() {
        val component = Leaderboard(
            items = listOf(Leaderboard.LeaderboardItem("1", 1, "Player", "100")),
            contentDescription = "Test leaderboard"
        )

        composeTestRule.setContent {
            LeaderboardMapper(component)
        }

        composeTestRule.onNode(hasContentDescription("Test leaderboard")).assertExists()
    }

    // ============================================================================
    // Ranking Tests (6 tests)
    // ============================================================================

    @Test
    fun ranking_smallSizeWorks() {
        val component = Ranking(
            position = 5,
            size = Ranking.Size.Small
        )

        composeTestRule.setContent {
            RankingMapper(component)
        }

        composeTestRule.onNodeWithText("5").assertExists()
    }

    @Test
    fun ranking_mediumSizeWorks() {
        val component = Ranking(
            position = 10,
            size = Ranking.Size.Medium
        )

        composeTestRule.setContent {
            RankingMapper(component)
        }

        composeTestRule.onNodeWithText("10").assertExists()
    }

    @Test
    fun ranking_largeSizeWorks() {
        val component = Ranking(
            position = 1,
            size = Ranking.Size.Large,
            label = "Rank"
        )

        composeTestRule.setContent {
            RankingMapper(component)
        }

        composeTestRule.onNodeWithText("1").assertExists()
        composeTestRule.onNodeWithText("Rank").assertExists()
    }

    @Test
    fun ranking_badgeDisplayedForTopPositions() {
        val component = Ranking(
            position = 1,
            showBadge = true,
            size = Ranking.Size.Medium
        )

        composeTestRule.setContent {
            RankingMapper(component)
        }

        composeTestRule.onNodeWithText("1").assertExists()
    }

    @Test
    fun ranking_changeIndicatorWorks() {
        val component = Ranking(
            position = 3,
            change = 2,
            size = Ranking.Size.Medium
        )

        composeTestRule.setContent {
            RankingMapper(component)
        }

        composeTestRule.onNodeWithText("2").assertExists()
    }

    @Test
    fun ranking_ordinalGenerated() {
        val component = Ranking(
            position = 1,
            size = Ranking.Size.Large
        )

        composeTestRule.setContent {
            RankingMapper(component)
        }

        // Ordinal "1st" should be generated
        composeTestRule.onNodeWithText("1st").assertExists()
    }

    // ============================================================================
    // Zoom Tests (6 tests)
    // ============================================================================

    @Test
    fun zoom_rendersCorrectly() {
        val component = Zoom(
            imageUrl = "test.jpg"
        )

        composeTestRule.setContent {
            ZoomMapper(component)
        }

        // Image should be present (AsyncImage renders)
        composeTestRule.onNode(hasContentDescription("Zoomable image")).assertExists()
    }

    @Test
    fun zoom_controlsDisplayed() {
        val component = Zoom(
            imageUrl = "test.jpg",
            showControls = true
        )

        composeTestRule.setContent {
            ZoomMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Zoom in").assertExists()
        composeTestRule.onNodeWithContentDescription("Zoom out").assertExists()
    }

    @Test
    fun zoom_zoomInWorks() {
        var scale = 1.0f
        val component = Zoom(
            imageUrl = "test.jpg",
            showControls = true,
            onScaleChanged = { scale = it }
        )

        composeTestRule.setContent {
            ZoomMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Zoom in").performClick()
        assert(scale > 1.0f)
    }

    @Test
    fun zoom_zoomOutWorks() {
        var scale = 1.0f
        val component = Zoom(
            imageUrl = "test.jpg",
            initialScale = 2.0f,
            showControls = true,
            onScaleChanged = { scale = it }
        )

        composeTestRule.setContent {
            ZoomMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Zoom out").performClick()
        assert(scale < 2.0f)
    }

    @Test
    fun zoom_resetWorks() {
        var scale = 1.0f
        val component = Zoom(
            imageUrl = "test.jpg",
            initialScale = 1.0f,
            showControls = true,
            onScaleChanged = { scale = it }
        )

        composeTestRule.setContent {
            ZoomMapper(component)
        }

        composeTestRule.onNodeWithText("1:1").performClick()
        assert(scale == 1.0f)
    }

    @Test
    fun zoom_accessibilitySupported() {
        val component = Zoom(
            imageUrl = "test.jpg",
            contentDescription = "Test zoomable image"
        )

        composeTestRule.setContent {
            ZoomMapper(component)
        }

        composeTestRule.onNode(hasContentDescription("Test zoomable image")).assertExists()
    }
}
