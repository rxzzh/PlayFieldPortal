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
The separately assigned local 2.47-second transition clip combines PFP's own geometric mark with
PSP light/sound reference material. The PMF audio required private-stream demux; a video-only
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
