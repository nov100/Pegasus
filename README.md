# Phase 1 – build modernisation (Gradle 8.9 / AGP 8.5.2 / JDK 17 / NDK r26 / compileSdk 35)

Done: AndroidX migration, `compile`->`implementation/api`, namespaces, Flurry tracking removed,
`once` replaced by local class, jcenter removed, NDK STL gnustl->c++_static, armeabi dropped.

Known open issues (expected on first build):
- Java compile errors against android-35 API surface (fix one by one).
- Native: Substrate hook layer only supports ARM32/x86 -> no arm64 yet (see gradle.properties `vaAbis`).
- targetSdk 28 so Android 15 will install it; runtime hooks still written for Android 8.
- Tencent jars in app/libs are 2017 builds (location picker / map); candidates for removal.
Build: `gradle.bat --no-daemon assembleDebug` (needs NDK 26.1.10909125: sdkmanager "ndk;26.1.10909125")

## Phase 2 (Android 9-15 runtime fixes, UNTESTED - no SDK build was possible)
- App: HiddenApiBypass exemptions before VirtualCore.startup (all processes).
- HCallbackStub: LAUNCH_ACTIVITY is gone on Android 9+; handle EXECUTE_TRANSACTION / LaunchActivityItem instead.
- InvocationStubManager: every hook is created/injected inside try/catch (log tag "VA++") so one broken hook can't kill the process.
- New ActivityTaskManagerStub (Android 10+): startActivity & co. are served by IActivityTaskManager.
- ActivityStack / ActivityManagerCompat: start/finish stub activity through IActivityTaskManager on Android 10+.
- StartActivity proxy: callingPackage is args[1]; Android 11+ callingFeatureId handled.
- Home: after adding an app, fall back to reloading the list on any failure; drag crash noise removed.

## Phase 2d (fix native SIGSEGV on app launch, from VA Log round 3 on real device)
- Symptom: `io.virtualapp:p0` crashed every time with SIGSEGV (fault addr ~0x9) inside
  `measureNativeOffset` -> `hookAndroidVM`, called from `NativeEngine.launchEngine` during
  `VClientImpl.bindApplicationNoCheck`. This happened on real Android 15 hardware (OPPO
  CPH2505), after the EXECUTE_TRANSACTION fix from phase2c had already fired correctly
  (confirmed: HCallbackStub.handleExecuteTransaction/handleMessage were in the crash stack,
  i.e. the Java-side activity-launch hook worked; the process just died one step later, in
  the native layer).
- Root cause: `measureNativeOffset` looks up `NativeEngine.nativeMark`'s jmethodID and scans
  raw memory near it for a pointer value, assuming an old Dalvik/early-ART `ArtMethod` layout.
  On this device/ART version that lookup can come back null (or the scan can fail to find the
  pointer), and the code did not check for that: it dereferenced memory at/near address 0,
  crashing the whole process (not just skipping the one native patch).
- Fix (`VMPatch.cpp`):
  - `measureNativeOffset`: if the `nativeMark` method id is null, log and return instead of
    dereferencing it.
  - Track whether the offset scan actually found a match (`patchEnv.native_offset_found`).
  - `hookAndroidVM`: if the scan did not find a match, skip installing the four hooks that
    depend on it (`getCallingUid` spoof, dex-open hook, camera setup hook, audio-record
    permission hook) instead of hooking through a bogus offset and crashing later.
- Net effect: on runtimes where this old offset-scanning trick no longer works (seen on
  Android 15), VirtualApp now loses those four specific native patches but the virtual
  process no longer SIGSEGVs, so activity launch should be able to proceed to the next stage.
  NOT YET TESTED ON DEVICE — only compiled by eye / reviewed; this sandbox has no NDK/CMake
  toolchain to actually build the .so, so this must be confirmed via GitHub Actions + VA Log.
- Still open: `VA++ cannot find function: __dl__Z9do_dlopenPKciPK17android_dlextinfoPv` in
  `IOUniformer.cpp::hook_dlopen` — this is already handled safely (symbol lookup failure is
  checked and the hook is just skipped, no crash), so it's logged as a lost feature
  (IO-redirection via dlopen hooking on the linker), not a bug to fix right now.

## Phase 2e (disable the native offset-scan hack entirely + fix add-app ClassCastException)
From VA Log round 4 on real device (Android 15, OPPO CPH2505, build phase2d):

1. **Still SIGSEGV on launch** — exact same crash as phase2c/2d, same fault addr 0x9, inside
   `measureNativeOffset`/`hookAndroidVM`, now against *different* apps (com.coloros.note,
   com.facebook.orca) so it's not app-specific. The .so was confirmed rebuilt (different
   BuildId each round), so phase2d's null-check on `nativeMark`'s method id did get compiled
   in, but it crashed anyway later in the same function. That means the raw-memory offset
   scan itself is unsafe on this ART build, not just "the method id can be null".
   - Fix: stopped calling `measureNativeOffset` at all on ART (`VMPatch.cpp hookAndroidVM`),
     gated behind `kEnableLegacyNativeOffsetHack = false`. This loses 4 native hooks that
     depended on it (calling-UID spoof, legacy Dalvik dex-open bridge, camera-setup hook,
     audio-record permission hook) — none of which are required to launch a virtual app's
     UI. Re-enable only after confirming on-device that the scan is safe for whatever it's
     needed for.
2. **Add-app ClassCastException** (separate bug, was already firing in earlier rounds too,
   just not flagged yet): `SpecialComponentList.protectIntentFilter` read
   `IntentFilter.mActions` assuming it's an `ArrayList<String>` (old Android). On Android 15
   it's actually an `android.util.ArraySet<String>` (a Set, not a List) -> ClassCastException
   inside `VAppManagerService.installPackage`, which likely explains the long-standing "app
   doesn't show in the list until you leave and re-enter" symptom (the install call throws,
   so the UI's success callback gets a null `InstallResult` -> NPE -> no immediate refresh).
   - Fix: `mirror/android/content/IntentFilter.java`'s `mActions` field re-typed from
     `RefObject<List<String>>` to `RefObject<Object>`; `SpecialComponentList.protectIntentFilter`
     now checks `instanceof List` (old path, unchanged) vs `instanceof Collection` (new
     ArraySet path: collects actions to remove/add since a Set has no `ListIterator`).
3. Still logged but harmless (already safe, no fix needed): `VA++ cannot find function:
   __dl__Z9do_dlopenPKciPK17android_dlextinfoPv` in `IOUniformer.cpp hook_dlopen` — symbol
   lookup failure is checked, hook is just skipped.
- Build marker bumped to "phase2e". NOT YET TESTED ON DEVICE (no NDK/network in this
  sandbox to compile) — needs a real GitHub Actions build + fresh VA Log.

## phase2f (from the phase2e device log)
phase2e passed the native crash (no more SIGSEGV; `hookAndroidVM` skips the legacy hack as intended).
The next failure is Java-level and happens for every cloned app (com.cloudmovieplayer.app, com.coloros.note):
1. `LoadedApk.registerAppInfoToArt` -> NPE "Attempt to read from null array" (LoadedApk.java:1220, Android 15).
   Cause: `PackageParserEx.initApplicationInfoBase` set `ai.splitSourceDirs = {apkPath}` but never
   `splitNames`; AOSP indexes `splitNames[i-1]` for every split. The NPE aborted `makeApplication`, which cascaded into
   `installProvider` NPE, `bindApplicationNoCheck` NPE (VClientImpl:343), "Target activity: Not found for token", and
   finally `ClientTransactionListenerController.onContextConfigurationPreChanged` NPE (app == null) killing the process.
   - Fix: on SDK >= N leave `splitSourceDirs`/`splitPublicSourceDirs` null (VA has no real split support; the base
     apk listed as a split was also wrong). < N behaviour unchanged.
2. `SpecialComponentList.<clinit>`: `WebViewFactory.getUpdateService` NPE (no Application yet). Was caught and only
   printed; now falls back silently to the well-known WebView package names.
- Build marker bumped to "phase2f". NOT TESTED ON DEVICE (no NDK/network here). Expect: log line 2 = phase2f and the
  `registerAppInfoToArt` trace gone. Any new error will be further along in bindApplicationNoCheck.

## phase2g (from the phase2f device log)
phase2f fixed registerAppInfoToArt: the app now reaches `Activity.attach`. New crash on every launch:
`SecurityException: Given calling package com.cloudmovieplayer.app does not match caller's uid` from
`IActivityManager.getContentProvider` (via PhoneWindow -> Settings.Global). On Android 10+ the signature is
`getContentProvider(caller, callingPackage, name, userId, stable)`; `GetContentProvider` read the name at index 1
(= callingPackage) so provider resolution never matched, and the virtual package name was passed to AMS.
- Fix: for SDK >= Q use name index 2 and overwrite `args[1]` with the host package.
- Build marker "phase2g". NOT TESTED ON DEVICE.
- Also seen, not fixed yet: `getTaskForActivity` NoSuchMethodException (signature changed), harmless so far.

## phase2h (from the phase2g device log)
All 56 FATALs in the log (guest `pladuk.pladuk.appvpn`) are the same crash:
`ClassCastException: String cannot be cast to Bundle` at `ProviderHook.invoke`, reached from `Settings.Secure.getInt`.
- Cause: Android 12+ `call(AttributionSource, authority, method, arg, extras)`; the hook read from index 1.
  - Fix: take method/arg/extras from the end of the argument list.
- Same class of bug, fixed preventively: other methods now locate args relative to the `Uri`;
  Android 11+ `delete`/`update` take a `Bundle` instead of selection strings.
- Preventive: `ExternalProviderHook` replaces a guest-package `AttributionSource` with a host one (Android 12+).
- Seen, not changed: `AutoFillManagerStub` inject NPE and `SpecialComponentList` WebView NPE at startup (both caught).
- Build marker "phase2h". NOT COMPILED against the Android SDK, NOT TESTED ON DEVICE.

## phase2i (from the phase2h device log, com.coloros.note)
- The phase2g `ProviderHook` ClassCastException does not appear in this (partial) log.
- Root error: `makeApplication` failed with "Found multiple conflicting per-domain rules"
  (`NetworkSecurityConfigProvider.handleNewApplication`). The later `installProvider` and
  `onContextConfigurationPreChanged` NPEs are consistent with the Application being null.
  - Fix: `ApplicationConfig.setDefaultInstance(null)` before `makeApplication`.
  - Logic checked against the AOSP change (e Foundation mirror), not the exact Android 15 / OPPO source.
- Now throws a clear IllegalStateException if `makeApplication` returns null.
- Build marker "phase2i". NOT COMPILED against the Android SDK, NOT TESTED ON DEVICE.
- 
