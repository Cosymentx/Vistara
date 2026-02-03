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

| ID | Task | Method / Strategy | Changes |
| :--- | :--- | :--- | :--- |
| **1.1** | **Package Restructure** | **Feature-Based Packaging**<br>Move from `com.obscura.wallpapers.*` to `com.obscura.wallpapers.features.*`. | `ui` -> `features` (Split by screen)<br>`data` -> `core.data`<br>`utils` -> `core.common` |
| **1.2** | **Build System** | **Dependency Swap & Update**<br>Replace libraries to change bytecode signatures. | Remove `Glide` -> Add `Coil`<br>Add `Kotlinx Serialization`<br>Update `Hilt` & `Compose` to latest. |
| **1.3** | **Application Entry** | **Total Rewrite**<br>Create new `VistaraApplication` inheriting `HiltAndroidApp`. | Rename `App` -> `VistaraApplication`<br>Change init order of SDKs.<br>Move `ActivityProvider` -> DI Graph. |
| **1.4** | **Main Activity** | **Navigation Host Rewrite**<br>Simplify `MainActivity` to a pure container. | Rename `MainActivity` -> `EntryActivity`<br>Remove `UnlockReceiver` logic from here (move to WorkManager/Service). |

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
