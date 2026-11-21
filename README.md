<p align="center">
  <img src="assets/branding/hodai-logo.png" alt="Hodai logo" width="160">
</p>

# Hodai

Hodai is a free music player for Windows, Android, macOS, and Linux. Open it, search for something, and press play. There is no account to create and nothing to set up first.

The name comes from hōdai (放題), the Japanese word behind phrases like "all you can eat". Hodai works the same way: a large catalogue of music that anyone can open and play, with no subscription in the way.

*Screenshots coming soon.*

## What it does

- Search and play songs, albums, artists, and playlists right away. The music source is configured before you ever open the app, so the first launch goes straight to music.
- Synced lyrics that follow along with the song.
- Downloads for offline listening, saved with their tags.
- Group Jam rooms, where friends on different devices listen to the same queue together.
- Your own playlists, plus a local library for audio files already on your device.
- No ads, no telemetry, and no data collection. Playback happens on your device.
- Plugins, if you want to connect other music sources. The plugin format is compatible with Spotube plugins.

## Download

All builds are on the [Releases page](https://github.com/andikachr/hodai/releases). Grab the file for your system.

### Windows

Download `Hodai-windows-x86_64-setup.exe` and run it. It installs for your user account, so you do not need administrator rights.

### Android

Download the APK and open it on your phone. The first time, Android will ask you to allow installs from your browser or file manager.

### macOS

Download `Hodai-macos-apple-silicon.dmg`, open it, and move Hodai to your Applications folder. Builds are for Apple Silicon Macs. Intel Macs are not supported, because the native build tools cannot cross-compile for them.

### Linux

Download `hodai-linux-<version>-<arch>.tar.xz`, extract it, and run the app inside. Package templates for AUR, Flatpak, Chocolatey, Homebrew, and WinGet live in the `packaging` folder of this repository.

### iPhone and iPad

There is no App Store version. The Releases page includes an IPA file, which you can install with a sideloading tool such as AltStore. It works, but plan on a few extra steps, and iOS asks you to refresh sideloaded apps periodically.

## Building from source

Hodai is a Kotlin and Compose Multiplatform app. Desktop builds are GraalVM native images.

### Prerequisites

- JDK 21, to run Gradle. The Gradle daemon JDK and GraalVM are provisioned automatically.
- Rust (`rustup` and `cargo`), for the library built through [Gobley](https://gobley.dev).
- Android SDK, for Android builds only.
- [ComposeNativeWebview](https://github.com/kdroidFilter/ComposeNativeWebview), which is not on Maven Central yet. Clone it at the commit pinned as `COMPOSE_WEBVIEW_COMMIT` in [spotube-release-binary.yml](.github/workflows/spotube-release-binary.yml) and run `./gradlew publishToMavenLocal` in it.
- On Linux, runtime libraries for VLC, GTK 3, and WebKitGTK 4.1. On Debian/Ubuntu:

  `sudo apt-get install libvlc5 vlc-plugin-base libwebkit2gtk-4.1-0 libgtk-3-0 libasound2 libgl1 libxtst6 libxi6 libxrender1`

  Fedora, Arch, and openSUSE use slightly different package names for the same libraries. On Ubuntu 24.04 and Debian 13, `libgtk-3-0` and `libasound2` are the `t64` variants, and apt resolves that on its own.

### Run in development

Desktop (JVM), the fastest edit and run loop:

```bash
./gradlew :composeApp:run
```

Android debug APK:

```bash
./gradlew :composeApp:assembleStableDebug
```

Use `:composeApp:assembleNightlyDebug` for the nightly channel. Development builds skip the release-only optimizations below.

### Build the desktop app

A native build takes about 10 minutes, uses every CPU core, and peaks at around 15 GB of RAM. It only happens for the tasks below, never for `run`.

Native toolchain per OS:

| OS      | Requirements |
| ------- | ------------ |
| Linux   | `gcc`, zlib headers, `patchelf`; `rpm` for .rpm, `fuse` and `zstd` for AppImage. Debian/Ubuntu: `sudo apt-get install build-essential zlib1g-dev patchelf rpm fuse zstd` |
| macOS   | Xcode Command Line Tools (`xcode-select --install`) |
| Windows | Visual Studio Build Tools with "Desktop development with C++" |

Build the app folder (Nucleus downloads GraalVM CE on the first build; set `GRAALVM_HOME` to use your own):

```bash
./gradlew :composeApp:createGraalvmNativeDistributable
```

Output: `composeApp/build/compose/binaries/main/graalvm-app/`. Build and launch in one step with `:composeApp:runGraalvmNativeDistributable`.

Installers are built per OS, and each OS builds only its own:

| OS               | Command |
| ---------------- | ------- |
| Linux            | `./gradlew :composeApp:packageGraalvmDeb :composeApp:packageGraalvmRpm` |
| Linux (AppImage) | `./gradlew :composeApp:packageGraalvmAppImage -PappImage=true` |
| Windows          | `./gradlew :composeApp:packageGraalvmExe` (or `packageGraalvmMsi`) |
| macOS            | `./gradlew :composeApp:packageGraalvmDmg` |

Output: `composeApp/build/compose/binaries/main/graalvm-<format>`. Add `-Pnightly=true` for the nightly channel, which uses separate icons and name.

If a native build fails with reflection or JNI errors while `:composeApp:run` works, the reachability metadata needs an update: run `./gradlew :composeApp:runWithNativeAgent`, use the broken feature, close the window normally, and commit the updated `composeApp/graalvm/reachability-metadata.json`. Keep the hand-written `uk.co.caprica.vlcj.binding.lib.LibVlc` entry in that file, or playback fails to initialize.

Production mode turns on automatically for `package*`, `create*`, `notarize*`, and `nativeImageCompile` tasks. It switches the Rust library to its release profile and the desktop app to a size-optimized native image. Force it either way with `-Pproduction=true|false`.

### Releases

The `spotube-release-binary.yml` workflow runs manually (`workflow_dispatch`) as stable, beta, or nightly. Beta is the stable build as a draft release, tagged `v<version>-beta.N`, with `betaNumber` set in `composeApp/build.gradle.kts` and back to `null` for stable. Desktop platforms build their own installers on their own runners, and Android ships a signed APK.

## Credits

Hodai is built on [Spotube](https://github.com/team-spotube/spotube), an open source music client created by Kingkor Roy Tirtho and the Spotube contributors. The playback and plugin engine comes from their work, and Hodai keeps the same license. Thank you to everyone who has contributed to Spotube.

## License

Hodai is open source under the [AGPL-3.0-or-later](LICENSE) license.
