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
