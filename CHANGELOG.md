# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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