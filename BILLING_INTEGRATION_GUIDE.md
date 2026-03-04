# 内购功能集成指南

## ✅ 已完成的工作

### 1. 核心架构
- ✅ `BillingManager.kt` - Google Play 计费管理器
- ✅ `BillingRepository.kt` - 数据仓库层
- ✅ `FeatureAccessManager.kt` - 功能访问控制
- ✅ `SubscriptionDao.kt` - 订阅数据持久化
- ✅ 数据库迁移（版本 6 → 7）

### 2. 数据模型
- ✅ `PremiumFeature.kt` - 高级功能枚举
- ✅ `PurchaseState.kt` - 购买状态
- ✅ `ProductType.kt` - 产品类型
- ✅ `UserSubscriptionEntity.kt` - 订阅实体

### 3. UI 组件
- ✅ `SubscriptionScreen.kt` - 订阅页面
- ✅ `PaywallScreen.kt` - 付费墙
- ✅ `PremiumBadge.kt` - Premium 徽章组件
- ✅ `SubscriptionViewModel.kt` - 订阅 ViewModel

### 4. 依赖配置
- ✅ 添加 Billing Library 依赖
- ✅ 添加 BILLING 权限
- ✅ Hilt 依赖注入配置

## 🔧 集成步骤

### 步骤 1: 在壁纸数据模型中添加 Premium 标记

找到你的 `Wallpaper` 数据类（可能在 `core/data/model/Wallpaper.kt`），添加以下字段：

```kotlin
@Entity(tableName = "wallpapers")
data class Wallpaper(
    @PrimaryKey val id: String,
    val url: String,
    val thumbnail: String,
    // ... 其他现有字段
    
    // 新增字段
    val isPremium: Boolean = false,  // 是否为高级壁纸
    val isVideo: Boolean = false     // 是否为视频壁纸
)
```

### 步骤 2: 更新数据库版本

在 `AppDatabase.kt` 中已经更新到版本 7，如果你的 Wallpaper 表需要添加字段，需要创建新的迁移：

```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE wallpapers ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE wallpapers ADD COLUMN isVideo INTEGER NOT NULL DEFAULT 0")
    }
}
```

### 步骤 3: 在壁纸卡片中显示 Premium 标识

找到你的壁纸卡片组件（例如 `WallpaperCard.kt`），添加 Premium 徽章：

```kotlin
import com.obscura.wallpapers.ui.components.PremiumBadge

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 现有的壁纸图片和内容
        AsyncImage(
            model = wallpaper.thumbnail,
            contentDescription = null,
            // ...
        )
        
        // 添加 Premium 徽章（右上角）
        if (wallpaper.isPremium || wallpaper.isVideo) {
            PremiumBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
    }
}
```

### 步骤 4: 在壁纸详情页添加权限检查

找到壁纸详情页面（例如 `WallpaperDetailScreen.kt` 或类似的），添加权限检查：

```kotlin
import com.obscura.wallpapers.core.billing.FeatureAccessManager
import com.obscura.wallpapers.core.billing.model.PremiumFeature
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun WallpaperDetailScreen(
    wallpaper: Wallpaper,
    featureAccessManager: FeatureAccessManager = hiltViewModel(), // 注入
    onNavigateToSubscription: () -> Unit
) {
    var showPaywall by remember { mutableStateOf(false) }
    
    // 下载/设置按钮点击处理
    val onDownloadClick = {
        if (wallpaper.isPremium) {
            // 检查权限
            lifecycleScope.launch {
                val canAccess = featureAccessManager.canAccessWallpaper(wallpaper.isPremium)
                if (!canAccess) {
                    showPaywall = true
                } else {
                    // 执行下载
                    downloadWallpaper(wallpaper)
                }
            }
        } else {
            // 免费壁纸直接下载
            downloadWallpaper(wallpaper)
        }
    }
    
    // 显示付费墙
    if (showPaywall) {
        PaywallScreen(
            feature = PremiumFeature.PREMIUM_WALLPAPER,
            onDismiss = { showPaywall = false },
            onSubscribe = {
                showPaywall = false
                onNavigateToSubscription()
            }
        )
    }
}
```

### 步骤 5: 在编辑功能入口添加权限检查

找到壁纸编辑入口（例如编辑按钮），添加权限检查：

```kotlin
import com.obscura.wallpapers.core.billing.FeatureAccessManager
import com.obscura.wallpapers.core.billing.model.PremiumFeature

@Composable
fun WallpaperActions(
    wallpaper: Wallpaper,
    featureAccessManager: FeatureAccessManager = hiltViewModel(),
    onNavigateToEdit: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    var showPaywall by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 编辑按钮
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    val canEdit = featureAccessManager.canEditWallpaper()
                    if (canEdit) {
                        onNavigateToEdit()
                    } else {
                        showPaywall = true
                    }
                }
            }
        ) {
            Icon(Icons.Default.Edit, contentDescription = "编辑")
        }
        
        // 显示 Premium 标识
        scope.launch {
            if (featureAccessManager.isFeatureLocked(PremiumFeature.WALLPAPER_EDIT)) {
                CrownIcon(modifier = Modifier.size(16.dp))
            }
        }
    }
    
    // 付费墙
    if (showPaywall) {
        PaywallScreen(
            feature = PremiumFeature.WALLPAPER_EDIT,
            onDismiss = { showPaywall = false },
            onSubscribe = {
                showPaywall = false
                onNavigateToSubscription()
            }
        )
    }
}
```

### 步骤 6: 在视频壁纸入口添加权限检查

找到视频壁纸相关的代码，添加权限检查：

```kotlin
@Composable
fun VideoWallpaperCard(
    wallpaper: Wallpaper,
    featureAccessManager: FeatureAccessManager = hiltViewModel(),
    onNavigateToSubscription: () -> Unit
) {
    var showPaywall by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier.clickable {
            scope.launch {
                val canUseVideo = featureAccessManager.canUseVideoWallpaper()
                if (canUseVideo) {
                    // 播放视频
                    playVideo(wallpaper)
                } else {
                    showPaywall = true
                }
            }
        }
    ) {
        // 视频缩略图
        AsyncImage(...)
        
        // Premium 徽章
        PremiumBadge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
    }
    
    if (showPaywall) {
        PaywallScreen(
            feature = PremiumFeature.VIDEO_WALLPAPER,
            onDismiss = { showPaywall = false },
            onSubscribe = {
                showPaywall = false
                onNavigateToSubscription()
            }
        )
    }
}
```

### 步骤 7: 添加订阅页面到导航

在你的导航配置中添加订阅页面路由：

```kotlin
// 在 NavGraph 或 Navigation.kt 中
composable("subscription") {
    SubscriptionScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}

// 在设置页面或其他地方添加入口
TextButton(
    onClick = { navController.navigate("subscription") }
) {
    Text("升级到高级版")
}
```

### 步骤 8: 实现壁纸 Premium 标记逻辑

创建一个工具类来随机标记 30% 的壁纸为高级内容：

```kotlin
// 在 Repository 或 UseCase 中
class WallpaperRepository {
    
    suspend fun fetchWallpapers(): List<Wallpaper> {
        val wallpapers = apiService.getWallpapers()
        
        // 随机标记 30% 为高级壁纸
        return markPremiumWallpapers(wallpapers)
    }
    
    private fun markPremiumWallpapers(wallpapers: List<Wallpaper>): List<Wallpaper> {
        val premiumCount = (wallpapers.size * 0.3).toInt()
        val premiumIndices = wallpapers.indices.shuffled().take(premiumCount).toSet()
        
        return wallpapers.mapIndexed { index, wallpaper ->
            wallpaper.copy(
                isPremium = index in premiumIndices,
                isVideo = wallpaper.url.endsWith(".mp4") // 根据实际情况判断
            )
        }
    }
}
```

## 📱 Google Play Console 配置

### 1. 创建订阅产品

登录 Google Play Console → 你的应用 → 应用内商品 → 订阅

创建以下产品：

#### 月度订阅
- 产品 ID: `premium_monthly`
- 名称: Obscura Premium 月度订阅
- 价格: $2.99/月
- 试用期: 3天免费

#### 年度订阅
- 产品 ID: `premium_yearly`
- 名称: Obscura Premium 年度订阅
- 价格: $19.99/年
- 试用期: 7天免费

#### 终身会员
- 产品 ID: `premium_lifetime`
- 名称: Obscura Premium 终身会员
- 价格: $49.99（一次性）

### 2. 添加测试账号

在 Google Play Console → 设置 → 许可测试 中添加测试邮箱，这样可以免费测试购买流程。

## 🧪 测试清单

- [ ] 测试订阅购买流程
- [ ] 测试恢复购买功能
- [ ] 测试 Premium 徽章显示
- [ ] 测试付费墙触发
- [ ] 测试权限检查逻辑
- [ ] 测试视频壁纸权限
- [ ] 测试编辑功能权限
- [ ] 测试高级壁纸权限
- [ ] 测试订阅过期处理
- [ ] 测试多设备同步

## 🔍 调试技巧

### 查看 Billing 日志
```kotlin
// 在 BillingManager 中已经添加了日志
// 使用 Logcat 过滤 "BillingManager" 标签
```

### 测试订阅状态
```kotlin
// 在任何 Composable 中
val featureAccessManager: FeatureAccessManager = hiltViewModel()
val isPremium by featureAccessManager.billingRepository.isPremiumUser.collectAsState(false)

Text("Premium Status: $isPremium")
```

## 📝 注意事项

1. **测试环境**: 使用测试账号进行测试，避免真实扣费
2. **权限检查**: 所有高级功能入口都要添加权限检查
3. **用户体验**: 付费墙不要过于频繁，影响用户体验
4. **错误处理**: 处理网络错误、购买取消等异常情况
5. **数据同步**: 定期刷新订阅状态，确保多设备同步

## 🚀 下一步

1. 完成上述集成步骤
2. 在 Google Play Console 配置产品
3. 添加测试账号进行测试
4. 收集用户反馈优化体验
5. 准备上线发布

## 💡 优化建议

1. **A/B 测试**: 测试不同的价格点和试用期
2. **促销活动**: 定期推出限时优惠
3. **用户引导**: 在关键时刻引导用户订阅
4. **数据分析**: 追踪转化率和流失率
5. **用户反馈**: 收集用户对定价的反馈
