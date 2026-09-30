# Plural Star Legacy, Specification

Status: DRAFT 2026-09-29, rewritten the same day. Zach's answers: the people left behind are on 32-bit Android phones, Legacy carries everything Plural Star does, and it ships from GitHub only for now. Then two corrections that reshape it: a lot of Plural Star requires the New Architecture switch, so a straight copy cannot work, and Legacy lives in this folder and never touches the mobile repo. DECISIONS 1 to 7 are settled; DECISION 8 is open.

## 1. Purpose

PS Legacy is Plural Star for the people we left behind: 32-bit Android phones, running on React Native's old architecture.

The main app cannot reach them. It has shipped `arm64-v8a` only since the first source commit, and it is built on React Native 0.86, which runs on the New Architecture only.

## 2. Terms

- **Plural Star**, or **the main app**: `C:\AppDev\PluralStar`, `com.pluralspace.app`.
- **PS Legacy**: the app this spec defines, in `C:\AppDev\PluralStarLegacy`.
- **Old architecture**: React Native's Legacy Architecture (the bridge and the Paper renderer), `newArchEnabled=false`. React Native 0.81 is the last release that has it. From 0.82 the switch is ignored.
- **New Architecture**: Fabric, TurboModules and JSI, the only architecture from 0.82 on.
- **32-bit phone**: any Android device whose ABI list has `armeabi-v7a` and not `arm64-v8a`, including 64-bit chips running a 32-bit-only Android.

## 3. Who is in, who is out

In: 32-bit ARM phones on Android 7.0 (API 24) or newer.

Out: Android 5 and 6, 32-bit x86, old iPhones and iPads, old PCs.

## 4. Scope: everything

Every screen and feature of the current mobile app, including the friends network, mirrors, device linking and sync, and Cloud Services. What the main app leaves out (Medical today), Legacy leaves out too. Parity comes from sharing the main app's source (section 6), not from rewriting it.

## 5. Why a straight copy cannot work

Checked 2026-09-29 against the main app's installed packages. These parts of Plural Star run on the New Architecture only, and each gets an old-architecture replacement:

| Main app | Why it cannot run on the old architecture | PS Legacy |
| --- | --- | --- |
| `react-native` 0.86.0 | New Architecture only since 0.82; `newArchEnabled=false` is ignored | `react-native` 0.81.6, the last old-architecture release |
| `react-native-reanimated` 4.5.3 and `react-native-worklets` 0.11.3 (pulled in by Keyboard Controller, plus the worklets Babel plugin) | Reanimated 4 is New Architecture only | `react-native-reanimated` 3.19.5 and its own Babel plugin |
| `@lodev09/react-native-true-sheet` 3.11.9 (`src/components/Sheet`) | v3 is New Architecture only; its README sends old-architecture apps to v2 | 2.0.6, behind an adapter |
| `@shopify/flash-list` 2.3.2 (3 files) | v2 is New Architecture only by design | 1.8.3, behind an adapter |
| `react-native-notify-kit` 10.5.0 (3 files and a patch) | New Architecture only (TurboModules) | `@notifee/react-native` 9.1.8's old-architecture bridge (the library notify-kit forked, archived April 2026) running notify-kit's own native core, with the main app's patch, built from source (section 6.5) |

Everything else in the main app ships old-architecture code and carries over as it is: Keyboard Controller, Safe Area Context, SVG, Async Storage, Blob Util, Image Picker, Documents Picker, Share, Localize, Clipboard, Image Resizer and Image Editor have `paper` or `oldarch` sources; Get Random Values and Linear Gradient are plain modules. Their versions are pinned against React Native 0.81.6 at install.

React Native 0.81 also pins React 19.1, where the main app is on 19.2.8. Anything in the source that needs 19.2 or a React Native API added after 0.81 is caught by the type check (section 7) and handled in section 6.3.

## 6. Layout

Everything lives in `C:\AppDev\PluralStarLegacy`. The mobile repo is read, never written.

### 6.1 The project

`C:\AppDev\PluralStarLegacy` itself is the React Native 0.81.6 project, created with the official CLI (section 9, step 1), with this spec at its root:

- `android/gradle.properties`: `newArchEnabled=false`, `hermesEnabled=true`, `reactNativeArchitectures=armeabi-v7a,arm64-v8a`.
- Application ID `com.pluralspace.app.legacy`, home screen label "PS Legacy", the main app's icon, minSdk 24.
- The main app's permissions, foreground service declaration and release signing key.
- versionName and versionCode in lockstep with the main app.
- Licence: AGPL-3.0, the main app's `LICENSE` copied verbatim, since Legacy ships the main app's source. `android/notifee-core/` keeps its own Apache 2.0 licence, which AGPL-3.0 can include. `README.md` is Legacy's own.

### 6.2 Shared source

`src`, `App.tsx` and `index.js` are copies of the main app's, never edited in place. `app.json`, the configs, `legacy` and `android` are Legacy's own. Refresh after main-app changes, **your PC** (PowerShell):

```
robocopy C:\AppDev\PluralStar\src C:\AppDev\PluralStarLegacy\src /MIR
Copy-Item -Force C:\AppDev\PluralStar\App.tsx C:\AppDev\PluralStarLegacy\App.tsx
Copy-Item -Force C:\AppDev\PluralStar\index.js C:\AppDev\PluralStarLegacy\index.js
```

### 6.3 Adapters

`legacy/` holds one module per replaced library, each presenting the API the shared source already uses on top of the old-architecture version:

- `true-sheet.tsx`: True Sheet 3's `detents`, `header`, `scrollable`, `onDidPresent`, `onDidDismiss`, `present()` and `dismiss()` on top of True Sheet 2 (`sizes`, `onPresent`, `onDismiss`; the header is rendered above the content). True Sheet 2 fits the sheet to its content, so the content gets a minimum height of the detent (92% of the window) and the sheet stays the fixed height it is in the main app.
- `flash-list.tsx`: FlashList 2's `FlashListRef` type and `maintainVisibleContentPosition={{disabled: true}}` on top of FlashList 1, which would pass that object to the native scroll view.
- `notify-kit.ts`: everything from notifee, plus notify-kit's `TimestampTrigger` type with `repeatInterval`. The value itself reaches the native core through the notifee patch in section 6.4, so front-check reminders every N hours are the same exact alarms as in the main app.

`metro.config.js` sends the shared source's imports of `@lodev09/react-native-true-sheet`, `@shopify/flash-list` and `react-native-notify-kit` to those modules. The modules reach the real packages under the private names `true-sheet-v2` and `flash-list-v1`, which Metro and `tsconfig.json` map back, so nothing imports itself. `babel.config.js` uses Reanimated 3's plugin in place of the worklets plugin.

Nothing in `src` has needed changing, and the type check (section 7) is clean.

### 6.4 Patches

`patches/`, applied by patch-package on every `npm install`:

- `react-native-image-picker+8.2.1.patch`: the main app's, unchanged.
- `@notifee+react-native+9.1.8.patch`: notifee's trigger validator drops any field it does not know, so it now passes `repeatInterval` through, with notify-kit's checks (a repeating frequency and a positive whole number).
- `react-native+0.81.6.patch`: types only. React Native 0.81 declares `StyleSheet.absoluteFill` as a registered style, but at runtime it is a plain object (`StyleSheetExports.js`), and the main app spreads it in `SystemMapScreen.tsx`. The patch declares it as the object it is, as React Native 0.86 does.

### 6.5 Notification core

notifee's bridge normally loads a precompiled core (`core-202108261754.aar`, 2021) that cannot be patched. Its `build.gradle` uses a Gradle project named `:notifee_core` instead whenever one exists, so `android/notifee-core/` is that project: notify-kit 10.5.0's core Java, copied verbatim from the main app's installed package with the main app's patch already in it (`ForegroundService.java`, `ReceiverService.java`), plus its Room schemas and Apache 2.0 licence. The core's public API is unchanged from notifee's (every call the old bridge makes, the event listener, the init provider and the callback interface were checked), so Legacy gets notify-kit's native fixes and the main app's patch on the old architecture.

When the main app updates notify-kit or its patch, copy the core again, **your PC** (PowerShell), after an `npm install` in the main app:

```
robocopy C:\AppDev\PluralStar\node_modules\react-native-notify-kit\android\src\main\java\app\notifee\core C:\AppDev\PluralStarLegacy\android\notifee-core\src\main\java\app\notifee\core /MIR
robocopy C:\AppDev\PluralStar\node_modules\react-native-notify-kit\android\schemas C:\AppDev\PluralStarLegacy\android\notifee-core\schemas /MIR
```

### 6.6 Signing

The release build reads the main app's keystore and `keystore.properties` from `C:\AppDev\PluralStar` at build time, read only, so Legacy is signed with the same key.

## 7. Verification

1. The TypeScript check over the shared source and adapters, against React Native 0.81.6's types. Every error is fixed in `legacy` or `patches`, never in `src`.
2. Zach's release build.
3. The phone test pass in section 11.

## 8. Network, data and Cloud

Nothing on the wire changes. PS Legacy runs the same network, sync and Cloud code as the main app, so a Legacy phone and a main-app phone can be friends, link as devices of one system, and share one Cloud vault. On one phone the two are separate apps with separate storage.

## 9. Work, in order

1. Done 2026-09-29: Zach created the project with the React Native CLI (`init PSLegacy --version 0.81.6 --package-name com.pluralspace.app.legacy`). It was generated into an `app` subfolder and moved up to the root of `C:\AppDev\PluralStarLegacy` the same day, where Zach wants it.
2. Done 2026-09-29: Android settings, package list, Metro and Babel configs, adapters and the Image Picker patch, written by hand.
3. Done 2026-09-29: shared source copied in, verified identical to the main app's.
4. Done 2026-09-29 in a scratch copy: `npm install` with all three patches applying, a clean type check, and a full Android JavaScript bundle (5.7 MB) that contains notifee's bridge, True Sheet 2, FlashList 1 and the `repeatInterval` pass-through, and nothing from notify-kit. The native build (Java, Kotlin, Gradle) could not be run there.
5. **your PC**, the first native build:
   ```
   cd C:\AppDev\PluralStarLegacy
   npm install
   cd android
   .\gradlew.bat assembleRelease
   ```
   The APK lands at `android\app\build\outputs\apk\release\app-release.apk`.
6. The test pass in section 11.
7. The APK on a GitHub release with install steps.

## 10. Distribution

GitHub only, no Play listing for now (DECISION 2). Each release is the signed APK on a GitHub release of [ByHanyou/Plural-Star-Legacy](https://github.com/ByHanyou/Plural-Star-Legacy), the repo for this folder.

- **Installing.** The phone must allow installs from the browser or file manager used: "Unknown sources" on Android 7, the per-app "Install unknown apps" permission from Android 8. The release page carries the steps.
- **Updating.** Nothing updates Legacy automatically. A newer APK installed over the old one keeps the data only while the signing key never changes.
- **Who it is for.** Phones that cannot install Plural Star from Play.

## 11. Testing

On real phones before the first release: one 32-bit phone on Android 7 to 9, one Android Go phone or other 64-bit chip running 32-bit Android, and one ordinary 64-bit phone running the same APK (React Native issue #54150 recorded a 32-bit build crashing at start-up on a 64-bit phone).

Each runs the same pass: install from the APK, setup, members, a front switch, the front notification and a reminder, a friend and their front, a second linked device, Cloud link with the key derivation timed, a full export and import, then the next APK installed over it with nothing lost.

## 12. Risks

- React Native 0.81 no longer gets fixes from the React Native team, and notifee's bridge is archived. Legacy carries whatever they leave broken. The notification core itself is notify-kit's maintained one (section 6.5).
- The old bridge on notify-kit's core is a pairing nobody else ships. The APIs match call for call, but only the first native build and the phone test prove it.
- Every main-app change that uses a replaced library in a new way, or a React Native API newer than 0.81, needs adapter work before the next Legacy release. The type check finds them.
- 32-bit phones are the slowest Plural Star will run on. Cloud key derivation (pure-JavaScript Argon2id at 64 MiB) will take longer than on current phones and must keep showing progress.

## 13. Decisions

- DECISION 1: settled 2026-09-29. This SPEC.md in `C:\AppDev\PluralStarLegacy`. All Legacy work lives in this folder; the mobile repo is never edited for Legacy.
- DECISION 2: settled 2026-09-29, Zach: "No listing. Git only. At least for now. We can't afford another $25 fee." Then: "Then we might make a listing. Let's get it working first, though." For whenever Play is revisited: the $25 is a one-time fee per developer account with no per-app charge, and a new app on a personal account created after 13 November 2023 needs a closed test with 12 testers opted in for 14 days in a row.
- DECISION 3: settled 2026-09-29 (default). Application ID `com.pluralspace.app.legacy`. Once published, it can never change.
- DECISION 4: settled 2026-09-29 (default). "Plural Star Legacy" on its release page, "PS Legacy" on the home screen, the main app's icon.
- DECISION 5: settled 2026-09-29 (default). Version numbers in lockstep with the main app.
- DECISION 6: settled 2026-09-29 with DECISION 2. GitHub releases are the only channel for now.
- DECISION 7: settled 2026-09-29 (default). The main app does not change.
- DECISION 8: open. Which phones run the test pass in section 11.

## 14. Sources, checked 2026-09-29

- React Native 0.82, "A New Era" (New Architecture only; 0.81 is the last with the Legacy Architecture): https://reactnative.dev/blog/2025/10/08/react-native-0.82
- Support 64-bit architectures, Android Developers: https://developer.android.com/google/play/requirements/64-bit
- Speeding up your Build phase (default ABIs), React Native docs: https://reactnative.dev/docs/next/build-speed
- React Native issue #54150: https://github.com/facebook/react-native/issues/54150
- Get started with Play Console (the one-time $25 fee): https://support.google.com/googleplay/android-developer/answer/6112435
- App testing requirements for new personal developer accounts: https://support.google.com/googleplay/android-developer/answer/14151465
- Package READMEs in the main app's `node_modules` (Reanimated 4.5.3, True Sheet 3.11.9, FlashList 2.3.2, notify-kit 10.5.0), read 2026-09-29.
