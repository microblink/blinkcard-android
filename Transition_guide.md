# Transition Guide: BlinkCard v2 to BlinkCard v3000

This guide will help you migrate your application from BlinkCard v2 to the new BlinkID v3000 SDK. The new BlinkCard v3 provides a modernized approach to document scanning and extraction with improved architecture and Jetpack Compose support.


## Key differences

### 1. Architecture changes

- **New Core Components**: Instead of Recognizer-based architecture, BlinkCard uses a streamlined Session-based approach
- **Modern Kotlin Features**: Written fully in Kotlin, the code is simple and easy to work with, while also supporting Java integration
- **Jetpack Compose**: Jetpack Compose is the main driver for the UI through `blinkcard-ux` package
- **Simplified Flow**: More straightforward API with clearer separation of concerns
- **Updated minimum OS requirement**: BlinkID SDK now requires Android API level 24 (Android 7.0 Nougat) or newer. This update allows us to leverage modern development practices, improve stability, and streamline future updates.

### 2. Integration methods

#### BlinkCard v2 (Old):
```kotlin
1. Maven (maven.microblink.com)
2. Manual Integration (through .aar)
```

#### BlinkCard v3000 (New):
```kotlin
1. Maven (Maven Central)
2. Manual Integration (through .aar)
3. Custom integration (source-available UX module allows forking and customizations)
```

## Migration guide

### 1. Update dependencies

#### Remove declaration of old maven repository:

```kts
// remove
maven { url 'https://maven.microblink.com' }
// from repositories declaration in your gradle files
```

#### Remove old dependencies:
```kts
// remove
implementation(com.microblink:blinkcard) 
// from build.gradle.kts

// or
microblink-blinkcard = { module = "com.microblink:blinkcard", version.ref = "microblinkBlinkCard" }
// from libs.versions.toml

// or through .aar file
```

#### Add new dependencies:
```kotlin
// for the base BlinkID SDK version, add
implementation(com.microblink:blinkcard-core)

// for the version that includes the scanning UX, add
implementation(com.microblink:blinkcard-ux)
// to build.gradle.kts
// NOTE: blinkid-ux depends on blinkid-core, so there is no need to include both 

// alternatively, use libs.versions.toml
blinkcard-core = { group = "com.microblink", name = "blinkcard-core", version.ref = "blinkCardSdkVersion" }
blinkcard-ux = { group = "com.microblink", name = "blinkcard-ux", versions.ref = "blinkCardSdkVersion" }

// or through .aar file
```

### 2. Update Import Statements

#### Old:
```kotlin
import com.microblink.blinkcard.*
```

#### New:
```kotlin
import com.microblink.blinkcard.core*
// if using the UX components
import com.microblink.blinkcard.ux*  
```

### 3. Initialization Changes

#### Old:
```java
// old initialization
MicroblinkSDK.setLicenseFile("license-key", context);

// creating recognizer
mRecognizer = new BlinkCardRecognizer();
// bundle recognizers into RecognizerBundle
mRecognizerBundle = new RecognizerBundle(mRecognizer);
```

#### New:
```kotlin
// New initialization
val instance = BlinkCardSdk.initializeSdk(
    context = context,
    BlinkCardSdkSettings(
        licenseKey = "licenseKey",
        downloadResources = downloadResources, // true by default
        resourceLocalFolder = "microblink/blinkcard"
    )
)

when {
    instance.isSuccess -> {
        BlinkCardCameraScanningScreen(
            blinkCardSdk = instance,
            uxSettings = blinkCardUxSettings,
            uiSettings = uiSettings,
            cameraSettings = cameraSettings,
            sessionSettings = blinkCardSessionSettings,
            onScanningSuccess = { blinkCardScanningResult ->  },
            onScanningCanceled = { },
        )
    }

    instance.isFailure -> {
        val exception = instance.exceptionOrNull()
        Log.e(TAG, "Initialization failed", exception)
    }
}
```

### 4. UI Implementation Changes

#### Old:

Many different implementation methods exist for BlinkCard, with the following being the simplest:
```kotlin

// STEP #1
class MyApplication : Application() {
    @Override
    fun onCreate() {
        MicroblinkSDK.setLicenseFile("path/to/license/file/within/assets/dir", this)
    }
}

// STEP #2
class MyActivity : Activity() {
    private var mRecognizer: BlinkCardRecognizer? = null
    private var mRecognizerBundle: RecognizerBundle? = null

    @Override
    protected fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        // create BlinkCardRecognizer
        mRecognizer = BlinkCardRecognizer()
        // bundle recognizers into RecognizerBundle
        mRecognizerBundle = RecognizerBundle(mRecognizer)
    }
}

// STEP #3
// method within MyActivity from previous step
fun startScanning() {
    // Settings for BlinkCardActivity
    val settings: UISettings = BlinkCardUISettings(mRecognizerBundle)
    ActivityRunner.startActivityForResult(this, MY_REQUEST_CODE, settings)
}

// STEP #4
@Override
protected fun onActivityResult(requestCode: kotlin.Int, resultCode: kotlin.Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == MY_REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            // load the data into all recognizers bundled within your RecognizerBundle
            mRecognizerBundle.loadFromIntent(data)
            // you can get the result by invoking getResult on recognizer
            val result: BlinkCardRecognizer.Result = mRecognizer.getResult()
            if (result.getResultState() === Recognizer.Result.State.Valid) {
                // result is valid, you can use it however you wish
            }
        }
    }
}
```

#### New (BlinkCard) using Jetpack Compose:

`CameraScanningScreen` is a `@Composable` function that can be invoked when needed.
It is recommended to use on its own separate screen through `Navigation` and `ViewModel` (see Sample app).
```kotlin
fun BlinkCardCameraScanningScreen(
    blinkCardSdk: BlinkCardSdk,
    uxSettings: BlinkCardUxSettings = BlinkCardUxSettings(),
    uiSettings: UiSettings = UiSettings(),
    cameraSettings: CameraSettings = CameraSettings(),
    sessionSettings: BlinkCardSessionSettings = BlinkCardSessionSettings(),
    onScanningSuccess: (BlinkCardScanningResult) -> Unit,
    onScanningCanceled: () -> Unit,
)
```

#### New (BlinkCard) using Android Views:

Wrap the `Composable` in a `ComposeView` class:
```xml
<androidx.compose.ui.platform.ComposeView
android:id="@+id/my_composable"
android:layout_width="wrap_content"
android:layout_height="wrap_content" />
```

```kotlin
findViewById<ComposeView>(R.id.my_composable).setContent {
    MaterialTheme {
        Surface {
            BlinkCardCameraScanningScreen(...)
        }
    }
}
```

### New (BlinkCard) using Activity:

```kotlin
val blinkCardLauncher = rememberLauncherForActivityResult(
    contract = MbBlinkCardScan(),
    onResult = { scanningResult ->
        if (scanningResult.status == ScanActivityResultStatus.Scanned) {
            // use scanningResult (BlinkCardScanningResult)
        }
    })

blinkCardLauncher.launch(
    BlinkCardScanActivitySettings(
        sdkSettings = BlinkCardSdkSettings(
            licenseKey = "licenseKey"
        ), 
        scanningSessionSettings = BlinkCardSessionSettings(), 
        uxSettings = BlinkCardUxSettings(), 
        cameraSettings = CameraSettings(), 
        scanActivityUiColors = null, 
        scanActivityUiStrings = SdkStrings.Default, 
        scanActivityTypography = ParcelableUiTypography.Default(null), 
        showOnboardingDialog = true, 
        showHelpButton = true, 
        enableEdgeToEdge = true, 
        deleteCachedAssetsAfterUse= false
    )
)
```
For additional information on using Jetpack Compose with Views, visit [official docs](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/compose-in-views).

### 5. Result Handling

#### Old:

By checking recognizer state:
```kotlin
  recognizerBundle.loadFromIntent(data)
  val result: BlinkCardRecognizer.Result = recognizer.result
  val name = result.getCardNumber()
```

#### New:

Using the UX module to fetch the results:
```kotlin
        BlinkCardCameraScanningScreen(
    blinkCardSdk = instance,
    uxSettings = blinkCardUxSettings,
    uiSettings = uiSettings,
    cameraSettings = cameraSettings,
    sessionSettings = blinkCardSessionSettings,
    onScanningSuccess = { blinkCardScanningResult ->
        // result is now available
        navController.navigateTo(
            route = Destination.ResultScreen, // navigate to result screen or somewhere else
            inclusive = false
        )
    },
    onScanningCanceled = {
        navController.popBackStack(
            route = Destination.Main,
            inclusive = false
        )
    },
)
```

Using the `BlinkCardScanActivity` result:
```kotlin
val blinkCardLauncher = rememberLauncherForActivityResult(
    contract = MbBlinkCardScan(),
    onResult = { scanningResult ->
        if (scanningResult.status == ScanActivityResultStatus.Scanned) {
            // use scanningResult (BlinkCardScanningResult)
        }
    }
)
```

### 6. Custom UI Implementation

#### Old:

Old BlinkCard offered several ways of custom UI integration through resource and UI customization.
More info can be found here [on our GitHub page](https://github.com/microblink/blinkcard-android/tree/v2.12.0?tab=readme-ov-file#-blinkcard-sdk-integration-levels).

#### New:

New implementation offers customization in two ways:
- Through `UiSettings` class
- By forking the repository and customizing certain classes

`UiSettings` offers quick and easy customization for colors, strings, and fonts used in the SDK.
```kotlin
public data class UiSettings(
    val typography: UiTypography? = null,
    val colorScheme: ColorScheme? = null,
    val uiColors: UiColors? = null,
    val sdkStrings: SdkStrings? = null,
    val showOnboardingDialog: Boolean = DefaultShowOnboardingDialog,
    val showHelpButton: Boolean = DefaultShowHelpButton
)
```

```kotlin
val blinkCardUiSettings = UiSettings(
    typography = ..., // override if necessary
    colorScheme = ...,  // override if necessary
    uiColors = ...,  // override if necessary
    sdkStrings = ...,  // override if necessary
    showOnboardingDialog = ...,  // default: true; override if necessary
    showHelpButton = ...,  // default: true; override if necessary
)

val blinkCardUxSettings = BlinkCardUxSettings(
   stepTimeoutDurationMs = ... // default: [Duration] 15000.milliseconds,
   allowHapticFeedback = ... // default: [Boolean] true
)

val blinkCardCameraSettings = data class CameraSettings(
    lensFacing = ... // default: [CameraLensFacing] CameraLensFacing.LensFacingBack,
    desiredResolution = ... // default: [Resolution] Resolution.Resolution2160p
)

fun BlinkCardCameraScanningScreen(
    sdkInstance,
    blinkCardUxSettings,
    blinkCardUiSettings,
    blinkCardCameraSettings
)
```

If these customization options are not enough, some files can be modified.
Our entire `blinkcard-ux` module is source-available in `sources` directory located in this repository.
The module can be cloned and implemented as a local module in your project, allowing you to modify certain files.

Any class in `blinkcard-ux` library which has this license header is allowed to be modified.
```
/**
 * Copyright (c) Microblink. Modifications are allowed under the terms of the
 * license for files located in the UX/UI lib folder.
 */
```
Any modifications to classes which do not have this exact header are not allowed.

## Best Practices for Migration

1. **Gradual Migration**:
    - Consider migrating feature by feature if possible
    - Test thoroughly in a development environment before production deployment

2. **Resource Management**:
    - Decide between downloaded or bundled resources early in the migration
    - Set up proper resource paths and Blink Card resource loading

3. **UI/UX Considerations**:
    - Take advantage of Jetpack Compose if possible
    - Consider reimplementing custom UI components using the new architecture

4. **Error Handling**:
    - Update error handling to work with the new async/await pattern
    - Implement proper error handling for resource downloading if used

## Support and Resources

- For API documentation: Visit the BlinkCard SDK [Android API](https://microblink.github.io/blinkcard-android/index.html) docs.
- For support: Contact technical support through the support portal
