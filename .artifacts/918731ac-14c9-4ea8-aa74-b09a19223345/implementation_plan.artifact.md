# Complete Localization and String Externalization

This plan aim to externalize all hardcoded strings in the project to `strings.xml` and ensure all languages follow the settings in the app.

## User Review Required

> [!IMPORTANT]
> I will be updating almost all UI files to replace hardcoded text with `stringResource(R.string...)`. This is a large change but necessary for full localization support.

## Proposed Changes

### [Resources]

#### [MODIFY] [strings.xml](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/res/values/strings.xml) (and all other translations)
- Add missing strings for Onboarding, Audit, Safety Tips, Device Library, Privacy Policy, and Terms of Service.
- Add strings for content descriptions and toast messages.

### [UI Components]

#### [MODIFY] [ScannerScreen.kt](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/java/com/glassesscan/app/ui/scanner/ScannerScreen.kt)
#### [MODIFY] [HistoryScreen.kt](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/java/com/glassesscan/app/ui/history/HistoryScreen.kt)
#### [MODIFY] [WelcomeScreen.kt](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/java/com/glassesscan/app/ui/onboarding/WelcomeScreen.kt)
#### [MODIFY] [AuditScreen.kt](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/java/com/glassesscan/app/ui/audit/AuditScreen.kt)
#### [MODIFY] [SafetyTipsScreen.kt](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/java/com/glassesscan/app/ui/settings/SafetyTipsScreen.kt)
#### [MODIFY] [DeviceLibraryScreen.kt](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/java/com/glassesscan/app/ui/settings/DeviceLibraryScreen.kt)
#### [MODIFY] [PrivacyPolicyScreen.kt](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/java/com/glassesscan/app/ui/settings/PrivacyPolicyScreen.kt)
#### [MODIFY] [TermsOfServiceScreen.kt](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/java/com/glassesscan/app/ui/settings/TermsOfServiceScreen.kt)
#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Restu/AndroidStudioProjects/GlassesScan/app/src/main/java/com/glassesscan/app/ui/settings/SettingsScreen.kt)

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure all resource references are valid.

### Manual Verification
- Deploy the app to the device and test language switching in the Settings menu.
- Verify that all screens (especially the ones mentioned above) update their text correctly.
