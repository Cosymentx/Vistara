# Vistara

Vistara 是一个基于 Jetpack Compose 的 Android 壁纸应用，支持静态/动态壁纸浏览、搜索、收藏、下载、编辑、登录和订阅/钻石购买。

## 项目概览

- Application ID：`com.vistara.aestheticwalls`
- Namespace：`com.vistara.aestheticwalls`
- 当前版本：`1.0.1`（versionCode `2`）
- 最低 Android 版本：API 24
- Target SDK：API 36
- Compile SDK：API 37
- Java 编译目标：Java 21
- 构建工具：Gradle Wrapper 9.6.1、Android Gradle Plugin 9.3.0

## 主要技术栈与版本

版本来源是 `gradle/libs.versions.toml`；Compose 组件未单独声明版本时由 Compose BOM 统一管理。

| 类别 | 库 | 版本 |
| --- | --- | --- |
| Kotlin | Kotlin / Compose Compiler Plugin | 2.4.10 |
| Android 构建 | Android Gradle Plugin | 9.3.0 |
| AndroidX 核心 | core-ktx | 1.19.0 |
| 生命周期 | lifecycle-runtime-ktx / lifecycle-viewmodel-compose | 2.11.0 |
| Activity | activity-compose | 1.13.0 |
| Compose | Compose BOM | 2026.06.01 |
| Compose UI | ui / ui-graphics / tooling | BOM 管理；tooling 1.11.4 |
| Material | material / material3 / material-icons-core | BOM 管理；icons 1.7.8 |
| Navigation | navigation-compose | 2.9.8 |
| ConstraintLayout | constraintlayout-compose | 1.1.1 |
| Hilt | hilt-android / compiler | 2.60.1 |
| Room | room-runtime / room-ktx / compiler | 2.8.4 |
| DataStore | datastore-preferences | 1.2.1 |
| WorkManager | work-runtime-ktx | 2.11.2 |
| Retrofit | retrofit / converter-gson | 3.0.0 |
| OkHttp | okhttp / logging-interceptor | 5.4.0 |
| Coil | coil-compose | 2.7.0 |
| Glide | glide / compiler | 4.16.0 |
| Glide Compose | compose | 1.0.0-beta10 |
| Media3 | exoplayer / ui / common / ui-compose | 1.10.1 |
| 图片裁剪 | android-image-cropper | 4.7.0 |
| Firebase | Firebase BOM / Firestore | 34.16.0 / 26.4.1 |
| AppsFlyer | af-android-sdk | 7.0.0 |
| Google Play Billing | billing / billing-ktx | 9.1.0 |
| Google 登录 | play-services-auth | 21.6.0 |
| AppCompat | appcompat | 1.7.1 |
| SplashScreen | core-splashscreen | 1.2.0 |
| 测试 | JUnit / Mockito / Truth | 4.13.2 / 5.23.0 / 1.4.2 |
| 协程测试 | kotlinx-coroutines-test / Turbine | 1.11.0 / 1.2.1 |

项目已移除直接声明的 Accompanist 依赖。`FlowRow`、下拉刷新和系统栏控制使用 AndroidX/Compose API；`android-image-cropper` 4.7.0 仍可能通过传递依赖带入 `accompanist-drawablepainter`。

## 环境要求

- Android Studio 使用与 Gradle 兼容的 JDK 21（当前 CI 使用 Temurin 21）。
- Android SDK Platform 37。
- 已配置 Android SDK、`local.properties` 和可执行的 `gradlew`。

## 本地构建

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

Windows PowerShell 使用：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Debug APK 输出在 `app/build/outputs/apk/debug/`，Release AAB 输出在 `app/build/outputs/bundle/release/`。

## Release 签名

`app/build.gradle.kts` 支持两种签名配置来源，环境变量优先：

| 环境变量 | 本地 `gradle.properties` 回退键 |
| --- | --- |
| `SIGNING_STORE_PATH` | `storeFile` |
| `SIGNING_STORE_PASSWORD` | `storePassword` |
| `SIGNING_KEY_ALIAS` | `keyAlias` |
| `SIGNING_KEY_PASSWORD` | `keyPassword` |

本地可以在根目录 `gradle.properties` 配置签名信息；不要把 keystore 文件提交到 Git。CI 需要配置以下 GitHub Actions Secrets：

- `SIGNING_STORE_BASE64`：`app/vistara.jks` 的 Base64 内容
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

CI 会将 keystore 还原到 `app/vistara.jks`，执行 `:app:bundleRelease`，并使用 bundletool 生成 Universal APK。流水线文件为 `.github/workflows/build-release.yml`。

## 目录结构

```text
app/src/main/java/com/vistara/aestheticwalls/
├── data/       数据模型、本地数据库、网络 API 与仓储
├── billing/    Google Play Billing
├── di/         Hilt、网络和 Glide 配置
├── manager/    壁纸、语言等系统能力
├── service/    动态壁纸服务
└── ui/         Activity、Compose 页面、组件和主题
```

## 验证记录

依赖升级后的当前验证命令：

```bash
./gradlew :app:compileDebugKotlin --stacktrace --no-daemon
./gradlew :app:assembleDebug --stacktrace --no-daemon
```

两项均应成功；项目仍可能输出 Android/Compose API deprecated warnings，但 warnings 不阻断构建。
