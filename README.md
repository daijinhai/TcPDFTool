# TcPDFTool - PDF文件检测工具

一个基于Java Swing的桌面应用程序，用于自动检测指定目录下PDF文件的内容完整性，当发现空文件或内容异常时自动发送通知，确保产品设计图PDF文件的质量。

## 📋 功能特性

### 🔍 智能检测
- **双重检测算法**：结合文件大小和图像内容检测
- **文件大小检测**：检测PDF文件是否小于设定阈值（默认10KB）
- **图像内容检测**：分析PDF第一页指定区域的白色像素占比
- **自定义检测策略**：支持多种检测模式组合

### 📁 文件监控
- **目录递归扫描**：自动扫描指定目录及其子目录
- **时间范围过滤**：支持按文件创建时间筛选（当天/最近1-7天）
- **定时扫描**：可配置扫描间隔（5分钟-1小时）
- **实时监控**：启动后自动开始监控

### 📢 通知系统
- **自动通知**：检测到问题文件立即发送通知
- **脚本集成**：调用自定义jar包发送通知
- **通知测试**：提供测试功能验证配置正确性
- **消息模板**：标准化通知内容格式

### 🎨 用户界面
- **现代化界面**：基于Java Swing的直观桌面应用
- **文件列表显示**：表格形式展示PDF文件信息和检测状态
- **状态标识**：不同颜色标识文件状态（正常/可疑/检测失败/检测中）
- **设置对话框**：完整的配置管理界面
- **实时日志**：显示扫描和检测过程的详细日志

## 🚀 快速开始

### 环境要求

- **Java**: JDK 1.8 或更高版本
- **Maven**: 3.6 或更高版本
- **操作系统**: Windows/macOS/Linux

### 安装步骤
 
1. **编译项目**
   ```bash
   mvn clean compile
   ```

2. **运行应用**
   ```bash
   mvn exec:java -Dexec.mainClass="com.tcpdftool.TcPDFToolApplication"
   ```

3. **打包应用**（可选）
   ```bash
   mvn clean package
   java -jar target/pdf-detection-tool-1.0.0.jar
   ```

### 首次使用

1. **配置监控目录**：点击"设置"按钮，选择要监控的PDF文件目录
2. **调整检测参数**：根据需要调整文件大小阈值和图像检测参数
3. **配置通知脚本**：设置通知脚本路径，测试通知功能
4. **开始监控**：点击"开始监控"按钮启动自动检测

## 📖 使用指南

### 主界面功能

- **文件列表**：显示扫描到的PDF文件及其检测状态
- **控制按钮**：
  - `开始监控` / `停止监控`：控制自动监控开关
  - `立即扫描`：手动触发一次扫描
  - `设置`：打开配置对话框
  - `清空日志`：清除日志显示区域

### 状态标识说明

| 状态 | 颜色 | 说明 |
|------|------|------|
| 正常 | 浅绿色 | 文件检测正常，无问题 |
| 可疑 | 浅橙色 | 检测到疑似空文件或异常 |
| 检测失败 | 浅红色 | 检测过程中发生错误 |
| 检测中 | 浅蓝色 | 正在进行检测 |

### 配置选项

#### 扫描设置
- **监控目录**：选择要监控的PDF文件目录
- **扫描间隔**：5分钟、15分钟、30分钟、1小时
- **文件时间范围**：当天、最近1-7天
- **高级选项**：递归扫描、自动启动等

#### 检测设置
- **文件大小检测**：设置最小文件大小阈值（KB）
- **图像内容检测**：配置检测区域大小和白色像素阈值
- **检测策略**：双重检测、单一检测等模式

#### 通知设置
- **通知脚本**：配置bat脚本路径
- **参数模板**：自定义通知消息格式
- **测试功能**：验证通知配置

## 🏗️ 项目结构

```
TcPDFTool/
├── src/main/java/com/tcpdftool/
│   ├── TcPDFToolApplication.java    # 主应用程序入口
│   ├── config/                      # 配置管理
│   │   ├── AppConfig.java          # 应用配置类
│   │   └── ConfigManager.java      # 配置管理器
│   ├── model/                       # 数据模型
│   │   ├── DetectionResult.java    # 检测结果模型
│   │   └── PDFFileInfo.java        # PDF文件信息模型
│   ├── service/                     # 业务服务
│   │   ├── FileScanner.java        # 文件扫描服务
│   │   ├── NotificationService.java # 通知服务
│   │   └── PDFDetector.java         # PDF检测服务
│   ├── ui/                          # 用户界面
│   │   ├── MainFrame.java          # 主窗口
│   │   ├── PDFFileTableModel.java  # 表格模型
│   │   └── SettingsDialog.java     # 设置对话框
│   └── util/                        # 工具类
│       └── LoggerUtil.java         # 日志工具
├── doc/                             # 文档
│   └── PRD-PDF检测工具.md          # 产品需求文档
├── pom.xml                          # Maven配置文件
└── README.md                        # 项目说明文档
```

## 🔧 技术栈

- **UI框架**: Java Swing
- **PDF处理**: Apache PDFBox 2.0.29
- **JSON配置**: Jackson 2.13.5
- **文件监控**: Directory Watcher 0.18.0
- **日志框架**: Logback 1.2.12
- **构建工具**: Maven 3.x

## 📝 配置文件

应用程序使用JSON格式的配置文件，默认位置：`~/.tcpdftool/config.json`

```json
{
  "monitorDirectory": "/path/to/pdf/directory",
  "scanInterval": 30,
  "fileTimeRange": 24,
  "fileSizeThreshold": 10,
  "imageDetectionEnabled": true,
  "detectionAreaWidth": 200,
  "detectionAreaHeight": 200,
  "whitePixelThreshold": 90,
  "notificationScriptPath": "/path/to/notify.bat"
}
```

## 🐛 故障排除

### 常见问题

1. **应用无法启动**
   - 检查Java版本是否为1.8+
   - 确认Maven依赖已正确下载

2. **PDF检测失败**
   - 检查PDF文件是否损坏
   - 调整检测参数阈值

3. **通知不工作**
   - 验证bat脚本路径是否正确
   - 使用测试功能检查脚本执行权限

### 日志查看

应用程序日志保存在：`~/.tcpdftool/logs/`目录下
