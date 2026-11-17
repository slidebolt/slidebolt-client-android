# Git workflow for slidebolt-client-android

This is a standalone Gradle multi-module product. It consumes SlideBolt only
through the public API and has no source dependency on the Go workspace.

Normal verification:

```bash
./gradlew check bddTest assembleDebug
```

Only the LAN-owned `slidebolt-ci` promotion job may create release tags and
publish signed APK/AAB artifacts. GitHub Actions is verification-only.
Signing credentials and live SlideBolt credentials never enter this repo.

