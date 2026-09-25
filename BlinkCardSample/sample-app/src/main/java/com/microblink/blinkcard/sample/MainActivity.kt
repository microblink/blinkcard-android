package com.microblink.blinkcard.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.microblink.blinkcard.sample.navigation.Destination
import com.microblink.blinkcard.sample.result.BlinkCardResultHolder
import com.microblink.blinkcard.sample.result.BlinkCardSampleResultScreen
import com.microblink.blinkcard.sample.ui.MainScreen
import com.microblink.blinkcard.sample.ui.theme.BlinkCardTheme
import com.microblink.blinkcard.sample.utils.MainViewModel
import com.microblink.blinkcard.ux.BlinkCardCameraScanningScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlinkCardTheme {
                MainNavHost(navController = rememberNavController())
            }
        }
    }

    @Composable
    fun MainNavHost(
        modifier: Modifier = Modifier,
        navController: NavHostController = rememberNavController(),
        startDestination: String = Destination.Main,
    ) {
        val context = LocalContext.current
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Destination.Main) {
                val mainState by viewModel.mainState.collectAsStateWithLifecycle()

                mainState.error?.let {
                    Toast.makeText(this@MainActivity, "Error: $it", Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }

                MainScreen(
                    viewModel = viewModel,
                    launchCardScanning = {
                        viewModel.viewModelScope.launch {
                            viewModel.initializeLocalSdk(context)
                            navController.navigate(Destination.CardScanning)
                        }
                    }
                )
            }
            composable(Destination.CardScanning) {
                val sdk = viewModel.localSdk
                if (sdk != null) {
                    BlinkCardCameraScanningScreen(
                        blinkCardSdk = sdk,
                        uxSettings = viewModel.blinkCardUxSettings,
                        uiSettings = viewModel.blinkCardUiSettings,
                        cameraSettings = viewModel.cameraSettings,
                        sessionSettings = viewModel.scanningSessionSettings,
                        onScanningSuccess = { result ->
                            viewModel.onScanningResultAvailable(result)
                            navController.navigate(Destination.BlinkCardResult)
                        },
                        onScanningCanceled = {
                            navController.popBackStack(
                                route = Destination.Main,
                                inclusive = false
                            )
                            viewModel.onScanningCanceled()
                        }
                    )
                }
            }
            composable(Destination.BlinkCardResult) {
                BlinkCardSampleResultScreen(
                    result = BlinkCardResultHolder.blinkCardResult,
                    onNavigateUp = {
                        viewModel.resetState()
                        navController.popBackStack(
                            route = Destination.Main,
                            inclusive = false
                        )
                    }
                )
            }
        }
    }
}
