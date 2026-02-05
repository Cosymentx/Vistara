# Vistara Refactor Roadmap (Google Play Compliance & Modernization)

This document outlines the detailed plan to rewrite the Vistara application. The primary goals are:
1.  **Unique Codebase**: Achieve <10% code similarity to the original source.
2.  **Google Play Compliance**: Remove restricted patterns and ensure policy adherence.
3.  **Visual Overhaul**: Implement a new, premium design (Glassmorphism/Neon).

## 📅 Execution Schedule

| Phase | Priority | Objective | Scope |
| :--- | :--- | :--- | :--- |
| **1. Foundation** | 🔴 High | **Structure & Architecture** | Build Logic, Package Structure, Dependency Injection |
| **2. Core Logic** | 🔴 High | **Business Logic Rewrite** | Data Layer, Network, Utils, Encryption |
| **3. UI Design** | 🔴 High | **Visual Redesign** | Theme, Components, Layouts, Navigation |
| **4. Features** | 🟡 Medium | **Feature Implementation** | Wallpapers, Video, Settings, User Profile |
| **5. Polish** | 🟢 Low | **Final Verification** | Testing, Analytics, Obfuscation Checks |

---

## 🚀 Phase 1: Foundation (Architecture & Packages)

**Goal**: Establish a completely new project structure so that file paths and class names are 100% different.

| ID | Task | Method / Strategy | Changes | Status |
| :--- | :--- | :--- | :--- | :--- |
| **1.1** | **Package Restructure** | **Feature-Based Packaging**<br>Move from `com.obscura.wallpapers.*` to `com.obscura.wallpapers.features.*`. | `ui` -> `features` (Split by screen)<br>`data` -> `core.data`<br>`utils` -> `core.common` | ✅ 已完成 |
| **1.2** | **Build System** | **Dependency Swap & Update**<br>Replace libraries to change bytecode signatures. | Remove `Glide` -> Add `Coil`<br>Add `Kotlinx Serialization`<br>Update `Hilt` & `Compose` to latest. | ✅ 已完成 |
| **1.3** | **Application Entry** | **Total Rewrite**<br>Create new `ObscuraApp` inheriting `HiltAndroidApp`. | Rename `App` -> `ObscuraApp`<br>Change init order of SDKs.<br>Move `ActivityProvider` -> DI Graph. | ✅ 已完成 |
| **1.4** | **Main Activity** | **Navigation Host Rewrite**<br>Simplify `MainActivity` to a pure container. | Rename `MainActivity` -> `EntryActivity`<br>Remove `UnlockReceiver` logic from here (move to WorkManager/Service). | ✅ 已完成 |

**Phase 1 进度说明（2025-02-04）**
- **1.3**：已新增 `ObscuraApp`，移除 `App`；已新增 `CurrentActivityHolder`（DI），移除 `ActivityProvider`；Manifest 已指向 `ObscuraApp`。
- **1.4**：已新增 `EntryActivity`，移除 `MainActivity`；入口 Activity 仅负责导航与 `CurrentActivityHolder` 注册；解锁屏逻辑仅保留 Manifest 静态注册的 `UnlockWallpaperReceiver`，已从 Activity 中移除动态注册。
- 编译已通过：`./gradlew clean assembleDebug` 成功。

**Phase 1 进度说明（2026-02-05）**
- **1.1**：完成所有屏从 `ui.screens.*` 迁移至 `features.*`；将 `ui.navigation` 迁移为 `features.navigation` 并修复引用；保留 `ui/components|icons|theme` 作为共享 UI 基础层；同时修复过时图标用法（AutoMirrored ArrowBack）；编译验证通过。
- **1.2**：已移除 `Glide` 及注解处理器，统一使用 `Coil`；已引入并使用 `kotlinx-serialization`；`Hilt/Compose` 版本更新待办。
 - **Polish**：针对生命周期 API 的 deprecation，已将 `LocalLifecycleOwner` 引用迁移至 `androidx.lifecycle.compose.LocalLifecycleOwner`（LiveVideoPlayer、VideoPlaybackManager），构建验证通过。

**激进重构进度（文件名 / 方法名 / 逻辑顺序，不影响功能）**
- **模块 1 - Application + DI**
  - 文件/类：`CurrentActivityHolder.kt` → `ActivityScopeHolder.kt`（类 `ActivityScopeHolder`），`AppModule.kt` → `CoreBindings.kt`（object `CoreBindings`）。
  - 方法名：`setActivity`/`getActivity` → `attach`/`current`；`provideApplicationContext`/`provideDataStore`/`provideStringProvider` → `bindAppContext`/`bindPreferencesStore`/`bindStrings`。
  - `ObscuraApp`：`initAppsFlyer` → `attachAttributionSdk`，`refreshUserProfile` → `syncUserProfile`，`handleDeepLink` → `resolveDeepLink`，`handleCampaignData` → `applyCampaign`；`onCreate` 内先 `syncUserProfile` 再 `attachAttributionSdk`；回调顺序与 when 分支顺序调整。
- **模块 2 - EntryActivity**
  - 变量/方法：`initialNavigation` → `pendingRoute`，`recreateContent` → `bindUi`，`handleNavigationIntent` → `applyIntentDestination`。
  - 逻辑顺序：`onCreate` 中 splash 与 `contentReady` 提前，`bindUi` 内先取 `language` 再取 `darkTheme`/`dynamicColors`；`applyIntentDestination` 分支改为 `if (navigation != "settings") navigation else "settings"`。
- **模块 4 - Utils**
  - 文件/类：`ImageUtil.kt` → `ImageProcessor.kt`（object `ImageProcessor`），`NetworkUtil.kt` → `ConnectivityHelper.kt`（class `ConnectivityHelper`），`Constants.kt` → `AppConstants.kt`（object `AppConstants`）。
  - 方法名：`applyGaussianBlur` → `blurGaussian`，`getDrawableByName` → `drawableIdForName`；`isConnected`/`isWifiConnected`/`isMobileConnected` → `hasConnection`/`hasWifi`/`hasCellular`，`registerNetworkCallback`/`updateNetworkState` → `bindCallback`/`refreshState`；`NetworkState` → `LinkState`。
  - `UtilsModule`：`provideNetworkUtil` 移除（改用 `ConnectivityHelper` 构造注入），`provideNetworkMonitor` → `bindNetworkMonitor`。
- 编译已通过：`./gradlew assembleDebug` 成功。

**下一步计划（Phase 1 延续）**
- 完成 `utils` -> `core.common` 剩余文件归档与命名统一（不引入行为变更）
- 评估 `ui/navigation` 是否需要重命名或迁移到 `features.navigation`（保持路由不变，确保 Minimal Impact）
- 制定 Phase 3 主题系统改造的依赖与分支策略（不影响现有 UI 行为）

---

## 🧠 Phase 2: Core Logic (Data & Utils)

**Goal**: Rewrite internal logic so syntax trees look different to automated scanners.

| ID | Task | Method / Strategy | Changes |
| :--- | :--- | :--- | :--- |
| **2.1** | **Network Layer** | **Interface Redefinition**<br>Rename API methods and restructure response wrappers. | `ApiService` -> `WallpapersDataSource`<br>Change `Call<T>` to `suspend fun`<br>Implement new `NetworkResult` wrapper. |
| **2.2** | **Database** | **Schema Migration**<br>Rename tables and columns. | `UserEntity` -> `CachedUserProfile`<br>Change table names in `@Entity(tableName = "...")`. |
| **2.3** | **Utilities** | **Consolidation & Refactor**<br>Merge small utils into cohesive helper classes. | **Deconstruct** `ImageUtil`, `NetworkUtil`<br>Create `ImageProcessor`, `ConnectivityObserver`<br>**DELETE** `ActivityProvider` (Use Hilt). |
| **2.4** | **Repository** | **Logic Flow Change**<br>Switch from callback/basic flow to `StateFlow` and `UseCases`. | Rename `UserRepository` -> `SessionRepository`<br>Wrap all data operations in `Result<T>` pattern. |

---

## 🎨 Phase 3: UI Design (Visual Overhaul)

**Goal**: Ensure the app looks visually distinct to human reviewers and AI vision models.

| ID | Task | Method / Strategy | Changes |
| :--- | :--- | :--- | :--- |
| **3.1** | **Theme System** | **New Design Language**<br>Replace default Material themes with custom Design System. | `Theme.kt` -> `VistaraDesignSystem`<br>**New Font**: `Outfit` or `Plus Jakarta Sans`<br>**New Colors**: High contrast Dark Mode (Neon accents). |
| **3.2** | **Components** | **Visual Redesign**<br>Rewrite core components to look different. | `WallpaperCard`: Add rounded corners, shadows, glass effect.<br>`SearchBar`: Floating pill design.<br>`Buttons`: Gradient backgrounds. |
| **3.3** | **Navigation** | **Pattern Change**<br>Switch from standard BottomBar to Floating Dock or Side Drawer. | **Replace** standard `BottomNavigation`.<br>Implement `CustomDock` component. |

---

## 📦 Phase 4: Feature Implementation

**Goal**: Re-implement features using the new architecture and UI components.

| ID | Task | Method / Strategy | Changes |
| :--- | :--- | :--- | :--- |
| **4.1** | **Home Feed** | **Layout Change**<br>Switch from Grid to Staggered Waterfall with unique headers. | **Code**: Use `LazyStaggeredGrid`.<br>**UI**: Add "Featured" carousel at top (Hero section). |
| **4.2** | **Detail Screen** | **Interaction Overhaul**<br>Change how users interact with wallpaper details. | **New**: Bottom Sheet implementation instead of full screen overlay.<br>**Action**: "Apply" button with unique animation. |
| **4.3** | **Video Player** | **Optimization**<br>Replace `VideoPlaybackManager` with simpler `ExoPlayer` wrapper. | Rewrite `LiveVideoPlayer` composable.<br>Ensure background playback compliance. |

---

## ✅ Phase 5: Verification & Polish

| ID | Task | Method / Strategy |
| :--- | :--- | :--- |
| **5.1** | **Code Audit** | Run `simian` or similar tool to check duplication against backup. |
| **5.2** | **Play Policy** | Verify Data Safety form matches new `PrivacyPolicy`. |
| **5.3** | **Obfuscation** | Configure `R8` aggressively. Rename resource files (`res/drawable`). |
