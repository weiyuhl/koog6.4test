# 开源许可功能说明

## 功能概述

已成功将 Jasmine 项目的开源许可功能复制到 JasmineStudio 项目中。

## 新增文件

### 1. OSS 许可相关文件
```
app/src/main/java/com/lhzkml/codestudio/oss/
├── OssLicenseLoader.kt          # 许可加载器
├── OssLicensesListScreen.kt     # 许可列表界面
└── OssLicensesDetailScreen.kt   # 许可详情界面
```

### 2. 数据模型
- `OssLicenseEntry`: 插件生成的许可条目（包含 offset 和 length）
- `ManualLicenseEntry`: 手动添加的许可条目（包含 licenseUrl）

## 功能特性

### 1. 自动收集依赖许可
- 使用 Google OSS Licenses Plugin 自动收集 Maven 依赖的许可信息
- 仅在 release 构建中生成许可文件

### 2. 手动添加许可
- 支持手动添加无法自动收集的许可（如 Koog AI Framework）
- 在 `OssLicenseLoader.manualLicenses` 中配置

### 3. 许可详情展示
- 支持从本地资源文件读取许可全文
- 支持从 URL 动态获取许可全文
- 自动识别文本中的 URL 并转换为可点击链接
- 优先尝试 HTTPS，失败时回退到 HTTP

### 4. UI 样式
- 与 Jasmine 项目保持一致的界面风格
- 白色卡片 + 灰色背景
- 圆角设计
- 清晰的层级结构

## 使用方式

### 用户操作流程
1. 打开应用
2. 进入"设置"页面
3. 点击"开源许可"选项
4. 查看所有开源库列表
5. 点击任意库名查看详细许可信息

### 开发者配置

#### 添加手动许可
在 `OssLicenseLoader.kt` 中修改：
```kotlin
val manualLicenses: List<ManualLicenseEntry> = listOf(
    ManualLicenseEntry("Koog AI Framework", "http://www.apache.org/licenses/LICENSE-2.0.txt"),
    // 添加更多手动许可...
)
```

#### 构建 Release 版本
```bash
./gradlew assembleRelease
```

Release 版本会自动生成以下资源文件：
- `res/raw/third_party_license_metadata` - 许可元数据
- `res/raw/third_party_licenses` - 许可全文

## 技术实现

### 1. 插件配置
- 在 `gradle/libs.versions.toml` 中添加 OSS Licenses Plugin
- 在 `app/build.gradle.kts` 中应用插件

### 2. 导航路由
- `Route.OssLicensesList` - 许可列表页面
- `Route.OssLicensesDetail` - 许可详情页面（带参数）

### 3. 参数传递
- 插件生成的许可：通过 offset 和 length 参数
- 手动添加的许可：通过 licenseUrl 参数

### 4. URL 处理
- 使用 `Uri.encode()` 和 `Uri.decode()` 处理特殊字符
- 支持 HTTP/HTTPS 协议
- 自动提取文本中的 URL 并转换为可点击链接

## 注意事项

1. **Debug 构建**：许可信息仅在 release 构建中可用，debug 构建会显示提示信息

2. **网络权限**：如果手动许可使用 HTTP URL，需要在 AndroidManifest.xml 中配置：
   ```xml
   <application
       android:usesCleartextTraffic="true"
       ...>
   ```

3. **缓存机制**：从 URL 获取的许可内容会被缓存，避免重复请求

4. **错误处理**：
   - 网络请求失败时显示友好提示
   - 提供浏览器打开链接的备选方案

## 与 Jasmine 项目的差异

1. **样式调整**：使用 JasmineStudio 项目的 Text 组件和颜色方案
2. **导航方式**：使用 NavigationViewModel 而非 NavController
3. **依赖注入**：简化了依赖注入，直接在 Composable 中使用 LocalContext
4. **手动许可**：默认添加了 Koog AI Framework 的许可

## 测试建议

1. 构建 release 版本并安装
2. 进入设置 → 开源许可
3. 验证列表显示正常
4. 点击查看详情
5. 测试 URL 链接点击功能
6. 验证网络许可获取功能

## 后续优化

1. 添加搜索功能
2. 支持按许可类型分组
3. 添加许可统计信息
4. 支持导出许可列表
