@file:OptIn(ExperimentalMaterial3Api::class)

package ru.beryukhov.coffeegram.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import org.koin.androidx.compose.koinViewModel
import ru.beryukhov.coffeegram.R
import ru.beryukhov.coffeegram.model.DaysCoffeesState
import ru.beryukhov.coffeegram.model.DaysCoffeesStore

@Composable
fun ColumnScope.StatsPage(
    modifier: Modifier = Modifier,
    statsPageViewModel: StatsPageViewModel = koinViewModel<StatsPageViewModelImpl>(),
) {
    CoffeeCharts(coffeeState = statsPageViewModel.getState(), modifier = modifier.weight(1f))
}

@Preview
@Composable
private fun Preview() {
    Column {
        StatsPage(
            statsPageViewModel = StatsPageViewModelStub,
        )
    }
}

@Composable
fun StatsAppBar(modifier: Modifier = Modifier) {
    TopAppBar(
        title = { Text(stringResource(R.string.stats)) },
        modifier = modifier
    )
}

interface StatsPageViewModel {
    @Composable
    fun getState(): DaysCoffeesState
}

object StatsPageViewModelStub : StatsPageViewModel {
    @Composable
    override fun getState() = DaysCoffeesState()
}

class StatsPageViewModelImpl(
    private val daysCoffeesStore: DaysCoffeesStore,
) : ViewModel(), StatsPageViewModel {
    @Composable
    override fun getState(): DaysCoffeesState = daysCoffeesStore.state.collectAsState().value
}
