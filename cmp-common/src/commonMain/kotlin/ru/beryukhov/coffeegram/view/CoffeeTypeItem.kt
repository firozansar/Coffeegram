@file:OptIn(ExperimentalResourceApi::class)

package ru.beryukhov.coffeegram.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ru.beryukhov.coffeegram.data.CoffeeType
import ru.beryukhov.coffeegram.model.DaysCoffeesIntent
import ru.beryukhov.coffeegram.model.DaysCoffeesStore

@Composable
fun CoffeeTypeItem(
    localDate: LocalDate,
    coffeeType: CoffeeType,
    count: Int,
    daysCoffeesStore: DaysCoffeesStore,
    modifier: Modifier = Modifier,
    ) {
    Row(
        modifier = modifier.padding(16.dp)
    ) {
        Image(
            coffeeType = coffeeType,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterVertically)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(coffeeType.name),
            style = typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterVertically).weight(1f)
        )
        Row(modifier = Modifier.align(Alignment.CenterVertically)) {
            Spacer(Modifier.width(16.dp))
            val textButtonModifier = Modifier.align(Alignment.CenterVertically)
                .sizeIn(
                    maxWidth = 32.dp,
                    maxHeight = 32.dp,
                    minWidth = 0.dp,
                    minHeight = 0.dp
                )
            TextButton(
                onClick = { daysCoffeesStore.newIntent(DaysCoffeesIntent.MinusCoffee(localDate, coffeeType)) },
                contentPadding = PaddingValues(0.dp),
                modifier = textButtonModifier
            ) {
                Text("-")
            }
            Text(
                text = "$count",
                style = typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            TextButton(
                onClick = { daysCoffeesStore.newIntent(DaysCoffeesIntent.PlusCoffee(localDate, coffeeType)) },
                contentPadding = PaddingValues(0.dp),
                modifier = textButtonModifier
            ) {
                Text("+")
            }
        }
    }
}
