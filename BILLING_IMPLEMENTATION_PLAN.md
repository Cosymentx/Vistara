# Obscura 内购与订阅实施计划

## 📋 概述

为 Obscura 壁纸应用添加 Google Play 内购（In-App Purchase）和订阅（Subscription）功能，实现应用变现。

## 🎯 商业模式设计（用户需求版）

### 免费版功能
- ✅ 浏览和搜索所有壁纸
- ✅ 下载和使用大部分免费壁纸
- ✅ 基础分类浏览
- ✅ 收藏功能
- ✅ 基础壁纸设置

### 高级版功能（订阅解锁）
- 🔒 **壁纸编辑功能**（必须订阅）
  - 裁剪、滤镜、调色、模糊等
  - 编辑入口显示 👑 图标
- 🔒 **动态视频壁纸**（必须订阅）
  - 所有视频壁纸需要订阅
  - 视频卡片显示 👑 图标
- 🔒 **高级壁纸**（随机 30% 壁纸需要订阅）
  - 精选高质量壁纸
  - 卡片右上角显示 👑 图标
  - 可以预览但不能下载/设置
- ✨ 无广告体验
- ☁️ 云端同步收藏

### 付费标识设计
- 👑 金色皇冠图标：表示需要订阅
- 🔓 解锁动画：订阅成功后的视觉反馈
- 💎 Premium 徽章：用户头像旁显示会员身份

## 📦 订阅方案

| 方案 | 价格 | 试用期 | 特点 |
|------|------|--------|------|
| **月度订阅** | $2.99/月 | 3天免费 | 灵活取消 |
| **年度订阅** | $19.99/年 | 7天免费 | 省 44% |
| **终身会员** | $49.99 | - | 一次付费永久使用 |

## 🏗️ 技术实施架构

### 1. 依赖配置

```kotlin
// app/build.gradle.kts
dependencies {
    // Google Play Billing Library v6
    implementation("com.android.billingclient:billing-ktx:6.2.1")
    
    // 协程支持（已有）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### 2. 项目结构

```
app/src/main/java/com/obscura/wallpapers/
├── core/
│   └── billing/
│       ├── BillingManager.kt              # 核心计费管理器
│       ├── BillingRepository.kt           # 数据仓库层
│       ├── model/
│       │   ├── ProductType.kt             # 产品类型枚举
│       │   ├── SubscriptionPlan.kt        # 订阅方案
│       │   └── PurchaseState.kt           # 购买状态
│       └── di/
│           └── BillingModule.kt           # Hilt 依赖注入
├── features/
│   └── subscription/
│       ├── SubscriptionScreen.kt          # 订阅页面
│       ├── SubscriptionViewModel.kt       # ViewModel
│       ├── components/
│       │   ├── PlanCard.kt                # 订阅方案卡片
│       │   ├── FeatureList.kt             # 功能列表
│       │   └── PurchaseButton.kt          # 购买按钮
│       └── PaywallScreen.kt               # 付费墙页面
└── core/
    └── data/
        └── local/
            └── entity/
                └── UserSubscriptionEntity.kt  # 订阅状态实体
```

## 📝 实施步骤

### Phase 1: 基础设施搭建（2-3天）

#### 1.1 Google Play Console 配置
- [ ] 创建应用内商品（In-app products）
  - 月度订阅：`premium_monthly`
  - 年度订阅：`premium_yearly`
  - 终身会员：`premium_lifetime`
  - 滤镜包：`filter_pack_01`
  - 壁纸包：`wallpaper_pack_exclusive`
- [ ] 配置订阅基础计划和优惠
- [ ] 设置价格和试用期
- [ ] 添加测试账号

#### 1.2 添加依赖和权限
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="com.android.vending.BILLING" />
```

#### 1.3 创建核心计费类
- [ ] `BillingManager.kt` - 封装 BillingClient
- [ ] `BillingRepository.kt` - 数据层抽象
- [ ] `ProductType.kt` - 产品类型定义
- [ ] `SubscriptionPlan.kt` - 订阅方案模型

### Phase 2: 核心功能开发（3-4天）

#### 2.1 BillingManager 核心功能
```kotlin
class BillingManager @Inject constructor(
    private val context: Context
) {
    // 初始化连接
    fun startConnection()
    
    // 查询可用产品
    suspend fun queryProducts(): List<ProductDetails>
    
    // 查询订阅状态
    suspend fun queryPurchases(): List<Purchase>
    
    // 发起购买
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails)
    
    // 确认购买
    suspend fun acknowledgePurchase(purchase: Purchase)
    
    // 消耗商品（一次性内购）
    suspend fun consumePurchase(purchase: Purchase)
    
    // 检查订阅状态
    fun isPremiumActive(): Flow<Boolean>
}
```

#### 2.2 数据库集成
```kotlin
@Entity(tableName = "user_subscription")
data class UserSubscriptionEntity(
    @PrimaryKey val id: Int = 1,
    val isPremium: Boolean = false,
    val subscriptionType: String? = null, // monthly, yearly, lifetime
    val purchaseToken: String? = null,
    val expiryDate: Long? = null,
    val autoRenewing: Boolean = false,
    val lastVerified: Long = System.currentTimeMillis()
)
```

#### 2.3 Repository 层
```kotlin
class BillingRepository @Inject constructor(
    private val billingManager: BillingManager,
    private val subscriptionDao: SubscriptionDao,
    private val dataStore: DataStore<Preferences>
) {
    val isPremiumUser: Flow<Boolean>
    
    suspend fun refreshSubscriptionStatus()
    
    suspend fun purchaseProduct(activity: Activity, productId: String)
    
    suspend fun restorePurchases()
}
```

### Phase 3: UI 开发（2-3天）

#### 3.1 订阅页面设计
- [ ] 精美的订阅方案卡片（Glassmorphism 风格）
- [ ] 功能对比表格
- [ ] 用户评价展示
- [ ] 常见问题解答
- [ ] 恢复购买按钮

#### 3.2 付费墙（Paywall）
- [ ] 在关键功能点触发（下载高清、视频壁纸、自动换壁纸）
- [ ] 优雅的引导文案
- [ ] 快速订阅入口

#### 3.3 设置页面集成
- [ ] 显示当前订阅状态
- [ ] 管理订阅按钮（跳转 Play Store）
- [ ] 恢复购买选项

### Phase 4: 功能限制实施（2-3天）

#### 4.1 创建权限检查工具
```kotlin
class FeatureAccessManager @Inject constructor(
    private val billingRepository: BillingRepository
) {
    // 检查是否可以使用编辑功能
    suspend fun canEditWallpaper(): Boolean
    
    // 检查是否可以使用视频壁纸
    suspend fun canUseVideoWallpaper(): Boolean
    
    // 检查特定壁纸是否需要订阅
    suspend fun canAccessWallpaper(wallpaperId: String): Boolean
    
    // 显示付费墙
    fun showPaywallIfNeeded(feature: PremiumFeature): Boolean
}

enum class PremiumFeature {
    WALLPAPER_EDIT,      // 壁纸编辑
    VIDEO_WALLPAPER,     // 视频壁纸
    PREMIUM_WALLPAPER    // 高级壁纸
}
```

#### 4.2 壁纸数据模型扩展
```kotlin
// 在 Wallpaper 实体中添加字段
@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey val id: String,
    val url: String,
    val thumbnail: String,
    // 新增：是否需要订阅
    val isPremium: Boolean = false,
    // 新增：是否为视频壁纸
    val isVideo: Boolean = false,
    // ... 其他字段
)
```

#### 4.3 高级壁纸标记策略
```kotlin
// 随机标记 30% 壁纸为高级内容
class WallpaperPremiumMarker {
    fun markPremiumWallpapers(wallpapers: List<Wallpaper>): List<Wallpaper> {
        val premiumCount = (wallpapers.size * 0.3).toInt()
        val premiumIndices = wallpapers.indices.shuffled().take(premiumCount)
        
        return wallpapers.mapIndexed { index, wallpaper ->
            wallpaper.copy(isPremium = index in premiumIndices)
        }
    }
}
```

#### 4.4 UI 组件添加 Premium 标识
- [ ] **WallpaperCard**: 右上角添加 👑 图标（isPremium = true）
- [ ] **VideoWallpaperCard**: 右上角添加 👑 图标（所有视频）
- [ ] **编辑按钮**: 按钮旁显示 👑 图标（未订阅时）
- [ ] **详情页**: 下载/设置按钮显示"订阅解锁"（高级内容）

#### 4.5 在关键功能点添加检查
- [ ] **壁纸详情页** - 点击下载/设置时检查权限
- [ ] **编辑入口** - 点击编辑按钮时检查权限
- [ ] **视频播放** - 点击视频壁纸时检查权限
- [ ] **预览功能** - 高级壁纸可以预览但不能应用

### Phase 5: 测试与优化（2-3天）

#### 5.1 测试清单
- [ ] 购买流程测试（使用测试账号）
- [ ] 订阅恢复测试
- [ ] 订阅过期处理
- [ ] 退款处理
- [ ] 网络异常处理
- [ ] 多设备同步测试

#### 5.2 安全性
- [ ] 服务器端验证（推荐使用 Google Play Developer API）
- [ ] 防止破解和篡改
- [ ] 加密存储购买凭证

#### 5.3 用户体验优化
- [ ] 加载状态提示
- [ ] 错误提示友好化
- [ ] 购买成功动画
- [ ] 引导新用户试用

### Phase 6: 上线准备（1-2天）

#### 6.1 Play Console 配置
- [ ] 完善应用内商品描述
- [ ] 上传订阅页面截图
- [ ] 配置促销优惠
- [ ] 设置订阅管理链接

#### 6.2 合规性检查
- [ ] 隐私政策更新（说明订阅数据收集）
- [ ] 用户协议更新
- [ ] 退款政策说明
- [ ] 数据安全表单更新

#### 6.3 分析埋点
```kotlin
// 关键事件追踪
- subscription_page_viewed
- plan_selected
- purchase_initiated
- purchase_completed
- purchase_failed
- subscription_cancelled
- paywall_shown
- paywall_dismissed
```

## 🔒 安全最佳实践

### 1. 本地验证
```kotlin
private fun verifyPurchase(purchase: Purchase): Boolean {
    // 验证签名
    val isValid = Security.verifyPurchase(
        purchase.originalJson,
        purchase.signature
    )
    
    // 验证状态
    return isValid && 
           purchase.purchaseState == Purchase.PurchaseState.PURCHASED
}
```

### 2. 服务器验证（推荐）
- 使用 Google Play Developer API
- 在后端验证购买凭证
- 防止客户端篡改

### 3. 混淆配置
```proguard
# Keep billing classes
-keep class com.android.billingclient.** { *; }
-keep class com.obscura.wallpapers.core.billing.** { *; }
```

## 📊 数据分析指标

### 关键指标
- **转化率**: 免费用户 → 付费用户
- **ARPU**: 平均每用户收入
- **留存率**: 订阅续订率
- **流失率**: 取消订阅率
- **试用转化**: 试用 → 付费转化率

### A/B 测试建议
- 不同价格点测试
- 试用期长度测试（3天 vs 7天）
- 付费墙触发时机测试
- 订阅页面设计测试

## 🎨 UI/UX 设计建议

### 订阅页面设计要点
1. **视觉吸引力**: 使用 Glassmorphism 效果，与应用主题一致
2. **价值突出**: 清晰展示高级功能的价值
3. **社会证明**: 显示用户评价和订阅人数
4. **紧迫感**: "限时优惠"、"仅剩 X 个名额"
5. **信任建立**: 显示安全支付标识、退款保证

### 付费墙最佳实践
- 在用户体验到核心价值后再展示
- 提供"稍后再说"选项，不强制
- 清晰说明为什么需要升级
- 提供快速订阅路径

## 💰 预期收益估算

假设：
- 月活用户: 10,000
- 付费转化率: 2%
- 平均订阅: $2.99/月

**月收入估算**: 10,000 × 2% × $2.99 = $598/月

优化后（5% 转化率）: $1,495/月

## 📅 时间线总览

| 阶段 | 任务 | 预计时间 |
|------|------|----------|
| Phase 1 | 基础设施搭建 | 2-3天 |
| Phase 2 | 核心功能开发 | 3-4天 |
| Phase 3 | UI 开发 | 2-3天 |
| Phase 4 | 功能限制实施 | 2天 |
| Phase 5 | 测试与优化 | 2-3天 |
| Phase 6 | 上线准备 | 1-2天 |
| **总计** | | **12-17天** |

## 🚀 下一步行动

1. **立即开始**: 在 Google Play Console 创建应用内商品
2. **设计评审**: 确定最终的订阅方案和定价
3. **技术准备**: 添加 Billing Library 依赖
4. **开始开发**: 从 Phase 1 开始逐步实施

## 📚 参考资源

- [Google Play Billing Library 文档](https://developer.android.com/google/play/billing)
- [订阅最佳实践](https://developer.android.com/google/play/billing/subscriptions)
- [Play Console 帮助中心](https://support.google.com/googleplay/android-developer)

---

**注意事项**:
- 确保遵守 Google Play 政策
- 提供清晰的退款政策
- 测试所有购买流程
- 准备好客服支持渠道
