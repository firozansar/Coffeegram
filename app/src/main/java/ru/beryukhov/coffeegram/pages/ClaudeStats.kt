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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.legend.legendItem
import com.patrykandpatrick.vico.compose.legend.verticalLegend
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import ru.beryukhov.coffeegram.data.CoffeeType
import ru.beryukhov.coffeegram.data.DayCoffee
import ru.beryukhov.coffeegram.model.DaysCoffeesState

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
    val endOfWeek = startOfWeek.plus(DatePeriod(days = 6)) // Sunday

    // Filter data for current week and aggregate by day
    val weekData = (0..6).map { dayOffset ->
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
            coffeesByType = dayCoffee.coffeeCountMap
        )
    }

    // Create chart entries
    val chartEntryModelProducer = ChartEntryModelProducer(weekData.mapIndexed { index, data ->
        entryOf(index.toFloat(), data.totalCoffees.toFloat())
    })

    // Create coffee type legend items
    val legendItems = CoffeeType.entries.map { coffeeType ->
        legendItem(
            icon = shapeComponent(
                shape = Shapes.pillShape,
                color = getCoffeeTypeColor(coffeeType)
            ),
            label = textComponent(
                color = MaterialTheme.colorScheme.onSurface,
                textSize = 12.sp
            ),
            labelText = coffeeType.name
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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

        // Legend
        verticalLegend(
            items = legendItems,
            iconSize = 16.dp,
            iconPadding = 8.dp
        )
    }
}

@Composable
fun AllTimeCoffeeChart(coffeeState: DaysCoffeesState) {
    // Only process if we have data
    if (coffeeState.value.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("No data available")
        }
        return
    }

    // Get date range for all available data
    val dates = coffeeState.value.keys
    val minDate = dates.minOrNull() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val maxDate = dates.maxOrNull() ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // Calculate day difference
    val daysBetween = maxDate.toEpochDays() - minDate.toEpochDays()

    // Prepare data
    val useMonthlyAggregation = daysBetween > 31

    // Aggregate data
    val aggregatedData = if (useMonthlyAggregation) {
        // Monthly aggregation
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
                typeBreakdown = typeCounts
            )
        }.sortedBy { it.label }
    } else {
        // Daily aggregation
        coffeeState.value.map { (date, dayCoffee) ->
            AggregatedData(
                label = "${date.month.name.take(3)} ${date.dayOfMonth}",
                totalCount = dayCoffee.coffeeCountMap.values.sum(),
                typeBreakdown = dayCoffee.coffeeCountMap
            )
        }.sortedBy { it.label }
    }

    // Create chart entries
    val chartEntryModelProducer = ChartEntryModelProducer(aggregatedData.mapIndexed { index, data ->
        entryOf(index.toFloat(), data.totalCount.toFloat())
    })

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

    // Create legend items
    val legendItems = typeDistribution.take(5).map { data ->
        legendItem(
            icon = shapeComponent(
                shape = Shapes.pillShape,
                color = getCoffeeTypeColor(data.type)
            ),
            label = textComponent(
                color = MaterialTheme.colorScheme.onSurface,
                textSize = 12.sp
            ),
            labelText = "${data.type.name} (${data.count})"
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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

        // Legend
        verticalLegend(
            items = legendItems,
            iconSize = 16.dp,
            iconPadding = 8.dp
        )
    }
}

// Helper functions and classes

@Composable
private fun rememberChartStyle(): ChartStyle {
    val colors = CoffeeType.entries.map { getCoffeeTypeColor(it) }
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
                columns = colors.map { color ->
                    LineComponent(
                        color = color.toArgb(),
                        thicknessDp = 8f
                    )
                }
            ),
            lineChart = ChartStyle.LineChart(
                lines = listOf(
                    LineChart.LineSpec(
                        lineColor = primary.toArgb(),
                        lineBackgroundShader = null,
                        pointSizeDp = 8f,
//                        pointColor = MaterialTheme.colorScheme.primary,
//                        pointInnerColor = MaterialTheme.colorScheme.surface
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
    val coffeesByType: Map<CoffeeType, Int>
)

data class YearMonth(val year: Int, val month: Month) {
    override fun toString(): String {
        return "${month.name.take(3)} $year"
    }
}

data class AggregatedData(
    val label: String,
    val totalCount: Int,
    val typeBreakdown: Map<CoffeeType, Int>
)

data class CoffeeTypeCount(val type: CoffeeType, val count: Int)

@Composable
fun getCoffeeTypeColor(coffeeType: CoffeeType): Color {
    return when (coffeeType.ordinal % 7) {
        0 -> Color(0xFF4285F4) // Blue
        1 -> Color(0xFFEA4335) // Red
        2 -> Color(0xFFFBBC05) // Yellow
        3 -> Color(0xFF34A853) // Green
        4 -> Color(0xFF8F6ED5) // Purple
        5 -> Color(0xFFF57C00) // Orange
        else -> Color(0xFF795548) // Brown
    }
}
