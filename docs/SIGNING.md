# Release signing

Create the key once and keep it safe. If you lose it you cannot update an installed release build; back it up outside the phone and the PC.

```
keytool -genkeypair -v -keystore cellwise-release.jks -alias cellwise -keyalg RSA -keysize 2048 -validity 10000
```
Put `cellwise-release.jks` outside the repo (or next to it; `*.jks` is git-ignored), then create `keystore.properties` next to `settings.gradle.kts` (git-ignored):
```
storeFile=../cellwise-release.jks
storePassword=...
keyAlias=cellwise
keyPassword=...
```
`app/build.gradle.kts` signs the `release` build type automatically when this file exists; debug builds are unaffected. Build: Android Studio > Build > Generate Signed App Bundle/APK, or `./gradlew assembleRelease`.
Debug and release builds have different signatures, so switching between them needs an uninstall (use Backup first).
