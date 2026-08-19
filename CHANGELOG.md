# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## v0.24 - 2026-08-19

### Added
- Six new fitness exercises: `Skipping`, `Burpees`, `ShoulderTaps`, `HighKneeTaps`, `MountainClimbers`, `Boxing`
- `Style.relativePointSize` — scales the landmark dots drawn by `Feature.ShowPoints` (0.3 draws points at 30% of the default size)

### Changed
- Pushups now count when filmed head-on (front-facing) as well as side-on — the torso-orientation gate has been removed

## v0.23 - 2026-08-12

### Changed
- ONNX Runtime replaced by a pure-Kotlin tree-ensemble evaluator — identical predictions, no native dependency, roughly 110 MB smaller APK footprint

## v0.22 - 2026-07-15

### Added
- Overlay styling options on `Style`:
  - `lineCap` — `ROUND` (default), `BUTT`, `SQUARE`
  - `linePattern` — `SOLID`, `DASHED`, `DOTTED`
  - `shadow` — drop shadow / glow behind lines, points and labels
  - `outline` — contrasting border behind lines and points
  - `imageFill` — reveals a supplied `Bitmap` through the skeleton's lines, points and labels
  - `typeface` — custom `Typeface` for measurement labels
  - `letterSpacing` — label letter spacing in ems
- Front Pushups fitness exercise
- StylingDemo sample app — live menu for every overlay style option

### Changed
- Overlay lines now use round caps and joins by default (previously butt caps). Set `lineCap = Style.LineCap.BUTT` to restore.

## v0.21 - 2026-04-17

### Added
- `QuickPoseCameraSwitchView.captureFrame(callback)` — captures a composited camera + overlay screenshot. Knows which surfaces are active internally.

## v0.20 - 2026-04-17

### Added
- `QuickPose.captureFrame(camera, overlay, callback)` — static method to capture a composited camera + overlay screenshot via PixelCopy.

## v0.19 - 2026-04-15

### Added
- `Head` and `WholeBodyAndHead` landmark groups (matches iOS SDK) — draws ear-to-nose head connections and extends `WholeBody` with them

## v0.18 - 2026-03-01

### Fixed
- Camera stretching fixed in demo apps

## v0.17 - 2025-09-17

### Removed
- Stale Dependency restricting 16kb Pagesize support
- Requires quickpose-mp bump to 0.6

## v0.16 - 2025-09-08

### Added
- 16Kb Pagesize support


## v0.15 - 2025-08-10

### Added
- Archs for x86, x86_64, arm, arm64

### Fixed
- Stale Thread Crash


## v0.14 - 2025-05-22

### Fixed
- Stability fixes for Pixel

## v0.13 - 2025-05-18

### Fixed
- Minor Log noise

## v0.12 - 2025-05-18

### Changed
- Camera views accept a target camera resolution as input
- QuickPoseCameraView returns an aspect ratio for using in scaling view
- ComposeFunctionDemo shows aspect ratio corrected view

### Fixed
- Crash linked to fps
- Startup stability issues (for best results call AndroidAssetUtil.initializeNativeAssetManager(this) in your startup code)

## v0.11 - 2025-04-30

### Fixed
- Overlays/Lines can now be invisible with relativeLineWidth == 0
- Bundle Smaller by default contains only 'full' model.


## v0.10 - 2025-04-23

### Fixed
- Off main thread startup by default

## v0.9.0 - 2025-03-10

### Fixed
- Conditional colours for Measuring Angle.

## v0.8.0 - 2025-03-09

### Fixed
- Minor naming inconsistencies
- Key Invalid flickering

## v0.7.0 - 2025-03-09

### Fixed
- Camera views are public.
- Internal function exposed for QP camera to work.
- onFrame returns overlay as canvas.

## v0.6.0 - 2025-03-09

### Fixed
- Status align with iOS
- Fitness overlay aligns with iOS