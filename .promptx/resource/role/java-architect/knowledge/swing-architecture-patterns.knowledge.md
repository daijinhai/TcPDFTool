# Swing架构模式

<knowledge>
## MVC架构模式在Swing中的应用

### 经典MVC结构
```java
// Model - 数据模型
public class PDFDetectionModel {
    private String scanDirectory;
    private int scanInterval;
    private List<DetectionResult> results;
    private final List<ModelListener> listeners = new ArrayList<>();
    
    public void setScanDirectory(String directory) {
        this.scanDirectory = directory;
        notifyListeners();
    }
    
    public void addResult(DetectionResult result) {
        this.results.add(result);
        notifyListeners();
    }
    
    private void notifyListeners() {
        listeners.forEach(listener -> listener.onModelChanged());
    }
}

// View - 用户界面
public class MainFrame extends JFrame implements ModelListener {
    private final PDFDetectionModel model;
    private final PDFDetectionController controller;
    
    public MainFrame(PDFDetectionModel model, PDFDetectionController controller) {
        this.model = model;
        this.controller = controller;
        initComponents();
        model.addListener(this);
    }
    
    @Override
    public void onModelChanged() {
        SwingUtilities.invokeLater(this::updateUI);
    }
    
    private void updateUI() {
        // 根据模型状态更新界面
        directoryLabel.setText(model.getScanDirectory());
        resultTable.setModel(new ResultTableModel(model.getResults()));
    }
}

// Controller - 控制器
public class PDFDetectionController {
    private final PDFDetectionModel model;
    private final PDFDetectionService service;
    
    public void startScanning() {
        service.startScanning(model.getScanDirectory(), 
            result -> model.addResult(result));
    }
    
    public void selectDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            model.setScanDirectory(chooser.getSelectedFile().getAbsolutePath());
        }
    }
}
```

## MVP架构模式

### MVP结构设计
```java
// View接口
public interface MainView {
    void showScanDirectory(String directory);
    void showScanResults(List<DetectionResult> results);
    void showProgress(boolean scanning);
    void showError(String message);
    void setPresenter(MainPresenter presenter);
}

// Presenter
public class MainPresenter {
    private final MainView view;
    private final PDFDetectionService service;
    private final PDFDetectionModel model;
    
    public MainPresenter(MainView view, PDFDetectionService service) {
        this.view = view;
        this.service = service;
        this.model = new PDFDetectionModel();
        view.setPresenter(this);
    }
    
    public void onSelectDirectoryClicked() {
        // 处理目录选择逻辑
        String directory = showDirectoryChooser();
        if (directory != null) {
            model.setScanDirectory(directory);
            view.showScanDirectory(directory);
        }
    }
    
    public void onStartScanClicked() {
        view.showProgress(true);
        service.scanAsync(model.getScanDirectory())
            .thenAccept(results -> {
                model.setResults(results);
                view.showScanResults(results);
                view.showProgress(false);
            })
            .exceptionally(throwable -> {
                view.showError(throwable.getMessage());
                view.showProgress(false);
                return null;
            });
    }
}

// View实现
public class MainFrameImpl extends JFrame implements MainView {
    private MainPresenter presenter;
    private JLabel directoryLabel;
    private JTable resultTable;
    private JProgressBar progressBar;
    
    @Override
    public void setPresenter(MainPresenter presenter) {
        this.presenter = presenter;
    }
    
    @Override
    public void showScanDirectory(String directory) {
        SwingUtilities.invokeLater(() -> 
            directoryLabel.setText(directory));
    }
    
    @Override
    public void showScanResults(List<DetectionResult> results) {
        SwingUtilities.invokeLater(() -> 
            resultTable.setModel(new ResultTableModel(results)));
    }
}
```

## 组件化架构

### 组件设计原则
```java
// 基础组件接口
public interface Component {
    void initialize();
    void destroy();
    JComponent getComponent();
}

// 配置面板组件
public class ConfigPanel implements Component {
    private JPanel panel;
    private final ConfigModel configModel;
    private final List<ConfigChangeListener> listeners = new ArrayList<>();
    
    public ConfigPanel(ConfigModel configModel) {
        this.configModel = configModel;
    }
    
    @Override
    public void initialize() {
        panel = new JPanel(new GridBagLayout());
        createScanConfigSection();
        createDetectionConfigSection();
        createNotificationConfigSection();
        bindEvents();
    }
    
    private void createScanConfigSection() {
        // 创建扫描配置UI
        JLabel directoryLabel = new JLabel("扫描目录:");
        JTextField directoryField = new JTextField(20);
        JButton browseButton = new JButton("浏览...");
        
        // 添加到面板
        GridBagConstraints gbc = new GridBagConstraints();
        panel.add(directoryLabel, gbc);
        panel.add(directoryField, gbc);
        panel.add(browseButton, gbc);
    }
    
    @Override
    public JComponent getComponent() {
        return panel;
    }
}

// 结果显示组件
public class ResultPanel implements Component {
    private JPanel panel;
    private JTable resultTable;
    private ResultTableModel tableModel;
    
    @Override
    public void initialize() {
        panel = new JPanel(new BorderLayout());
        
        tableModel = new ResultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(resultTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 添加工具栏
        JToolBar toolBar = createToolBar();
        panel.add(toolBar, BorderLayout.NORTH);
    }
    
    public void updateResults(List<DetectionResult> results) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setResults(results);
            tableModel.fireTableDataChanged();
        });
    }
}
```

## 事件驱动架构

### 事件系统设计
```java
// 事件基类
public abstract class AppEvent {
    private final long timestamp;
    private final Object source;
    
    protected AppEvent(Object source) {
        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }
    
    public long getTimestamp() { return timestamp; }
    public Object getSource() { return source; }
}

// 具体事件类
public class ScanStartedEvent extends AppEvent {
    private final String directory;
    
    public ScanStartedEvent(Object source, String directory) {
        super(source);
        this.directory = directory;
    }
    
    public String getDirectory() { return directory; }
}

public class PDFDetectedEvent extends AppEvent {
    private final DetectionResult result;
    
    public PDFDetectedEvent(Object source, DetectionResult result) {
        super(source);
        this.result = result;
    }
    
    public DetectionResult getResult() { return result; }
}

// 事件总线
public class EventBus {
    private final Map<Class<? extends AppEvent>, List<EventListener>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService eventExecutor = Executors.newSingleThreadExecutor();
    
    public <T extends AppEvent> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
    
    public void publish(AppEvent event) {
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            eventExecutor.submit(() -> {
                for (EventListener listener : eventListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        // 记录异常但不影响其他监听器
                        logger.error("事件处理异常", e);
                    }
                }
            });
        }
    }
}

// 事件监听器
@FunctionalInterface
public interface EventListener<T extends AppEvent> {
    void onEvent(T event);
}
```

## 状态管理模式

### 状态机设计
```java
// 应用状态枚举
public enum AppState {
    IDLE("空闲"),
    SCANNING("扫描中"),
    PROCESSING("处理中"),
    ERROR("错误");
    
    private final String description;
    
    AppState(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}

// 状态管理器
public class StateManager {
    private AppState currentState = AppState.IDLE;
    private final List<StateChangeListener> listeners = new ArrayList<>();
    
    public void changeState(AppState newState) {
        AppState oldState = this.currentState;
        this.currentState = newState;
        
        // 通知状态变化
        SwingUtilities.invokeLater(() -> {
            for (StateChangeListener listener : listeners) {
                listener.onStateChanged(oldState, newState);
            }
        });
    }
    
    public AppState getCurrentState() {
        return currentState;
    }
    
    public boolean canTransitionTo(AppState targetState) {
        // 定义状态转换规则
        switch (currentState) {
            case IDLE:
                return targetState == AppState.SCANNING;
            case SCANNING:
                return targetState == AppState.PROCESSING || targetState == AppState.ERROR || targetState == AppState.IDLE;
            case PROCESSING:
                return targetState == AppState.IDLE || targetState == AppState.ERROR;
            case ERROR:
                return targetState == AppState.IDLE;
            default:
                return false;
        }
    }
}

// 状态变化监听器
public interface StateChangeListener {
    void onStateChanged(AppState oldState, AppState newState);
}
```

## 线程管理模式

### EDT安全的异步操作
```java
public class AsyncTaskManager {
    private final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();
    
    public <T> CompletableFuture<T> executeAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, backgroundExecutor);
    }
    
    public <T> void executeAsyncWithCallback(
            Supplier<T> backgroundTask,
            Consumer<T> successCallback,
            Consumer<Throwable> errorCallback) {
        
        executeAsync(backgroundTask)
            .thenAcceptAsync(result -> 
                SwingUtilities.invokeLater(() -> successCallback.accept(result)))
            .exceptionally(throwable -> {
                SwingUtilities.invokeLater(() -> errorCallback.accept(throwable));
                return null;
            });
    }
}

// 使用示例
public class ScanController {
    private final AsyncTaskManager taskManager = new AsyncTaskManager();
    private final MainView view;
    
    public void startScan(String directory) {
        view.showProgress(true);
        
        taskManager.executeAsyncWithCallback(
            // 后台任务
            () -> scanDirectory(directory),
            // 成功回调（在EDT中执行）
            results -> {
                view.showResults(results);
                view.showProgress(false);
            },
            // 错误回调（在EDT中执行）
            error -> {
                view.showError(error.getMessage());
                view.showProgress(false);
            }
        );
    }
}
```

## 插件化架构

### 插件接口设计
```java
// 插件接口
public interface Plugin {
    String getName();
    String getVersion();
    void initialize(PluginContext context);
    void destroy();
}

// 检测插件接口
public interface DetectionPlugin extends Plugin {
    boolean canHandle(File file);
    DetectionResult detect(File file, DetectionConfig config);
}

// 通知插件接口
public interface NotificationPlugin extends Plugin {
    void sendNotification(String message, NotificationConfig config);
    boolean testConnection(NotificationConfig config);
}

// 插件管理器
public class PluginManager {
    private final Map<String, Plugin> plugins = new HashMap<>();
    private final PluginContext context;
    
    public PluginManager(PluginContext context) {
        this.context = context;
    }
    
    public void loadPlugin(String pluginPath) {
        try {
            // 使用类加载器加载插件
            URLClassLoader classLoader = new URLClassLoader(new URL[]{new File(pluginPath).toURI().toURL()});
            ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
            
            for (Plugin plugin : serviceLoader) {
                plugin.initialize(context);
                plugins.put(plugin.getName(), plugin);
                logger.info("插件加载成功: {}", plugin.getName());
            }
        } catch (Exception e) {
            logger.error("插件加载失败: {}", pluginPath, e);
        }
    }
    
    public <T extends Plugin> List<T> getPlugins(Class<T> pluginType) {
        return plugins.values().stream()
            .filter(pluginType::isInstance)
            .map(pluginType::cast)
            .collect(Collectors.toList());
    }
}
```

## 配置和依赖注入

### 简单的依赖注入容器
```java
public class DIContainer {
    private final Map<Class<?>, Object> singletons = new HashMap<>();
    private final Map<Class<?>, Supplier<?>> factories = new HashMap<>();
    
    public <T> void registerSingleton(Class<T> type, T instance) {
        singletons.put(type, instance);
    }
    
    public <T> void registerFactory(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> type) {
        // 先查找单例
        T singleton = (T) singletons.get(type);
        if (singleton != null) {
            return singleton;
        }
        
        // 再查找工厂
        Supplier<T> factory = (Supplier<T>) factories.get(type);
        if (factory != null) {
            return factory.get();
        }
        
        throw new IllegalArgumentException("未注册的类型: " + type.getName());
    }
}

// 应用启动配置
public class AppBootstrap {
    private final DIContainer container = new DIContainer();
    
    public void initialize() {
        // 注册服务
        container.registerSingleton(ConfigManager.class, new ConfigManager());
        container.registerSingleton(EventBus.class, new EventBus());
        container.registerFactory(PDFDetectionService.class, 
            () -> new PDFDetectionService(container.getInstance(ConfigManager.class)));
        
        // 注册UI组件
        container.registerFactory(MainPresenter.class,
            () -> new MainPresenter(
                container.getInstance(MainView.class),
                container.getInstance(PDFDetectionService.class)
            ));
    }
    
    public DIContainer getContainer() {
        return container;
    }
}
```
</knowledge>