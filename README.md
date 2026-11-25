# Musique for Android

A native Android Spotify client with an Apple Music style interface.

Kotlin, Jetpack Compose, and a Rust [librespot](https://github.com/librespot-org/librespot) core for streaming.
Requires a Spotify Premium account.

## Building

CI builds the native library for each ABI and then the APKs on every push. The APKs are attached to each run as
`musique-apks`, and tags `v*` publish a GitHub release.

Locally:

```sh
# native library (needs Rust, cargo-ndk and Android NDK r29)
./buildLibrespot.sh        # or build-librespot.ps1 on Windows

# app
./gradlew assembleDebug
```

## Credits

Musique for Android started from [Outify](https://github.com/iTomKo/Outify) by iTomKo and builds on
[librespot](https://github.com/librespot-org/librespot). Licensed under the GNU GPL v3, see [LICENSE](LICENSE).
