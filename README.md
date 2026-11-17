# SlideBolt Android clients

Data-driven phone/tablet and Android TV clients for SlideBolt. Both applications
discover `_slidebolt-api._tcp`, use the code-pairing API, store credentials with
Android Keystore, and render devices, entities, state, and advertised commands
without plugin-specific branches.

Both shells permit cleartext traffic because an NSD-advertised installation
may use HTTP on a trusted local network. Remote installations should advertise
HTTPS; bearer tokens are never placed in URLs or logs.

## Modules

- `app-phone`, `app-tv`: installable Compose applications.
- `core-model`, `core-client`, `core-session`: platform-free contracts, API,
  WebSocket, pairing, and multi-installation state.
- `platform-android`: Android NSD and encrypted credential persistence.
- `feature-browser`: shared connection, device, entity, control, and diagnostic UI.
- `acceptance`: Cucumber scenarios against a real HTTP client and fake server.

## Build

Requires JDK 17 and Android SDK platform 37. Runtime behavior remains targeted
at stable Android 16 / API 36:

```bash
./gradlew check bddTest assembleDebug
```

Debug APKs are written beneath each app module's `build/outputs/apk` directory.

## What is tested without a device

Host tests prove pairing, authentication, generic decoding, sessions, and BDD
journeys. Android builds prove both APKs. Emulator jobs can prove Compose,
lifecycle, manual URL connection, and Keystore behavior. A physical Wi-Fi device
is still the final authority for Android `NsdManager` multicast behavior.

## Live verification

The local Jenkins service stores the admin credential and runs:

```bash
SLIDEBOLT_API_URL=https://example.test/core \
SLIDEBOLT_ADMIN_TOKEN=... \
scripts/live-verify.sh
```

It creates and approves a disposable pairing, checks authenticated inventory,
revokes the temporary token, and proves it can no longer be used. The script
never sends entity commands and never prints credentials.

