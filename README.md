<h1 align="center">Plural Star Legacy</h1>

<p align="center">
  <img src="https://raw.githubusercontent.com/ByHanyou/Plural-Star/main/docs/icon.png" width="120" alt="Plural Star icon" />
</p>

<p align="center">
  <strong>Plural Star for 32-bit Android phones.</strong><br>
  Every feature. Private. Offline-first.
</p>

<p align="center">
  <a href="https://github.com/ByHanyou/Plural-Star-Legacy/releases/latest">
    <img src="https://img.shields.io/badge/GitHub-Download%20APK-DAA520?style=for-the-badge&logo=android&logoColor=white" alt="Download the APK from GitHub" />
  </a>
</p>

<p align="center">
  <a href="https://www.buymeacoffee.com/PluralStar">
    <img src="https://img.buymeacoffee.com/button-api/?text=Support+PS&amp;emoji=%E2%98%95&amp;slug=PluralStar&amp;button_colour=151929&amp;font_colour=ffffff&amp;font_family=Cookie&amp;outline_colour=ffffff&amp;coffee_colour=FFDD00" alt="Support Plural Star on Buy Me a Coffee" />
  </a>
  &nbsp;
  <a href="https://discord.gg/FFQw33cu8m">
    <img src="https://img.shields.io/badge/Discord-Join%20Us-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Join our Discord" />
  </a>
</p>

<p align="center">
  <a href="https://byhanyou.github.io/Plural-Star/">Privacy Policy</a>
</p>

---

Plural Star Legacy is for the people the main app left behind. [Plural Star](https://github.com/ByHanyou/Plural-Star) only installs on 64-bit Android phones, so on a 32-bit phone Google Play hides it and the APK refuses to install. PS Legacy is the same app, with every feature, built to run on those phones.

It is not a cut-down version. It runs the same source code as Plural Star on React Native's older architecture, the one 32-bit phones can run, with replacements for the few parts of the main app that only work on the newer one.

Made in part with AI assistance, and open source for the same reason as the main app: so anyone who wants to, or has concerns, can examine the code.

## Is this the right app for you?

Use PS Legacy if your phone:

- runs Android 7.0 or newer, **and**
- can't install Plural Star: Google Play doesn't list it for your phone, or the APK fails with `INSTALL_FAILED_NO_MATCHING_ABIS`.

That includes many budget and Android Go phones that have a 64-bit chip but run a 32-bit version of Android. If Plural Star installs on your phone, use Plural Star.

## Features

Everything Plural Star has. See the [Plural Star README](https://github.com/ByHanyou/Plural-Star#features) for the full descriptions.

- Three-tier front tracking (Primary Front, Co-Front, Co-Conscious) with mood, location, notes and energy, a persistent front notification, and front-check reminders
- Observatory Mode for the singlets in our lives
- Member profiles with pictures, banners, markdown bios, tags, groups, Facets, Custom Fronts and the Archive
- System Manager, Custom Fields, Mailbox, System Polls and the System Map
- Friends & Syncing: end-to-end encrypted friends and linked devices
- Cloud Services *(Experimental)*
- Day Planner, Whiteboard, System Chat and the System Journal
- Front and member history, retroactive entries and System Statistics
- Import from Simply Plural, PluralKit, Octocon, Ampersand, Ourcana, HiveMind, Tupperbox, Parallax, PluralLog and PluralSpace, and full export and restore
- 24 languages, themes, custom colors, adjustable text size and OpenDyslexic
- The same screen reader and accessibility support as Plural Star

PS Legacy speaks the same network as Plural Star. A PS Legacy phone and a Plural Star phone can be friends, link as devices of one system, and share one Cloud vault.

## How it differs from Plural Star

- **Where to get it.** GitHub releases only, for now. There is no Google Play or App Store listing.
- **Updates.** Nothing updates PS Legacy automatically. Install the newer APK over the old one and your data stays.
- **It is a separate app.** On your home screen it is **PS Legacy**, and it keeps its own data. To move a system between Plural Star and PS Legacy, use export and import, link the devices, or use Cloud Services.
- **Speed.** 32-bit phones are the oldest phones Plural Star runs on. Large systems and linking Cloud Services (deriving your key) take longer than on a newer phone.

## Privacy

Exactly the same as Plural Star. Everything lives on your device: no accounts, no tracking, no ads. The only outbound requests are the optional ones listed in the [Plural Star privacy section](https://github.com/ByHanyou/Plural-Star#privacy), and Friends & Syncing and Cloud Services seal your data on the device before it leaves.

Full privacy policy: [https://byhanyou.github.io/Plural-Star/](https://byhanyou.github.io/Plural-Star/)

---

## Installation

1. Download the latest APK from [Releases](https://github.com/ByHanyou/Plural-Star-Legacy/releases/latest).
2. Allow the install when Android asks:
   - **Android 7:** Settings, Security, turn on **Unknown sources**.
   - **Android 8 and newer:** allow **Install unknown apps** for the browser or file manager you downloaded with.
3. Open the APK and tap **Install**.

**Updating:** download the new APK and install it over the old one. Your data is kept.

---

## Build from Source

```bash
# Requirements:
# - Node 20+
# - JDK 17
# - Android SDK with NDK 27.0.12077973
git clone https://github.com/ByHanyou/Plural-Star-Legacy.git
cd Plural-Star-Legacy
npm install
```

**Android release APK**

```bash
cd android
./gradlew assembleRelease
```

The APK lands at `android/app/build/outputs/apk/release/app-release.apk`.

Release builds are signed with the Plural Star release key, which is not in this repository. To build your own, point `signingConfigs.release` in `android/app/build.gradle` at your own keystore.

**How the project fits together**

- `src`, `App.tsx` and `index.js` are copied unchanged from Plural Star and never edited here.
- `legacy/` holds small adapters that give the shared code the same APIs it uses in Plural Star, on top of the older libraries this build needs.
- `patches/` holds the fixes applied to installed packages on every `npm install`.
- `android/notifee-core/` is the native notification core from react-native-notify-kit, built from source.

The full design, and the steps for bringing in changes from Plural Star, are in [SPEC.md](SPEC.md).

---

## License

[GNU Affero General Public License v3.0](LICENSE)

This software is free and open source. You are free to use, modify, and distribute it under the terms of the AGPL-3.0 license. Any distributed modifications or network-accessible deployments must also be released under AGPL-3.0.

`android/notifee-core/` is the notification core of [react-native-notify-kit](https://github.com/marcocrupi/react-native-notify-kit), originally Notifee by Invertase, used under the [Apache License 2.0](android/notifee-core/LICENSE).

---

## Support

Plural Star is free, always, and so is PS Legacy. If it's been useful to you, a contribution helps cover development time.

<a href="https://www.buymeacoffee.com/PluralStar">
  <img src="https://img.buymeacoffee.com/button-api/?text=Support+PS&amp;emoji=%E2%98%95&amp;slug=PluralStar&amp;button_colour=151929&amp;font_colour=ffffff&amp;font_family=Cookie&amp;outline_colour=ffffff&amp;coffee_colour=FFDD00" alt="Support Plural Star on Buy Me a Coffee" />
</a>

---

## Contact

**The Hanyou System**  
[Discord](https://discord.gg/FFQw33cu8m) · [r/PluralStar](https://www.reddit.com/r/PluralStar/) · [GitHub Issues](https://github.com/ByHanyou/Plural-Star-Legacy/issues)
