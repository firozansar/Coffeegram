@file:Suppress("ModifierMissing")

package ru.beryukhov.coffeegram.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import ru.beryukhov.coffeegram.data.CoffeeType
import ru.beryukhov.coffeegram.data.DayCoffee
import ru.beryukhov.coffeegram.model.DaysCoffeesState
import ru.beryukhov.date_time_utils.YearMonth

@Composable
fun CoffeeCharts(coffeeState: DaysCoffeesState, modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Weekly", "All Time")

    Column(modifier = modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            0 -> WeeklyCoffeeChart(coffeeState)
            1 -> AllTimeCoffeeChart(coffeeState)
        }
    }
}

@Composable
fun WeeklyCoffeeChart(coffeeState: DaysCoffeesState) {
    // Get dates for the current week (Monday to Sunday)
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val startOfWeek = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))

    // Filter data for current week and aggregate by day
    val weekData = weeklyChartData(startOfWeek, coffeeState)

    // Create chart entries
    val chartEntryModelProducer = ChartEntryModelProducer(floatEntries(weekData))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Coffee Consumption This Week",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Vico chart style
        ProvideChartStyle(rememberChartStyle()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Chart(
                    chart = columnChart(),
                    model = chartEntryModelProducer.getModel(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _ ->
                            weekData.getOrNull(value.toInt())?.dayName ?: ""
                        }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

internal fun weeklyChartData(
    startOfWeek: LocalDate,
    coffeeState: DaysCoffeesState
) = (0..6).map { dayOffset ->
    val date = startOfWeek.plus(DatePeriod(days = dayOffset))
    val dayCoffee = coffeeState.value[date] ?: DayCoffee()
    val totalForDay = dayCoffee.coffeeCountMap.values.sum()

    WeeklyChartData(
        date = date,
        dayName = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        },
        totalCoffees = totalForDay,
    )
}

internal fun floatEntries(weekData: List<WeeklyChartData>): List<FloatEntry> =
    weekData.mapIndexed { index, data ->
        entryOf(index.toFloat(), data.totalCoffees.toFloat())
    }

@Composable
fun AllTimeCoffeeChart(coffeeState: DaysCoffeesState) {
    // Only process if we have data
    if (coffeeState.value.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("No data available")
        }
        return
    }

    // Get date range for all available data
    val dates = coffeeState.value.keys
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val minDate = dates.minOrNull() ?: today
    val maxDate = dates.maxOrNull() ?: today

    // Calculate day difference
    val daysBetween = (maxDate - minDate).days

    // Prepare data
    val useMonthlyAggregation = daysBetween > 31

    // Aggregate data
    val aggregatedData = if (useMonthlyAggregation) {
        monthlyAggregation(coffeeState)
    } else {
        dailyAggregation(coffeeState)
    }

    // Create chart entries
    val chartEntryModelProducer = ChartEntryModelProducer(
        aggregatedData.mapIndexed { index: Int, data: AggregatedData ->
            entryOf(index.toFloat(), data.totalCount.toFloat())
        }
    )

    // Create coffee type distribution data
    val typeDistribution = CoffeeType.entries.map { coffeeType ->
        val total = coffeeState.value.values.sumOf {
            it.coffeeCountMap[coffeeType] ?: 0
        }

        CoffeeTypeCount(
            type = coffeeType,
            count = total
        )
    }.sortedByDescending { it.count }

    // Create type distribution chart entries
    val typeChartEntryModelProducer = ChartEntryModelProducer(typeDistribution.mapIndexed { index, data ->
        entryOf(index.toFloat(), data.count.toFloat())
    })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Coffee Consumption Over Time",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Line chart with Vico
        ProvideChartStyle(rememberChartStyle()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Chart(
                    chart = lineChart(),
                    model = chartEntryModelProducer.getModel(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _ ->
                            aggregatedData.getOrNull(value.toInt())?.label ?: ""
                        }
                    ),
                    chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = aggregatedData.size > 7)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Coffee type distribution
        Text(
            text = "Coffee Type Distribution",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Column chart for type distribution
        ProvideChartStyle(rememberChartStyle()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Chart(
                    chart = columnChart(),
                    model = typeChartEntryModelProducer.getModel(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _ ->
                            typeDistribution.getOrNull(value.toInt())?.type?.name?.take(3) ?: ""
                        }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

internal fun dailyAggregation(coffeeState: DaysCoffeesState): List<AggregatedData> =
    coffeeState.value.map { (date, dayCoffee) ->
        AggregatedData(
            label = "${date.month.name.take(3)} ${date.dayOfMonth}",
            totalCount = dayCoffee.coffeeCountMap.values.sum(),
        ) to date
    }.sortedBy { it.second }.map { it.first }

internal fun monthlyAggregation(coffeeState: DaysCoffeesState): List<AggregatedData> =
    coffeeState.value.entries.groupBy { entry ->
        YearMonth(entry.key.year, entry.key.month)
    }.map { (yearMonth, entries) ->
        val typeCounts = mutableMapOf<CoffeeType, Int>()
        entries.forEach { entry ->
            entry.value.coffeeCountMap.forEach { (type, count) ->
                typeCounts[type] = (typeCounts[type] ?: 0) + count
            }
        }

        AggregatedData(
            label = yearMonth.toString(),
            totalCount = typeCounts.values.sum(),
        ) to yearMonth
    }.sortedBy { it.second }.map { it.first }

// Helper functions and classes

@Composable
private fun rememberChartStyle(): ChartStyle {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    return remember {
        ChartStyle(
            axis = ChartStyle.Axis(
                axisLabelColor = onSurface,
                axisGuidelineColor = onSurface.copy(alpha = 0.1f),
                axisLineColor = onSurface.copy(alpha = 0.3f)
            ),
            columnChart = ChartStyle.ColumnChart(
                listOf(
                    LineComponent(
                        color = primary.toArgb(),
                        thicknessDp = 8f
                    )
                )
            ),
            lineChart = ChartStyle.LineChart(
                lines = listOf(
                    LineChart.LineSpec(
                        lineColor = primary.toArgb(),
                        lineBackgroundShader = null,
                        pointSizeDp = 8f,
                    )
                )
            ),
            marker = ChartStyle.Marker(),
            elevationOverlayColor = surface
        )
    }
}

data class WeeklyChartData(
    val date: LocalDate,
    val dayName: String,
    val totalCoffees: Int,
)

data class AggregatedData(
    val label: String,
    val totalCount: Int,
)

data class CoffeeTypeCount(val type: CoffeeType, val count: Int)
