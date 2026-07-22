# Remove unsupported Accompanist dependencies

## Goal

Make the app build with the currently upgraded dependency set without downgrading
any library version.

## Scope

- Remove the invalid Accompanist dependency declarations and usages.
- Keep existing AndroidX `FlowRow` and Material pull-to-refresh implementations.
- Replace the detail screen's Accompanist system UI controller with AndroidX
  window/insets APIs while preserving transparent system bars and light icons off.
- Do not change unrelated dependency versions or application behavior.

## Implementation

Delete the Accompanist version and all five library aliases/dependencies. In
`WallpaperDetailScreen`, use `WindowCompat.getInsetsController(window, decorView)`
to set transparent status/navigation bars and `isAppearanceLightStatusBars` /
`isAppearanceLightNavigationBars` to `false`. Keep the existing lifecycle effect
that applies and restores immersive mode, adapting only its controller type and
calls.

## Verification

Run `:app:compileDebugKotlin` first. If dependency resolution succeeds, inspect
and fix subsequent compile errors one at a time, then run the relevant debug
assemble task. No dependency downgrade is permitted.
