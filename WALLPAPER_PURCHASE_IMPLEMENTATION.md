# 壁纸购买和订阅功能实现计划

## 已完成
1. ✅ 在Wallpaper模型中添加 `requiresPurchase` 和 `purchasePrice` 字段
2. ✅ 创建数据库迁移 MIGRATION_7_8
3. ✅ 更新AppDatabase版本到8

## 待实现功能

### 1. 随机标记壁纸为需要购买
- 在WallpaperRepository中添加逻辑，随机30%的壁纸标记为需要购买
- 价格范围：10-50金币

### 2. UI显示购买/订阅标记
- 在壁纸列表项中显示金币图标和价格（如果需要购买）
- 在壁纸列表项中显示皇冠图标（如果是高级壁纸）
- 在详情页显示购买/订阅按钮

### 3. 详情页功能限制
- 编辑功能：检查是否为订阅用户，否则显示订阅弹窗
- 下载功能：检查是否需要购买，是否已购买
- 设置壁纸：检查是否需要购买，是否已购买

### 4. 优化设置壁纸状态反馈
- 点击设置壁纸时立即显示加载状态
- 显示进度指示器
- 设置成功后显示成功提示并关闭弹窗
- 设置失败显示错误信息

### 5. 购买流程
- 创建购买确认对话框
- 扣除金币
- 更新壁纸购买状态
- 保存到本地数据库

## 需要添加的字符串资源
- wallpaper_requires_purchase: "需要购买"
- wallpaper_purchase_with_coins: "使用 %d 金币购买"
- wallpaper_purchased: "已购买"
- wallpaper_premium_only: "订阅专享"
- wallpaper_setting: "正在设置壁纸..."
- wallpaper_set_success: "壁纸设置成功"
- wallpaper_set_failed: "壁纸设置失败"
- purchase_confirm_title: "购买壁纸"
- purchase_confirm_message: "确定要花费 %d 金币购买这张壁纸吗？"
- purchase_insufficient_coins: "金币不足"
- purchase_success: "购买成功"
- purchase_failed: "购买失败"
- feature_requires_subscription: "此功能需要订阅"
