# Java开发最佳实践

<knowledge>
## 代码组织和结构

### 包结构设计
```
com.company.project
├── config/          # 配置类
├── controller/      # 控制层（桌面应用为UI层）
├── service/         # 业务服务层
├── repository/      # 数据访问层
├── model/          # 数据模型
├── util/           # 工具类
├── exception/      # 异常类
└── constant/       # 常量定义
```

### 类命名规范
- **实体类**：使用名词，如 `User`, `Order`, `PDFFile`
- **服务类**：以Service结尾，如 `UserService`, `PDFDetectionService`
- **工具类**：以Util结尾，如 `FileUtil`, `DateUtil`
- **异常类**：以Exception结尾，如 `PDFProcessException`
- **常量类**：以Constants结尾，如 `AppConstants`

### 方法命名规范
- **查询方法**：get/find/query开头，如 `getUserById()`, `findPDFFiles()`
- **判断方法**：is/has/can开头，如 `isEmpty()`, `hasContent()`
- **操作方法**：动词开头，如 `save()`, `delete()`, `process()`
- **转换方法**：to开头，如 `toString()`, `toJson()`

## 异常处理最佳实践

### 异常层次设计
```java
// 应用基础异常
public class AppException extends Exception {
    private final String errorCode;
    private final String errorMessage;
    
    public AppException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}

// 业务异常
public class BusinessException extends AppException {
    public BusinessException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}

// 技术异常
public class TechnicalException extends AppException {
    public TechnicalException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage);
        initCause(cause);
    }
}
```

### 异常处理原则
1. **及早发现，就近处理**：在最合适的层级处理异常
2. **不要忽略异常**：至少要记录日志
3. **提供有意义的错误信息**：帮助用户和开发者理解问题
4. **使用检查异常处理业务异常**：强制调用者处理
5. **使用运行时异常处理编程错误**：如参数校验失败

## 日志记录最佳实践

### 日志级别使用
```java
// ERROR：系统错误，需要立即关注
logger.error("PDF文件处理失败: {}", fileName, exception);

// WARN：警告信息，可能影响功能但不会导致系统崩溃
logger.warn("PDF文件大小异常: {} KB，文件: {}", fileSize, fileName);

// INFO：重要的业务流程信息
logger.info("开始扫描目录: {}，扫描间隔: {} 分钟", directory, interval);

// DEBUG：详细的调试信息，生产环境通常关闭
logger.debug("检测到PDF文件: {}，创建时间: {}", fileName, createTime);
```

### 日志格式规范
```java
// 好的日志格式
logger.info("用户登录成功 - 用户ID: {}, IP: {}, 时间: {}", 
    userId, ipAddress, LocalDateTime.now());

// 避免的日志格式
logger.info("用户" + userId + "从" + ipAddress + "登录成功");
```

## 性能优化最佳实践

### 字符串处理优化
```java
// 大量字符串拼接使用StringBuilder
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item).append(",");
}
String result = sb.toString();

// 字符串常量池优化
private static final String EMPTY_PDF_MESSAGE = "PDF文件为空";
```

### 集合使用优化
```java
// 预估集合大小，避免扩容
List<File> files = new ArrayList<>(expectedSize);
Map<String, Object> config = new HashMap<>(16);

// 使用合适的集合类型
Set<String> uniqueIds = new HashSet<>();  // 需要去重
List<String> orderedItems = new ArrayList<>();  // 需要保持顺序
```

### 资源管理优化
```java
// 使用try-with-resources自动关闭资源
try (FileInputStream fis = new FileInputStream(file);
     BufferedInputStream bis = new BufferedInputStream(fis)) {
    // 处理文件
} catch (IOException e) {
    logger.error("文件处理失败", e);
}
```

## 线程安全最佳实践

### 线程安全的单例模式
```java
public class ConfigManager {
    private static volatile ConfigManager instance;
    private final Properties config;
    
    private ConfigManager() {
        config = new Properties();
        loadConfig();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
}
```

### 线程池使用
```java
// 创建合适的线程池
private final ExecutorService executor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors());

// 使用CompletableFuture处理异步任务
CompletableFuture<List<File>> scanTask = CompletableFuture
    .supplyAsync(() -> scanDirectory(path), executor)
    .exceptionally(throwable -> {
        logger.error("目录扫描失败", throwable);
        return Collections.emptyList();
    });
```

## 配置管理最佳实践

### 配置文件结构
```properties
# 应用基础配置
app.name=PDF检测工具
app.version=1.0.0

# 扫描配置
scan.directory=/default/scan/path
scan.interval.minutes=30
scan.time.range.days=1

# 检测配置
detection.size.threshold.kb=10
detection.image.width=200
detection.image.height=200
detection.white.pixel.threshold=0.9

# 通知配置
notification.bat.script.path=/path/to/notify.bat
notification.enabled=true

# 日志配置
logging.level=INFO
logging.file.path=logs/app.log
logging.file.max.size=10MB
```

### 配置类设计
```java
@Component
public class AppConfig {
    @Value("${scan.directory}")
    private String scanDirectory;
    
    @Value("${scan.interval.minutes:30}")
    private int scanIntervalMinutes;
    
    @Value("${detection.size.threshold.kb:10}")
    private int sizeThresholdKB;
    
    // getter和setter方法
}
```

## 测试最佳实践

### 单元测试结构
```java
@Test
public void testPDFSizeDetection() {
    // Given - 准备测试数据
    File testFile = createTestPDFFile(5); // 5KB文件
    PDFDetector detector = new PDFDetector();
    
    // When - 执行测试
    DetectionResult result = detector.detectBySize(testFile, 10);
    
    // Then - 验证结果
    assertEquals(DetectionResult.EMPTY_BY_SIZE, result.getType());
    assertTrue(result.getMessage().contains("小于阈值"));
}
```

### 集成测试策略
```java
@Test
public void testPDFDetectionWorkflow() {
    // 测试完整的检测流程
    String testDirectory = createTestDirectory();
    createTestPDFFiles(testDirectory);
    
    PDFDetectionService service = new PDFDetectionService();
    List<DetectionResult> results = service.scanAndDetect(testDirectory);
    
    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(r -> r.getType() == DetectionResult.EMPTY_BY_SIZE));
}
```

## 代码质量检查

### 代码审查检查点
1. **命名规范**：类名、方法名、变量名是否符合规范
2. **方法长度**：方法是否过长（建议不超过50行）
3. **类职责**：类是否遵循单一职责原则
4. **异常处理**：是否正确处理异常
5. **资源管理**：是否正确关闭资源
6. **线程安全**：多线程环境下是否安全
7. **性能考虑**：是否有明显的性能问题
8. **测试覆盖**：关键逻辑是否有测试覆盖

### 静态代码分析工具
- **SpotBugs**：查找潜在的bug和问题
- **Checkstyle**：检查代码风格和规范
- **PMD**：检查代码质量和最佳实践
- **SonarQube**：综合代码质量分析

## 部署和运维最佳实践

### 应用打包
```xml
<!-- Maven打包配置 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.2.4</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>com.company.project.Application</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### JVM调优参数
```bash
# 生产环境JVM参数建议
java -Xms512m -Xmx1024m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+PrintGCDetails \
     -XX:+PrintGCTimeStamps \
     -Xloggc:gc.log \
     -jar pdf-detection-tool.jar
```

### 监控和诊断
```java
// JMX监控配置
@Component
public class AppMonitor {
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onPDFDetected(PDFDetectedEvent event) {
        meterRegistry.counter("pdf.detected", 
            "result", event.getResult().getType().name())
            .increment();
    }
}
```
</knowledge>