@file:Suppress("ModifierMissing")

package ru.beryukhov.coffeegram.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    val maxCoffeeCount = weekData.maxOfOrNull { it.totalCoffees } ?: 0

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Coffee Consumption This Week",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom bar chart for the week
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(
                    start = 40.dp, // Space for y-axis labels
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 40.dp // Space for x-axis labels
                )
        ) {
            // Draw chart background
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw grid lines
                val yStep = size.height / (maxCoffeeCount + 1).coerceAtLeast(1)
                val xStep = size.width / 7

                // Horizontal grid lines
                for (i in 0..maxCoffeeCount) {
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, size.height - i * yStep),
                        end = Offset(size.width, size.height - i * yStep),
                        strokeWidth = 1f
                    )
                }

                // Draw bars
                weekData.forEachIndexed { index, data ->
                    val barWidth = xStep * 0.6f
                    val barColor = when (index % 7) {
                        0 -> Color(0xFF4285F4) // Blue
                        1 -> Color(0xFFEA4335) // Red
                        2 -> Color(0xFFFBBC05) // Yellow
                        3 -> Color(0xFF34A853) // Green
                        4 -> Color(0xFF8F6ED5) // Purple
                        5 -> Color(0xFFF57C00) // Orange
                        else -> Color(0xFF795548) // Brown
                    }

                    val barHeight = if (maxCoffeeCount > 0) {
                        data.totalCoffees.toFloat() / maxCoffeeCount * size.height
                    } else {
                        0f
                    }

                    drawRect(
                        color = barColor,
                        topLeft = Offset(
                            x = index * xStep + (xStep - barWidth) / 2,
                            y = size.height - barHeight
                        ),
                        size = Size(barWidth, barHeight)
                    )
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekData.forEach { data ->
                    Text(
                        text = data.dayName,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }

            // Y-axis labels
            Column(
                modifier = Modifier
                    .height(260.dp)
                    .align(Alignment.CenterStart)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in maxCoffeeCount downTo 0) {
                    Text(
                        text = i.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Legend for coffee types
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CoffeeType.entries.forEach { coffeeType ->
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = getCoffeeTypeColor(coffeeType),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    Text(
                        text = coffeeType.name,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
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

    val maxValue = aggregatedData.maxOfOrNull { it.totalCount } ?: 0

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Coffee Consumption Over Time",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Line chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(
                    start = 40.dp, // Space for y-axis labels
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 40.dp // Space for x-axis labels
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val xStep = if (aggregatedData.isNotEmpty()) {
                    size.width / (aggregatedData.size - 1).coerceAtLeast(1)
                } else {
                    size.width
                }

                // Draw grid lines
                for (i in 0..5) {
                    val y = size.height - i * size.height / 5
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                // Draw line
                if (aggregatedData.size > 1) {
                    val points = aggregatedData.mapIndexed { index, data ->
                        val x = index * xStep
                        val y = if (maxValue > 0) {
                            size.height - data.totalCount.toFloat() / maxValue * size.height
                        } else {
                            size.height
                        }
                        Offset(x, y)
                    }

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = Color(0xFF4285F4),
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 3f
                        )
                    }

                    // Draw points
                    points.forEach { point ->
                        drawCircle(
                            color = Color(0xFF4285F4),
                            radius = 6f,
                            center = point
                        )
                    }
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Show subset of labels to avoid crowding
                val labelIndices = if (aggregatedData.size <= 6) {
                    aggregatedData.indices.toList()
                } else {
                    val step = aggregatedData.size / 6
                    aggregatedData.indices.step(step).toList()
                }

                // Add spacing for labels
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        labelIndices.forEach { index ->
                            Text(
                                text = aggregatedData.getOrNull(index)?.label ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(48.dp)
                            )
                        }
                    }
                }
            }

            // Y-axis labels
            Column(
                modifier = Modifier
                    .height(260.dp)
                    .align(Alignment.CenterStart)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 5 downTo 0) {
                    val value = maxValue * i / 5
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Coffee type distribution (stacked bar)
        Text(
            text = "Coffee Type Distribution",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Simplified coffee type distribution using stacked elements
        val typeDistribution = CoffeeType.entries.map { coffeeType ->
            val total = coffeeState.value.values.sumOf {
                it.coffeeCountMap[coffeeType] ?: 0
            }

            CoffeeTypeCount(
                type = coffeeType,
                count = total
            )
        }.sortedByDescending { it.count }

        val totalCount = typeDistribution.sumOf { it.count }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(vertical = 8.dp)
        ) {
            typeDistribution.forEach { item ->
                if (item.count > 0) {
                    val percentage = if (totalCount > 0) {
                        item.count.toFloat() / totalCount
                    } else {
                        0f
                    }

                    Box(
                        modifier = Modifier
                            .weight(percentage)
                            .fillMaxHeight()
                            .background(getCoffeeTypeColor(item.type))
                    )
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            typeDistribution.take(5).forEach { item ->
                if (item.count > 0) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = getCoffeeTypeColor(item.type),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Text(
                            text = "${item.type.name} (${item.count})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// Helper classes

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
