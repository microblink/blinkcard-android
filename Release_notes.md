# Release notes

## v3000.0.1

### What's new
- Improved SDK and session shutdown behavior, and removed misleading warning logs during normal cleanup
- Improved analytics reporting
- Improved SDK initialization to help reduce failures

## v3000.0.0

### New fraud detection: BIN intelligence
BIN intelligence enhances payment risk assessment at the moment of card capture. 
Real-time BIN lookup validates the PAN and returns detailed card information, including type, brand, category, issuing bank, and issuer country. 
This additional fraud signal helps customers identify high-risk transactions early, before the transaction begins.

### Architectural shift: session-based API
The legacy `Recognizer` architecture is deprecated. BlinkCard v3000 moves to a session-based model, decoupling scanning logic from the UI lifecycle and enabling a headless integration for custom implementations.

### Epoch versioning scheme
We are moving to an Epoch-based versioning system: `(EPOCH * 1000 + MAJOR).MINOR.PATCH`. This new versioning makes it easier for customers to immediately identify the impact of an update and plan upgrades with confidence. 
- **EPOCH**: Fundamental architectural rewrites.
- **MAJOR**: Breaking API changes.
- **MINOR**: New features, backward-compatible.
- **PATCH**: Bug fixes.

### Other improvements
- **i18n**: Expanded localization support to better serve a global user base: we added 33 new languages, bringing the total supported languages to 56, enabling a more convenient user experience across diverse regions.
- **a11y**: Enhanced accessibility capabilities, including support for screen readers.
- We introduced more granular event tracking throughout the SDK lifecycle, enabling customers to gain deeper insights into success rates and drop-off points during scanning sessions. These enhanced analytics help identify optimization opportunities and ensure an improved, more reliable user experience.
- Improved photocopy detection model, reducing FAR@FRR of 1% from 27.53% to 13.69%, significantly enhancing reliability.

### Highlights & integration improvements
- **Lighter SDK**: By incorporating dynamic model loading, BlinkCard v3000 drastically reduces download size from ~11MB to ~3.8MB, and lowers memory usage.
- **Unified session-based API**: We've moved away from recognizer-approach to a single session-based BlinkCardScanningSession, unifying scanning logic under one simplified API.
- **Backward compatibility**: Existing production keys will continue to work with v3000.0. No new license key is required for the upgrade.
- **More maintainable codebase**: This new architecture sets the stage for easier and faster updates.
- **Updated minimum OS requirement**: BlinkCard SDK now requires Android API level 24 (Android 7.0 Nougat) or newer. This update allows us to leverage modern development practices, improve stability, and streamline future updates.

### Architecture changes
- **New core components**: Instead of Recognizer-based architecture, BlinkCard uses a streamlined Session-based approach.
- **Modern Kotlin features**: Written fully in Kotlin, the code is simple and easy to work with, while still supporting Java integration.
- **Jetpack Compose UI & modular SDK**:
  - UI is now built with Jetpack Compose, simplifying integration via `blinkcard-ux`.
  - The SDK is modular:
    - `blinkcard-core` for scanning logic.
    - `blinkcard-ux` for prebuilt UI components.
- **Simplified flow**: More straightforward API with clearer separation of concerns.

### Major API changes

- Simplified initialization & result handling
  - New SDK initialization method:
    ```kotlin
    BlinkCardSdk.initializeSdk(BlinkCardSdkSettings(licenseKey))
    ```
  - Results are now retrieved through structured session-based callbacks instead of recognizer bundles.

- Enhanced UI customization & source-available UX
  - `UiSettings` enables direct customization of typography, colors, and strings.
  - The source-available UI layer allows advanced modifications for branding and accessibility.

### Plan your upgrade
We have prepared transition guides to help you get started with BlinkCard v3000.0.0:

- [Transition guide](https://blinkcard.docs.microblink.com/migration-v3000?platform=android)

These guides outline all major API changes and best practices for migrating existing code. 

For any questions or feedback, reach out to support@microblink.com. We value your input and look forward to hearing how BlinkCard v3000 improves your app’s experience!


## v2.12.0

### Improvements
- Integrated a tilt detector that will improve the quality of captured images and the extracted data:
    - new UI message `Keep card parallel to phone` for detectiion status `DetectionStatus::CameraAngleTooSteep`
- Integrated a stability check to ensure consecutive images for extraction are consistent
- Improved accuracy of owner field extraction

### Bug fixes
- Fixed issue with anonymization of vertical cards where in some cases sensitive fields were not anonymized correctly
- Fixed issue with anonymization of cards when document was scanned at a steep angle
- Fixed issue with images when extension are enabled that resulted in an incorrect aspect ratio

## v2.11.1

### Behavior Changes
- Improved extraction of the owner field
- Only digits are allowed in card number, CVV, and date of expiry results
- Only letters are allowed in owner field result

## v2.11.0

### Improvements
- Improved data extraction accuracy across all supported card types

### UI changes
- Added success checkmark after a successful first side scan
- Added an error state when scanning the wrong side of the card
- Minor changes in scanning instruction messages

### Other changes
- Native code is now built using NDK r28b

## 2.10.2

- The SDK is now supported on Android 15 16KB page size devices.

## 2.10.1

### Bug fixes

- Resolved an issue where UI elements could be obscured by system bars when using edge-to-edge mode. The SDK now properly applies insets to prevent overlap, ensuring content remains fully visible

## 2.10.0

### Improvements

- Significant improvements in photocopy detection. Both the False Rejection Rate and False Acceptance Rate are reduced by ~50% as measured on the default match level.

### Bug fixes

- Fix for duplicate attrs resource: `attr/mb_onboardingImageColor` when combining multiple Microblink's SDKs in the same app

### Other changes

- Removed `libc++_shared.so` from the SDK


## 2.9.3

### Bug fixes

- fixed Ping server URL error when using `proguard-android-optimize.txt` default proguard file

## 2.9.2 

### Bug fixes

- fixed URL of the server performing online license check when it's enabled
    - in v2.9.1 the URL depended on the `BUILD_TYPE` property, pointing to production server only when `BUILD_TYPE` was set to `distribute`. However, apparently the `BUILD_TYPE` is not a compile-time property on Android like it's on other platforms and native code, so it was affected by the setting of the app that was integrating the SDK and that caused the SDK to call to a dev server which is unavailable from the external network.

### Other changes

- Add option to change Ping URL for Ping proxy feature through `MicroblinkSDK.setPingProxyUrl()`
- minor improvements in the default UI w.r.t. accessibility

## 2.9.1 

### Minor API changes

- split up `Image` class to `Image` and `InputImage`
    - `InputImage` is to be used as an input to the recognizers. `Image` will be the result of recognizer processing
    - `InputImage` retains ROI functionality, but is now not serializable
    - `InputImage` handles YUV planes more efficiently
    - `Image` is now always `BGRA` and is serializable

### Bug fixes

- removed UI thread blocking while waiting for recognition to complete
    - this fixes the ANR that can occur when app gets resumed from background on slow network connections when using online license keys
- fix documentation generated links to Javadoc
- fixed crash that occurred when HDR image was provided as `Bitmap` to the Direct API
    - if provided Bitmap is not in `ARGB_8888` config, it will be automatically converted into `ARGB_8888` config
    - only if the conversion is not possible, then the exception will be thrown


### Other changes

- native code is now built using NDK r26c and uses dynamic c++ runtime (`libc++_shared.so`)
    - in case when multiple different dependencies use the same runtime in the same app, the latest available version of the library should be packaged into the app
- added support for license keys that support multiple application IDs
- added `android.permission.INTERNET` permission to the manifest of `LibBlinkCard`
    - this permission is needed in order to correctly perform license key validation for licenses that require that

## 2.9.0

### What's new in the BlinkCard Recognizer?
- Improved scanning performance and added support for virtually any card layout
- Improved IBAN parser which now supports more IBAN formats
- Added option `allowInvalidCardNumber` which allows reading invalid card numbers to avoid endless scanning on samples and test cards:
    - use with care as it might reduce accuracy in certain situations in production
    - for invalid card number the flag `cardNumberValid` in `BlinkCardRecognizer.Result` will be set to `false`

### Improvements
- Better support for RTL languages in our default UX

### Breaking API changes:
- Min Android SDK API level is raised to **21 (Android 5.0)**
- Removed legacy recognizers: `LegacyBlinkCardRecognizer` and `LegacyBlinkCardEliteRecognizer`


## 2.8.1
- Fixed crashing issue when opening onboarding screens the help button

## 2.8.0

### Improvements
- Included hand, photocopy, and screen detection models to achieve liveness functionality
- Added anonymization info on which side was anonymized. String data is anonymized using an asterisk instead of blanking the result. 
- Expanded the number of supported credit card types by 100%.
- Improved data extraction, including a 30% reduction in incorrect processing of CVV field.

### What's new in the BlinkCard Recognizer?
- Added new settings `handScaleThreshold`, `handDocumentOverlapThreshold`, `screenAnalysisMatchLevel`, `photocopyAnalysisMatchLevel`. These settings are used in combination with the new liveness features.
- Added a new callback `LivenessStatusCallback`, which is invoked when each side of a card is scanned. It is called with one parameter, a `LivenessStatus` enum. Use `BlinkCardRecognizer.setLivenessStatusCallback` method to set the callback.


### BlinkCard Recognizer Result
- Two new booleans: `firstSideAnonymized` and `secondSideAnonymized` have been added to indicate whether the first or second side of the card has been anonymized, respectively.
- New result documentLivenessCheck which has new liveness model results. It contains liveness information about the first and second sides of the card. Liveness information contains the results of checks performed on the card using screen detection, photocopy detection, and the presence of a live hand.

## 2.7.0

### New features:
- Improved support for diverse credit card designs.

### Architecture support:
- Devices that are based on the Intel x86 architecture, rather than ARMv7, are no longer supported. x86 and x86_64 architectures are used on very few devices today with most of them being manufactured before 2015, and only a few after that (e.g. Asus Zenfone 4). According to the Device catalog on Google Play Console, these devices make up about 1% of all Android devices (223 out of 22074 devices that have an API level of 21 and above support this architecture).

#### New user instructions that lead to successful scans
- More detailed instructions on how to scan credit cards, via an intro tutorial or tooltip during scanning, leading to improved success rates in credit card scanning and data extraction.

#### New layout for `BlinkCardUISettings`
- The scanning screen now shows a reticle in the center with scanning instruction.

### What's new in the BlinkCard Recognizer?
- Added a new property `fallbackAnonymization` in `AnonymizationSettings`. If true, anonymization is applied on all fields of the image if extraction is uncertain.
- Improved anonymization performance.

## 2.6.0

### New features:
- We've added support for launching the scan activity via the new Activity Result API. Check out our sample to see how to implement it.

### Improvements
- We’ve added support for 1000+ new credit card types.
- Decrease of wrongly PAN field processing by 30% for horizontal credit cards, and by 60% for vertical credit cards.
- Improvements in the Anonymization functionality for Quick Read formats on VISA credit cards, as well as general improvements for all other credit card types.

## 2.5.2

### Visual identity update
- We’ve updated Microblink logo and colors

### Fixes
- Fixed crash on some devices (mostly Huawei) when opening camera if SDK was used in full screen mode

## 2.5.1

- Scanning improvements

## 2.5.0

### Improvements and fixes:
- Improved date extraction from partial dates.
- Fixed broken anonymization in result screen when the PAN number was anonymized but the image was not. We now correctly anonymize the image too.


### Minor breaking API change

- We've changed how `RecognizerRunner` processes images. Now it can treat `Image` objects as either video frame or photo frame. Until now, Direct API always processed images as photo frames, that not giving ability to recognizers to use time-redundant information for yield better recognition quality.

## 2.4.0

### Improvements and fixes:
- We updated the default BlinkCard scanning screen (`BlinkCardOverlayController` and `BlinkCardActivity`) which is activated when using BlinkCardUISettings. Now the UI supports landscape mode.
- From now on, BlinkCard reads and extracts the expiry date in MM/YYYY format
- We’ve fixed camera issues for the following devices:
	- Motorola Moto G100 - Camera 2 API wasn’t working
	- Realme X50 5G - Problem with Camera 2 API in legacy mode
	- LG K4 - Camera 2 API wasn’t working
	- Galaxy Tab Active 2 - broken aspect ratio


## 2.3.0

### New features:
- We've added support for vertical payment cards.
- As of this release, BlinkCard supports the Visa Quick Read format (the one where the card number spans through four lines).
- We've changed the threshold for "Camera too far" and "Camera too near" callbacks. From now on, the card needs to be closer to the camera.

## 2.2.0

### New features:
- We've added a new feature that lets you anonymize extracted results, images or both:
	- Choose which fields you want to mask:
		- Card number
		- Card number prefix
		- CVV
		- Owner
		- IBAN
	- Choose a `BlinkCardAnonymizationMode` for each field:
		- `None`
		- `ImageOnly` - Black boxes will cover chosen data on the extracted image, results are not anonymized
		- `ResultFieldsOnly` - String data is redacted from the result, images are not anonymized
		- `FullResult` - Both images and string data (results) are anonymized
	- You can customize the way you anonymize the card number through `CardNumberAnonymizationSettings`:
		- `prefixDigitsVisible` - Defines how many digits at the beginning of the card number remain visible after anonymization
		- `suffixDigitsVisible` - Defines how many digits at the end of the card number remain visible after anonymization

### Improvements
- We've added support for new horizontal card layouts

## 2.1.0

### New features:
- We’ve updated SDK's Android target API level to 30.
- We’ve translated the complete SDK to the Serbian language.
- We’ve made the SDK safe from tapjacking, a form of attack where a user is tricked into tapping something he or she didn’t intend to tap. We did this by adding a new security option that prompts the SDK to discard touches when the activity's window is obscured by another visible window. To activate it, use UISettings.setFilterTouchesWhenObscured(true).

### Other improvements:
- We’ve updated our default UI with new error messages shown if the user doesn’t place the card within the frame.
- We’ve updated security code field on the edit results screen, it is no longer anonymized.
- We’ve added a new edit results screen customisation option, you can now change the toolbar icon (`mb_blinkcardEditToolbarNavigationIcon` theme attribute).
- We've introduced other variants of `resetRecognitionState` methods that enable resetting of the current recognition step. Call `RecognizerRunner.resetRecognitionState(false)` when scanning with Direct API and `RecognizerRunnerView.resetRecognitionState(false)` when scanning with the camera to clear the scanning cache and results only for the current recognition step, for example, only for the backside of the document.

### Minor API changes:
- We've replaced `Using time-limited license!` warning with `Using trial license!` warning. The warning message is displayed when using a trial license key. To disable it, use `MicroblinkSDK.setShowTrialLicenseWarning(false)`.

### Bug fixes:
- SDK does not require permission android.permission.ACCESS_NETWORK_STATE to unlock itself anymore, in cases when the license key needs online activation.
- We have fixed an edit results screen issue causing user changes being lost when the app goes to background.
- We've fixed the front camera error on `Oukitel WP8 Pro`.


## 2.0.0

### **BlinkCard v2 Release Announcement**

We're proud to announce our AI-driven BlinkCard v2.0! Extract the **card number** (PAN), **expiry date**, **owner** information (name or company title), **IBAN**, and **CVV**, from a large range of different card layouts.

As of this version, BlinkCard SDK is fully compatible with other Microblink SDKs, which means that you can use it with other Microblink SDKs in the same application.

- BlinkCard v2.0 now supports card layouts with all information on the back side of the card!

- `BlinkCardRecognizer` is a Combined recognizer, which means it's designed for scanning **both sides of a card**. However, if all required data is found on the first side, we do not wait for second side scanning. We can return the result early. A set of required fields is defined through the recognizer's settings.

- "Front side" and "back side" are terms more suited to ID scanning. We start the scanning process with the **side containing the card number**. This makes the UX easier for users with cards where all data is on the back side.

- We've expanded the set of possible recognizer states with **StageValid**. This state is set when first side scanning completes with valid data, and second side scanning is required.

- Available `BlinkCardRecognizer` **settings**:
	- You can toggle mandatory **extraction** of all fields except the PAN.
	- You can enable the **blur filter**. When blur filtering is enabled, blurred frames are discarded. Otherwise, we process the blurred frames but set the blur indicator result member.
	- You can define required **padding** around the detected document. This ensures some empty space exists between the document and the edge of the frame.

- `BlinkCardRecognizer.Result` structure:
    - Contains:
        - The card issuer
        - PAN
        - PAN prefix
        - Expiry date
        - Owner information
        - IBAN
        - CVV
        - Cropped document images
        - Blur indicators for both sides
        - Processing status
	- **Processing status** can be one of:
		- Success - if the process ended successfully and data is valid
		- DetectionFailed - if detection of the document failed
		- ImagePreprocessingFailed - if preprocessing of the image failed
		- StabilityTestFailed - if inconsistent results were detected between different video frames (when video processing, we require at least two frames with consistent data, for image processing this isn't applicable)
		- ScanningWrongSide - if the first side presented in the scanning process does not contain the PAN, or when the user failed to present the second side
		- FieldIdentificationFailed - if we detected a field, but we're unable to parse it (possible glare issues, or a finger covering the field)
		- ImageReturnFailed - failed to return requested images
		- UnsupportedCard - this card layout is currently unsupported.


### New features:

- We added a new BlinkCard screen that allows users to edit `BlinkCardRecognizer` scan results:
    - This screen allows users to edit scanned data and input data that wasn't scanned.
    - Enable it by calling `BlinkCardUISettings.setEditScreenEnabled(true)`.
    - Configure which fields should be displayed on this screen by using `BlinkCardUISettings.setEditScreenFieldConfiguration()` method.
    - Set your custom theme with `BlinkCardUISettings.setEditScreenTheme()` method.
    - Change default strings by using `BlinkCardUISettings.setEditScreenStrings()`.
    - To get user-edited fields, in your `onActivityResult(int requestCode, int resultCode, Intent data)` method call `BlinkCardEditResultBundle.createFromIntent(data)`.
    - This feature is available only for `BlinkCardRecognizer`.
    - If you are using a custom UI, you can launch edit screen by building intent with the following method `BlinkCardEditActivity.buildIntent()`.

- We updated the default BlinkCard scanning screen (`BlinkCardOverlayController`):
    - Instructions on how to reduce glare will be displayed when the user enables flashlight, you can disable it with `BlinkCardUISettings.setShowGlareWarning(false)`.
    - If the edit screen is enabled, a new button will show up after 5 seconds of unsuccessful scanning to allow the user to go directly to the edit screen.

### Other improvements:

- We have translated complete SDK to following languages: **Arabic(UAE)**, **Chinese simplified**, **Chinese traditional**, **Croatian**, **Czech**, **Dutch**, **English**, **Filipino**, **French**, **German**, **Hewrew**, **Hungarian**, **Indonesian**, **Italian**, **Malay**, **Portuguese**, **Romanian**, **Slovak**, **Spanish**, **Slovenian**, **Thai** and **Vietnamese**.
- We have improved recognition timeout logic when using `BlinkCardRecognizer`.
    - When a credit card has multiple sides to scan, the timeout timer for the second side starts after the second side of the card has been detected. Previously, it has been started immediately after the first side has been scanned.
    - Timeout duration can be configured by using `RecognizerBundle.setNumMsBeforeTimeout`

### Major API changes:

- To ensure compatibility with other Microblink SDKs, we have repackaged all classes. We have renamed the root package `com.microblink` to `com.microblink.blinkcard`, which is unique to `BlinkCard` SDK.
- `ScanResultListener` interface now has an additional method called when the scanning cannot continue because of an unrecoverable error. You have to implement the `onUnrecoverableError` method.
If you’re using built-in activities, when `onActivityResult` is called with `RESULT_CANCELED` result code, the exception will be available via `ActivityRunner.EXTRA_SCAN_EXCEPTION` intent extra. If the user canceled the scan, the exception would be `null`.

### Minor API changes:

- We have renamed old `BlinkCardRecognizer` and `BlinkCardEliteRecognizer` recognizers to `LegacyBlinkCardRecognizer` and `LegacyBlinkCardEliteRecognizer`. They are now deprecated.
- We removed `RecognizerRunnerView` custom attributes: `mb_initialOrientation` and `mb_aspectMode`. Use `RecognizerRunnerView.setInitialOrientation` and `RecognizerRunnerView.setAspectMode` to configure the attributes in the code.
- We renamed `RecogitionMode` to `RecognitionDebugMode` in `RecognizerBundle`.


### Bug fixes:

- We fixed race conditions in camera management, which in some cases caused that the camera was unable to resume after it had been paused.
- We fixed crash when using Direct API on high resolution `com.microblink.image.Image` from `HighResImageWrapper`


## 1.1.0

### Major API changes:

- SDK has been migrated to **AndroidX** dependencies - previous SDK dependency com.android.support:appcompat-v7 has been replaced with  **androidx.appcompat:appcompat**

### New features:

- `BlinkCardRecognizer` now extracts IBAN from the Payment / Debit card
- it is possible to set theme that will be used by activity, launched from the UISettings - use UISettings.setActivityTheme


### Improvements:

- improved camera performance on some Samsung devices

### Minor API changes:

- all scan activity classes are final now
- in `BlinkCardRecognizer` and `BlinkCardEliteRecognizer` results, `documentDataMatch` value is now returned as `DataMatchResult` enum with three possible values: `NotPerformed`,  `Failed` and `Success`
- new API for configuring camera options on `UISettings` -  use `UISettings.setCameraSettings`, which accepts object of `CameraSettings` type
- `BlinkCardOverlayController` does not use UI settings anymore, it uses `BlinkCardOverlaySettings`, you can create `BlinkCardOverlayController` from `BlinkCardUISettings` by calling method `uiSettings.createOverlayController`

### Bug fixes:

- fixed bug in `BlinkCardRecognizer`:
    - `anonymizeCvv` now works independently of any other anonymization setting
- fixed problems with aspect ratio of camera preview on Huawei Mate 10
- `BlinkCardActivity` correctly set volume to media instead of ring
- `BlinkCardActivity` now apply secure flag if enabled in ui settings

## 1.0.1

### Bug fixes:
- fixed scanning bug for devices with problematic camera resolution, which caused that SDK was unable to scan data, known affected devices were: `OnePlus 6T`, `OnePlus 7 Pro` and `Vivo V15`

## 1.0.0

- SDK for Android that enables you to perform scans of various credit or payment cards in your app
- for more information, see [documentation](https://github.com/microblink/blinkcard-android) and [sample applications](https://github.com/microblink/blinkcard-android/tree/master/BlinkCardSample)
