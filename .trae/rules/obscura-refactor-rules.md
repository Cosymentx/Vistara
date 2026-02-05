# Refactor Governance Rules

**for Android Wallpaper App (Jetpack Compose + Hilt)**

------------------------------------------------------------------------

## 0. Highest Priority Principles (Non-negotiable)

1.  **No Feature Regression**
    -   All existing behavior must remain identical (UI, interactions,
        business logic, analytics, ads, permissions, etc.).
    -   No breaking changes are allowed.
2.  **Minimal Impact**
    -   Each refactor must target **one single feature/module only**.
    -   No "side optimizations" outside the declared scope.
3.  **Rollback Friendly**
    -   Every change must be a **self-contained, reversible commit**.
    -   Never mix multiple refactor goals in a single change.
4.  **Buildable & Verifiable**
    -   After every refactor:
        -   `./gradlew assembleDebug` must pass.
        -   Core flows must launch and work (Home → Preview → Settings).
        -   No crashes, no compilation errors.

------------------------------------------------------------------------

## 1. Mandatory AI Refactor Workflow

### STEP 1: Target Declaration

``` text
[REFACTOR TARGET]
Module/Feature:
Current Problems:
Refactor Goal:
Files Involved:
Out of Scope:
```

### STEP 2: Impact Analysis

``` text
[IMPACT ANALYSIS]
- New Classes:
- Deleted Classes:
- Renamed:
- Package Changes:
- Potential Risks:
```

### STEP 3: Implementation Rules

-   Only modify files declared in STEP 1.
-   No cross-module or unrelated cleanup.
-   No new third-party dependencies.

### STEP 4: Self Validation

``` text
[SELF CHECK]
✓ Build passes
✓ App launches
✓ Core flows reachable
✓ UI unchanged
✓ Resources resolved correctly
```

### STEP 5: Human Approval Gate

``` text
[WAIT FOR USER]
Do you accept this refactor? (Y/N)
```

------------------------------------------------------------------------

## 2. Architecture & Code Standards

### 2.1 Layered Architecture

    ui/
      screen/
      component/
    domain/
      model/
      usecase/
    data/
      repository/
      datasource/
    di/

### 2.2 Compose Rules

-   Prefer stateless composables.
-   All state must be hoisted.
-   UI must only render, no business logic.
-   `Modifier` must always be the last parameter.

### 2.3 Hilt Rules

-   Never create objects directly inside Composables.
-   All repositories must follow `Interface + Impl`.
-   DI modules must be named `XxxModule`.

------------------------------------------------------------------------

## 3. Refactor Scope Limits

  Item                 Limit

-------------------- --------------------------------

  Files modified       ≤ 10
  New classes          ≤ 5
  Deleted classes      ≤ 3
  Renames              Must provide old → new mapping
  Cross-package move   Requires full impact list

------------------------------------------------------------------------

## 4. Forbidden Changes

❌ Remove analytics, ads, or tracking\
❌ Change backend APIs or contracts\
❌ Rename existing resources\
❌ Alter app behavior or business rules\
❌ Change minSdk / targetSdk\
❌ Modify Proguard, signing configs, or permissions

------------------------------------------------------------------------

## 5. Mandatory AI Output Format

``` text
=== REFACTOR REPORT ===
Target:
Scope:
Risks:
Self Check Result:
Waiting for Approval
======================
```

------------------------------------------------------------------------

## 6. Recommended Advanced Guards

### 6.1 Test Coverage

-   Each refactored module should include **at least one unit test**
    (when feasible).

### 6.2 Complexity Limits

-   Single function ≤ 50 lines\
-   Cyclomatic complexity ≤ 10

### 6.3 Duplication Goal

-   Similar logic duplication \< 20% after refactor.

------------------------------------------------------------------------

## 7. Additional Recommended Rules

### 7.1 Regression Checklist

-   Home screen loads
-   Wallpaper list displays
-   Preview works
-   Download / Set wallpaper works
-   Settings screen opens

### 7.2 Change Log

-   Generate `refactor_log.md` for every refactor.

### 7.3 Feature Flags

-   Guard risky logic using `BuildConfig` or remote flags.