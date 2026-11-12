# PSP fidelity pass

This branch keeps the custom-font work and improves the default launcher presentation.

## Implemented

- Eight hand-authored Android vector category glyphs, proportioned against locally decoded PSP
  6.61 textures. Separate faces and gradients preserve highlights instead of flattening the art
  to a white silhouette. User picks and theme icons still take precedence; explicit color tints
  remain available.
- Offset-shadow mode now draws a blurred silhouette behind a sharp glyph. Contour modes retain
  their original single-node rendering. Selected XMB text uses a tighter directional shadow.
- Background motion samples the source model's X-tilt and morph timing: 1500 frames at 30 fps,
  a 50-second loop. The surface is an analytic three-fold AGSL/Canvas reconstruction, not the
  original GMO mesh or an emulated firmware program. It retains reduced/static motion and
  wallpaper precedence.
- VSYNC frame buckets replace a post-VSYNC sleep that could turn a nominal 30 fps into about
  20 fps on a 60 Hz panel.
- Ordinary apps and games share `LaunchPresentation`, implemented by the existing bounded
  GameBoot gate. Missing app targets do not show a transition; repeated taps do not enqueue
  app launches; usage and presence update after Android accepts the launch. The setting is
  exposed as **Launch Transition** under Display.

## Local reference material

The PSP 6.61 updater was downloaded from Sony's public original endpoint. MD5:
`6be8878f475ac5b1a499b95ab2f7d301`; SHA-256:
`dc23a6dabdaed40bbfaad811d1f170346b911ba9750478fe5928d9f10c94f552`.

The local evaluation directory, outside this repository, contains the firmware, RCO dumps,
PSAR/RLZ/GIM conversion tools, decoded PNGs, and audio. Those files are not bundled or committed.
The device's user-media tier contains four extracted PSP cues (navigation, cancel, decide, error).
The separately assigned local 2.47-second transition clip contains the light/sound reference
material without launcher lettering; the user deferred a personal logo. The PMF audio required private-stream demux; a video-only
conversion misses that track. The logo must also be supplied separately: the original PMF alone
contains only the light pass.

Public builds retain the existing authored sound defaults. Local media can be reset in Settings.
The eight new category paths are a vector redraw, not the extracted Sony raster payloads.

## Validation

Targeted JVM/Robolectric checks: 54 passed, zero failures:

- Icon matte / category override resolution: 21
- Custom font inheritance / UI media defaults: 19
- Wave timing, loop boundary, pose samples, crest bounds: 6
- App launch ordering, duplicate presses, missing target, failure recovery: 3
- Existing GameBoot gate behavior: 5

Lite debug assembles successfully. On-device verification and backup details are recorded in
`pfp-trial/status.json` and `pfp-xmb-evaluation/` in the parent workspace.
The earlier font-color CI failure was not part of this targeted run; this does not claim a full
repository test-suite pass.

References: [PPSSPP XMB documentation](https://www.ppsspp.org/docs/getting-started/how-to-run-the-xmb/),
[RCOMage](https://github.com/kakaroto/RCOMage),
[pspdecrypt](https://github.com/John-K/pspdecrypt),
[XMB GMO converter](https://github.com/mills32/Playstation-Portable-XMB-3D-model-converter).

## Game list controls

Confirm/A and one touch on a real game now launch directly by default. X opens Game Detail
without auto-launching; its former Games Filter binding is retired. X is hidden on game
folders/placeholders, while non-gaming columns retain their sorting shortcut. The status chip
and Options remain separate entry points for list arrangement. Folder touch navigation and
multi-select retain their existing behavior.

The existing Direct Launch compatibility toggle is retained; the device is explicitly set on.
Targeted controls regression: 73 tests passed (4 game-action cases, 38 contextual hints,
26 input-mapping cases, 5 launch-gate cases).


### Menu return and launch identity (2026-11-12)

Drill navigation clears outgoing rows in the same state update that restores the parent cursor,
and increments the landing token. The incoming list snaps to that cursor even when StateFlow
conflates the empty loading frame. Touch rows carry their rendered target identity instead of
only an index: a Room reorder finds the same target, a removed target is ignored, and the fixed
`umd_slot` row cannot activate a different inserted game. Controller activation snapshots the
row before updating the cursor.

Network uses a less crowded globe; the new `ic_apps` grid is a general app category glyph and
bypasses console-art routing. User/theme icon overrides retain their priority. On the test
handheld only, UMD is explicitly inserted with Steam Link and uses INSERTED mode rather than
recent-game fallback; Bilibili belongs to Network. Steam Link ICON0 is a centered authored SVG
card; its matching menu-safe background was generated with the built-in image generator.
These personal device assets and the private configuration backup are not bundled in source.

#### PPSSPP task reuse found on the handheld

An actual cross-launch reproduced a second issue: PFP resolved the selected Lenneth URI, but
Android brought forward the top Patapon activity from PPSSPP's old task. The installed 1.17.1
also only reads the shortcut during native initialization. PPSSPP 1.20.4 handles runtime ROM
switches in `onNewIntent`; ACTION_VIEW intents for the Free/Gold packages now carry CLEAR_TOP
and SINGLE_TOP alongside NEW_TASK and their read grant. Other emulators and native apps retain
their recipes. This relies on a PPSSPP build supporting runtime shortcut messages (verified
in 1.19.3 and 1.20.4); it does not kill the emulator or clear its data.

On the handheld, the old 1.17.1 APK and external memstick (44 files) were copied before updating
to the official 1.20.4 APK. Both APKs verify with the same SHA-256 signing certificate
`c5342629df16762391907b3297aad122b4c1a8a9e608dfb94217a08e3bbf13a2`.

References:
- [PPSSPP 1.17.1 shortcut handling](https://github.com/hrydgard/ppsspp/blob/v1.17.1/android/src/org/ppsspp/ppsspp/PpssppActivity.java)
- [PPSSPP 1.20.4 runtime shortcut handling](https://github.com/hrydgard/ppsspp/blob/v1.20.4/android/src/org/ppsspp/ppsspp/PpssppActivity.java)
- [Official Android APK downloads](https://www.ppsspp.org/download/)
- [Android activity flags](https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_CLEAR_TOP)

Validation for this revision: 74 targeted JVM/Robolectric checks passed across menu motion,
secondary actions, row identity, category icon resolution, and emulator intent recipes. Device
video shows Favorites returning directly to its remembered root row. Steam Link launched to its
own activity; Lenneth and Yuusha or2 emitted their matching native PPSSPP BOOT paths, including
runtime switches in the same PPSSPP process. All 33 pre-existing save-data files matched their
pre-upgrade bytes; only the non-save `.nomedia` markers were recreated by PPSSPP. The test does
not establish full gameplay compatibility for the entire library.


### Apps, system status and menu rendering (2026-11-12)

The personal handheld configuration now orders visible columns as Settings, Photo, Music, Game,
Apps, App Store, Network. Chrome, Bilibili HD, Clash VPN and YouTube are explicitly assigned to
Apps. Their landscape ICON0 cards and backgrounds use the actual installed/official brand art.
Steam Link remains the fixed UMD insertion; its main image is a real Valve Steam Deck press
photo, while its previously selected background remains. Device artwork, ROMs, font and private
media are not bundled in this repository.

The status strip derives VPN presence and system HTTP/PAC proxy configuration from lifecycle-
scoped ConnectivityManager callbacks and the proxy-change broadcast. It distinguishes VPN ON,
PROXY ON, PROXY OFF and unknown. This reports local OS configuration, not internet reachability
or the health of a particular proxy service. It never starts a VPN or probes an external site.
Battery state is initialized from the sticky system broadcast and refreshed on resume; missing
or malformed level/scale data retains the last valid reading rather than inventing zero.

Category changes now reuse cached root rows until the current Room result arrives. The item
column has no category-entry vertical animation or outgoing/incoming double tree. Category
icon size is measured once; animation state is read in placement/graphics layers, avoiding
per-frame recomposition and shadow texture regeneration. The established spring timings remain; a shorter tween experiment did not improve overall
frame delivery on the handheld. The wave shader uses an analytic crest bound to clip its invisible upper region before
fragment shading, then skips transparent samples inside the remainder. A half-resolution offscreen experiment was rejected after it increased composition
cost on the Odin2 Mini; the final wave remains full resolution. Offset icon/text shadows are
broader, denser and lower, using the locally inspected reference masks as a visual guide.

Settings > System > Memory & Background Apps exposes the OS cached-process cleanup API, displays
actual available memory and reports requested packages rather than claiming process deaths.
It excludes Home, persistent and system-UID processes. Android may retain previous/protected
apps; this is not a guaranteed force-stop. Android 14+ disables cross-app cleanup and offers the
system App Settings entry instead. No privileged helper, root or Shizuku requirement is added.

Validation for this revision: 128 targeted JVM/Robolectric checks passed with zero failures.
Lite debug builds successfully. Controlled on-device Photo/Music frame measurements, rejected
experiments, screenshots and the private final backup are recorded under the parent workspace's
`pfp-revisions/apps-proxy/` and `pfp-backups/2026-11-12-apps-proxy-final/`. Frame measurements are a
single controlled debug-build menu scenario, not a full application or game-performance claim.

The final paired Photo/Music sample recorded 319 baseline and 329 revised frames: P50 14 -> 11 ms,
P90 36 -> 30 ms, P95 69 -> 32 ms and P99 89 -> 73 ms. Deadline-missed frames were 16 (5.02%) and
19 (5.78%), respectively: the long-tail frame times improved, but this sample does not establish
a lower jank rate. The last 120 frame records show UI scheduling/work-to-sync P90 31.97 -> 15.32 ms;
GPU P90 remained 14 ms. The final manifest-only rebuild preserves the measured rendering code.
