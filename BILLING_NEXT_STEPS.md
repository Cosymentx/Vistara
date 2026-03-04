# 内购功能 - 下一步操作指南

## ✅ 已完成的工作

### 1. 核心代码已创建
- ✅ `BillingManager.kt` - Google Play 计费管理器
- ✅ `BillingRepository.kt` - 数据仓库层
- ✅ `FeatureAccessManager.kt` - 功能访问控制
- ✅ `SubscriptionDao.kt` - 订阅数据持久化
- ✅ `UserSubscriptionEntity.kt` - 订阅实体
- ✅ `PremiumFeature.kt` - 高级功能枚举
- ✅ `PurchaseState.kt` - 购买状态
- ✅ `ProductType.kt` - 产品类型
- ✅ `BillingModule.kt` - Hilt 依赖注入
- ✅ `SubscriptionScreen.kt` - 订阅页面
- ✅ `PaywallScreen.kt` - 付费墙
- ✅ `SubscriptionViewModel.kt` - ViewModel
- ✅ `PremiumBadge.kt` - Premium 徽章组件
- ✅ 数据库迁移（版本 6 → 7）
- ✅ AndroidManifest.xml 添加 BILLING 权限
- ✅ libs.versions.toml 已包含 billing 库

### 2. 数据库更新
- ✅ AppDatabase 版本更新到 7
- ✅ 添加 `user_subscription` 表
- ✅ 添加 `SubscriptionDao` 到 AppDatabase

## 🔧 立即需要执行的步骤

### 步骤 1: 同步 Gradle 项目

在 Android Studio 中：
1. 点击顶部工具栏的 "Sync Project with Gradle Files" 按钮
2. 或者运行命令：`./gradlew clean build`

这将下载 Google Play Billing Library 并解决所有依赖问题。

### 步骤 2: 更新 AppDatabase

确保 `AppDatabase.kt` 包含新的 DAO 和迁移：

```kotlin
@Database(
    entities = [
        Wallpaper::class,
        AutoChangeHistory::class,
        UserSubscriptionEntity::class  // ✅ 已添加
    ],
    version = 7,  // ✅ 已更新
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun subscriptionDao(): SubscriptionDao  // ✅ 需要添加
    
    companion object {
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(...)
                .addMigrations(
                    DatabaseMigrations.MIGRATION_1_2,
                    DatabaseMigrations.MIGRATION_6_7  // ✅ 需要添加
                )
                .build()
        }
    }
}
```

### 步骤 3: 添加订阅页面到导航

在你的导航配置文件中（例如 `Navigation.kt` 或 `NavGraph.kt`）添加：

```kotlin
// 在 NavHost 中添加
composable("subscription") {
    SubscriptionScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}

// 在设置页面或其他地方添加入口
Button(
    onClick = { navController.navigate("subscription") }
) {
    Text("升级到高级版")
}
```

### 步骤 4: Google Play Console 配置

1. 登录 [Google Play Console](https://play.google.com/console)
2. 选择你的应用
3. 进入 "应用内商品" → "订阅"
4. 创建以下产品：

#### 产品 1: 月度订阅
- 产品 ID: `premium_monthly`
- 名称: Obscura Premium 月度订阅
- 描述: 解锁所有高级功能，每月自动续订
- 价格: $2.99/月
- 试用期: 3天免费

#### 产品 2: 年度订阅
- 产品 ID: `premium_yearly`
- 名称: Obscura Premium 年度订阅
- 描述: 解锁所有高级功能，每年自动续订，节省 44%
- 价格: $19.99/年
- 试用期: 7天免费

#### 产品 3: 终身会员
- 产品 ID: `premium_lifetime`
- 名称: Obscura Premium 终身会员
- 描述: 一次付费，永久享受所有高级功能
- 价格: $49.99（一次性）

### 步骤 5: 添加测试账号

在 Google Play Console → 设置 → 许可测试中添加测试邮箱，这样可以免费测试购买流程。

### 步骤 6: 编译并测试

```bash
# 清理并编译
./gradlew clean assembleDebug

# 安装到设备
./gradlew installDebug

# 或使用自定义任务
./gradlew buildInstallAndRun
```

## 📱 如何集成到现有功能

### 1. 在壁纸卡片中显示 Premium 标识

```kotlin
import com.obscura.wallpapers.ui.components.PremiumBadge

@Composable
fun WallpaperCard(wallpaper: Wallpaper) {
    Box {
        AsyncImage(model = wallpaper.url, ...)
        
        // 添加 Premium 徽章
        if (wallpaper.isPremium) {
            PremiumBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
    }
}
```

### 2. 在功能入口添加权限检查

```kotlin
import com.obscura.wallpapers.core.billing.FeatureAccessManager
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun WallpaperDetailScreen(
    wallpaper: Wallpaper,
    featureAccessManager: FeatureAccessManager = hiltViewModel()
) {
    var showPaywall by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Button(
        onClick = {
            scope.launch {
                val canAccess = featureAccessManager.canAccessWallpaper(wallpaper.isPremium)
                if (canAccess) {
                    downloadWallpaper(wallpaper)
                } else {
                    showPaywall = true
                }
            }
        }
    ) {
        Text("下载")
    }
    
    if (showPaywall) {
        PaywallScreen(
            feature = PremiumFeature.PREMIUM_WALLPAPER,
            onDismiss = { showPaywall = false },
            onSubscribe = {
                showPaywall = false
                navController.navigate("subscription")
            }
        )
    }
}
```

### 3. 在设置页面显示订阅状态

```kotlin
import com.obscura.wallpapers.core.billing.BillingRepository
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    billingRepository: BillingRepository = hiltViewModel()
) {
    val isPremium by billingRepository.isPremiumUser.collectAsState(false)
    
    Card {
        Row {
            Text(if (isPremium) "高级会员" else "免费版")
            if (!isPremium) {
                Button(onClick = { navController.navigate("subscription") }) {
                    Text("升级")
                }
            }
        }
    }
}
```

## 🧪 测试清单

### 本地测试
- [ ] 编译通过
- [ ] 应用启动正常
- [ ] 订阅页面显示正常
- [ ] 付费墙弹窗显示正常
- [ ] Premium 徽章显示正常

### Google Play 测试
- [ ] 使用测试账号登录
- [ ] 查看订阅产品列表
- [ ] 测试购买流程
- [ ] 测试恢复购买
- [ ] 测试订阅状态同步
- [ ] 测试功能权限控制

### 边界情况测试
- [ ] 网络断开时的处理
- [ ] 购买取消的处理
- [ ] 订阅过期的处理
- [ ] 多设备同步测试

## 📊 数据分析埋点建议

在关键位置添加分析事件：

```kotlin
// 订阅页面浏览
analytics.logEvent("subscription_page_viewed")

// 选择订阅方案
analytics.logEvent("plan_selected", mapOf("plan_id" to productId))

// 发起购买
analytics.logEvent("purchase_initiated", mapOf("product_id" to productId))

// 购买成功
analytics.logEvent("purchase_completed", mapOf(
    "product_id" to productId,
    "price" to price
))

// 购买失败
analytics.logEvent("purchase_failed", mapOf(
    "product_id" to productId,
    "error" to errorMessage
))

// 付费墙显示
analytics.logEvent("paywall_shown", mapOf("feature" to featureName))

// 付费墙关闭
analytics.logEvent("paywall_dismissed", mapOf("feature" to featureName))
```

## 🔐 安全建议

### 1. 服务器端验证（推荐）

虽然客户端验证已经实现，但强烈建议添加服务器端验证：

1. 在后端创建验证端点
2. 使用 Google Play Developer API 验证购买凭证
3. 在服务器端维护用户订阅状态

### 2. 混淆配置

在 `proguard-rules.pro` 中添加：

```proguard
# Keep billing classes
-keep class com.android.billingclient.** { *; }
-keep class com.obscura.wallpapers.core.billing.** { *; }
```

## 📝 常见问题

### Q: 测试时无法看到订阅产品？
A: 确保：
1. 应用已上传到 Google Play Console（至少是内部测试轨道）
2. 使用的账号已添加到测试账号列表
3. 产品状态为"有效"

### Q: 购买后状态没有更新？
A: 检查：
1. BillingManager 是否正确初始化
2. 购买回调是否正确处理
3. 数据库是否正确更新

### Q: 如何测试订阅过期？
A: 使用测试账号购买时，订阅会在几分钟内过期，方便测试。

## 🚀 上线前检查清单

- [ ] 所有测试通过
- [ ] Google Play Console 产品配置完成
- [ ] 隐私政策更新（说明订阅数据收集）
- [ ] 用户协议更新
- [ ] 退款政策说明
- [ ] 数据安全表单更新
- [ ] 混淆配置正确
- [ ] 分析埋点完成
- [ ] 服务器端验证（如果有）

## 📚 参考资源

- [Google Play Billing Library 文档](https://developer.android.com/google/play/billing)
- [订阅最佳实践](https://developer.android.com/google/play/billing/subscriptions)
- [测试 Google Play Billing](https://developer.android.com/google/play/billing/test)
- [Play Console 帮助中心](https://support.google.com/googleplay/android-developer)

---

## 💡 下一步建议

1. **立即执行**: 同步 Gradle 项目并编译
2. **配置 Play Console**: 创建订阅产品
3. **集成到现有功能**: 按照上面的示例添加权限检查
4. **测试**: 使用测试账号完整测试购买流程
5. **优化**: 根据用户反馈调整定价和功能

有任何问题随时问我！
