package ru.beryukhov.coffeegram

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import ru.beryukhov.coffeegram.animations.TransitionSlot
import ru.beryukhov.coffeegram.data.CoffeeTypes
import ru.beryukhov.coffeegram.data.DAY_COFFEE_PATH
import ru.beryukhov.coffeegram.data.DayCoffee
import ru.beryukhov.coffeegram.data.START_ACTIVITY_PATH
import ru.beryukhov.coffeegram.data.toDataMap
import ru.beryukhov.coffeegram.model.NavigationIntent
import ru.beryukhov.coffeegram.model.NavigationState.Companion.NAVIGATION_STATE_KEY
import ru.beryukhov.coffeegram.model.NavigationState.Companion.TODAYS_COFFEE_LIST
import ru.beryukhov.coffeegram.model.NavigationStore
import ru.beryukhov.coffeegram.pages.LandingPage

class MainActivity : ComponentActivity() {

    internal val nodeClient by lazy { Wearable.getNodeClient(this) }
    internal val messageClient by lazy { Wearable.getMessageClient(this) }
    internal val dataClient by lazy { Wearable.getDataClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var doAnimationState by rememberSaveable {
                mutableStateOf(true)
            }
            val navigationStore: NavigationStore = koinInject()
            TransitionSlot(
                doAnimation = doAnimationState,
                StartPage = { modifier -> LandingPage(modifier = modifier) },
                EndPage = { modifier, topPadding ->
                    PagesContent(
                        modifier = modifier,
                        topPadding = topPadding,
                        startWearableActivity = ::startWearableActivity,
                        showMap = checkCoarseLocationPermission(),
                        navigationStore = navigationStore
                    )
                },
            ) {
                doAnimationState = false
                if (intent.getStringExtra(NAVIGATION_STATE_KEY) == TODAYS_COFFEE_LIST) {
                    navigationStore.newIntent(
                        NavigationIntent.OpenCoffeeListPage(
                            dayOfMonth = Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date.dayOfMonth
                        )
                    )
                }
            }
        }
    }
}

private fun MainActivity.startWearableActivity() {
    lifecycleScope.launch {
        try {
            val nodes = nodeClient.connectedNodes.await() // todo depending on nodes count show or hide button

            // Send a message to all nodes in parallel
            nodes.map { node ->
                async {
                    messageClient.sendMessage(node.id, START_ACTIVITY_PATH, byteArrayOf()).await()
                }
            }.awaitAll()

            Log.d(TAG, "Starting activity requests sent successfully")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d(TAG, "Starting activity failed: $exception")
        }
    }
    sendDayCoffee(
        // todo replace mock
        DayCoffee(
            mapOf(
                CoffeeTypes.Cappuccino to 1,
                CoffeeTypes.Americano to 2
            )
        )
    )
}

private fun MainActivity.sendDayCoffee(dayCoffee: DayCoffee) {
    lifecycleScope.launch {
        try {
            val request = PutDataMapRequest.create(DAY_COFFEE_PATH).apply {
                dayCoffee.toDataMap(dataMap)
            }.asPutDataRequest().setUrgent()

            val result = dataClient.putDataItem(request).await()

            Log.d(TAG, "DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d(TAG, "Saving DataItem failed: $exception")
        }
    }
}

private fun MainActivity.checkCoarseLocationPermission(): Boolean = checkSelfPermission(
    ACCESS_COARSE_LOCATION
) == PackageManager.PERMISSION_GRANTED

private const val TAG = "TestWatch_"
