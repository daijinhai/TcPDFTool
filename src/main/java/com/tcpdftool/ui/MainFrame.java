package com.tcpdftool.ui;

import com.tcpdftool.config.AppConfig;
import com.tcpdftool.config.ConfigManager;
import com.tcpdftool.model.PDFFileInfo;
import com.tcpdftool.service.FileScanner;
import com.tcpdftool.service.NotificationService;
import com.tcpdftool.service.PDFDetector;
import com.tcpdftool.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 主界面窗口
 */
public class MainFrame extends JFrame {
    
    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    
    private final ConfigManager configManager;
    private FileScanner fileScanner;
    private PDFDetector pdfDetector;
    private NotificationService notificationService;
    
    // UI组件
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JButton startButton;
    private JButton stopButton;
    private JButton settingsButton;
    private JButton refreshButton;
    private JButton redetectButton;
    private JLabel statusLabel;
    private JLabel fileCountLabel;
    private JProgressBar progressBar;
    private JTextArea logArea;
    
    // 状态变量
    private boolean isRunning = false;
    
    // 批量通知相关状态
    private final List<PDFFileInfo> currentScanSuspiciousFiles = new ArrayList<>();
    private int pendingDetections = 0;
    private int totalFilesInCurrentScan = 0;
    
    public MainFrame(ConfigManager configManager) {
        this.configManager = configManager;
        
        // 初始化服务
        AppConfig config = configManager.getConfig();
        this.fileScanner = new FileScanner(config);
        this.pdfDetector = new PDFDetector(config);
        this.notificationService = new NotificationService(config);
        
        // 设置回调
        setupServiceCallbacks();
        
        // 初始化UI
        initializeUI();
        
        // 设置窗口属性
        setupWindow();
        
        logger.info("主界面初始化完成");
    }
    
    /**
     * 重新检测所有文件
     */
    private void redetectAllFiles() {
        if (isRunning) {
            appendLog("正在监控中，无法执行重新检测");
            return;
        }
        
        AppConfig config = configManager.getConfig();
        String monitorDir = config.getMonitorDirectory();
        
        if (monitorDir == null || monitorDir.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "请先在设置中配置监控目录", 
                "配置错误", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        appendLog("开始重新检测所有文件...");
        
        // 清空当前文件列表和可疑文件列表
        tableModel.setRowCount(0);
        currentScanSuspiciousFiles.clear();
        pendingDetections = 0;
        totalFilesInCurrentScan = 0;
        updateFileCount();
        
        // 创建临时扫描器进行重新扫描
        FileScanner tempScanner = new FileScanner(config);
        tempScanner.setOnScanCompleted(files -> {
            SwingUtilities.invokeLater(() -> {
                totalFilesInCurrentScan = files.size();
                pendingDetections = files.size();
                
                for (PDFFileInfo fileInfo : files) {
                    addFileToTable(fileInfo);
                    // 异步检测每个文件
                    pdfDetector.detectAsync(fileInfo);
                }
                updateFileCount();
                appendLog("重新扫描完成，发现 " + files.size() + " 个PDF文件，正在重新检测...");
            });
        });
        
        // 异步执行扫描
        CompletableFuture.runAsync(() -> {
            tempScanner.startScanning();
            try {
                Thread.sleep(2000); // 等待扫描完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                tempScanner.stopScanning();
            }
        });
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // 创建菜单栏
        createMenuBar();
        
        // 创建工具栏
        JPanel toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);
        
        // 创建主面板
        JPanel mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);
        
        // 创建状态栏
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }
    
    /**
     * 创建菜单栏
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> exitApplication());
        fileMenu.add(exitItem);
        
        // 工具菜单
        JMenu toolsMenu = new JMenu("工具");
        JMenuItem settingsItem = new JMenuItem("设置");
        settingsItem.addActionListener(e -> openSettings());
        JMenuItem testNotificationItem = new JMenuItem("测试通知");
        testNotificationItem.addActionListener(e -> testNotification());
        toolsMenu.add(settingsItem);
        toolsMenu.addSeparator();
        toolsMenu.add(testNotificationItem);
        
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    /**
     * 创建工具栏
     */
    private JPanel createToolBar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolBar.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        startButton = new JButton("开始监控");
        startButton.setIcon(createIcon("▶"));
        startButton.addActionListener(e -> startMonitoring());
        
        stopButton = new JButton("停止监控");
        stopButton.setIcon(createIcon("⏸"));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopMonitoring());
        
        refreshButton = new JButton("刷新");
        refreshButton.setIcon(createIcon("🔄"));
        refreshButton.addActionListener(e -> refreshFileList());
        
        redetectButton = new JButton("重新检测");
        redetectButton.setIcon(createIcon("🔍"));
        redetectButton.addActionListener(e -> redetectAllFiles());
        
        settingsButton = new JButton("设置");
        settingsButton.setIcon(createIcon("⚙"));
        settingsButton.addActionListener(e -> openSettings());
        
        toolBar.add(startButton);
        toolBar.add(stopButton);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL));
        toolBar.add(refreshButton);
        toolBar.add(redetectButton);
        toolBar.add(settingsButton);
        
        return toolBar;
    }
    
    /**
     * 创建主面板
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.7);
        
        // 上半部分：文件列表
        JPanel filePanel = createFilePanel();
        splitPane.setTopComponent(filePanel);
        
        // 下半部分：日志区域
        JPanel logPanel = createLogPanel();
        splitPane.setBottomComponent(logPanel);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * 创建文件列表面板
     */
    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("PDF文件列表"));
        
        // 创建表格
        String[] columnNames = {"序号", "状态", "文件名", "大小", "创建时间", "检测结果", "通知状态", "文件路径"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 不允许编辑
            }
        };
        
        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileTable.setAutoCreateRowSorter(true);
        
        // 设置自定义渲染器，为不同状态添加颜色标识
        fileTable.setDefaultRenderer(Object.class, new StatusTableCellRenderer());
        
        // 设置列宽
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // 序号
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(50);  // 状态
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(200); // 文件名
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // 大小
        fileTable.getColumnModel().getColumn(4).setPreferredWidth(120); // 创建时间
        fileTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // 检测结果
        fileTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // 通知状态
        fileTable.getColumnModel().getColumn(7).setPreferredWidth(300); // 文件路径
        
        // 添加右键菜单
        createTableContextMenu();
        
        JScrollPane scrollPane = new JScrollPane(fileTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 添加文件计数标签
        fileCountLabel = new JLabel("文件数量: 0");
        fileCountLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(fileCountLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建表格右键菜单
     */
    private void createTableContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem openFileItem = new JMenuItem("打开文件");
        openFileItem.addActionListener(e -> openSelectedFile());
        
        JMenuItem openFolderItem = new JMenuItem("打开文件夹");
        openFolderItem.addActionListener(e -> openSelectedFileFolder());
        
        contextMenu.add(openFileItem);
        contextMenu.add(openFolderItem);
        
        fileTable.setComponentPopupMenu(contextMenu);
    }
    
    /**
     * 创建日志面板
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("运行日志"));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 日志控制按钮
        JPanel logControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearLogButton = new JButton("清空日志");
        clearLogButton.addActionListener(e -> logArea.setText(""));
        logControlPanel.add(clearLogButton);
        
        panel.add(logControlPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建状态栏
     */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(new EmptyBorder(2, 5, 2, 5));
        
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(200, 20));
        
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(progressBar, BorderLayout.EAST);
        
        return statusBar;
    }
    
    /**
     * 设置服务回调
     */
    private void setupServiceCallbacks() {
        // 文件扫描回调
        fileScanner.setOnNewFileFound(this::onNewFileFound);
        fileScanner.setOnScanCompleted(this::onScanCompleted);
        
        // PDF检测回调
        pdfDetector.setOnDetectionCompleted(this::onDetectionCompleted);
        
        // 通知服务UI日志回调
        notificationService.setUiLogCallback(msg -> {
            javax.swing.SwingUtilities.invokeLater(() -> appendLog(msg));
        });
    }
    
    /**
     * 设置窗口属性
     */
    private void setupWindow() {
        setTitle("TcPDFTool - PDF文件检测工具");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // 加载窗口配置
        AppConfig config = configManager.getConfig();
        setSize(config.getWindowWidth(), config.getWindowHeight());
        setLocation(config.getWindowX(), config.getWindowY());
        
        // 设置窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
        
        // 设置图标
        setIconImage(createApplicationIcon());
    }
    
    /**
     * 创建应用图标
     */
    private Image createApplicationIcon() {
        // 创建一个简单的PDF图标
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制PDF图标
        g2d.setColor(Color.RED);
        g2d.fillRect(4, 4, 24, 24);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g2d.drawString("PDF", 8, 18);
        
        g2d.dispose();
        return icon;
    }
    
    /**
     * 创建图标
     */
    private Icon createIcon(String text) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                g.drawString(text, x, y + 12);
            }
            
            @Override
            public int getIconWidth() {
                return 16;
            }
            
            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }
    
    // 事件处理方法
    
    /**
     * 开始监控
     */
    private void startMonitoring() {
        if (isRunning) {
            return;
        }
        
        AppConfig config = configManager.getConfig();
        String monitorDir = config.getMonitorDirectory();
        
        if (monitorDir == null || monitorDir.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "请先在设置中配置监控目录", 
                "配置错误", 
                JOptionPane.WARNING_MESSAGE);
            openSettings();
            return;
        }
        
        isRunning = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("正在监控: " + monitorDir);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        
        // 清空表格
        tableModel.setRowCount(0);
        fileCountLabel.setText("文件数量: 0");
        
        // 启动文件扫描
        fileScanner.startScanning();
        
        appendLog("开始监控目录: " + monitorDir);
        logger.info("开始监控目录: {}", monitorDir);
    }
    
    /**
     * 停止监控
     */
    private void stopMonitoring() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("已停止监控");
        progressBar.setVisible(false);
        
        // 停止文件扫描
        fileScanner.stopScanning();
        
        appendLog("已停止监控");
        logger.info("已停止监控");
    }
    
    /**
     * 刷新文件列表
     */
    private void refreshFileList() {
        if (isRunning) {
            appendLog("正在监控中，无需手动刷新");
            return;
        }
        
        AppConfig config = configManager.getConfig();
        String monitorDir = config.getMonitorDirectory();
        
        if (monitorDir == null || monitorDir.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "请先在设置中配置监控目录", 
                "配置错误", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 临时启动扫描获取文件列表
        FileScanner tempScanner = new FileScanner(config);
        tempScanner.setOnScanCompleted(files -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                for (PDFFileInfo fileInfo : files) {
                    addFileToTable(fileInfo);
                    // 异步检测
                    pdfDetector.detectAsync(fileInfo);
                }
                updateFileCount();
                appendLog("刷新完成，发现 " + files.size() + " 个PDF文件");
            });
        });
        
        CompletableFuture.runAsync(() -> {
            tempScanner.startScanning();
            try {
                Thread.sleep(2000); // 等待扫描完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            tempScanner.stopScanning();
        });
        
        appendLog("正在刷新文件列表...");
    }
    
    /**
     * 打开设置对话框
     */
    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this, configManager);
        dialog.setVisible(true);
        
        if (dialog.isConfigChanged()) {
            appendLog("配置已更新，正在重载服务...");
            // 如果正在运行，先停止监控
            if (isRunning) {
                stopMonitoring();
            }
            // 安全关闭旧实例
            try { fileScanner.stopScanning(); } catch (Exception ignored) {}
            try { pdfDetector.shutdown(); } catch (Exception ignored) {}
            try { notificationService.shutdown(); } catch (Exception ignored) {}
            
            // 使用最新配置重建服务并重新绑定回调
            AppConfig cfg = configManager.getConfig();
            fileScanner = new FileScanner(cfg);
            pdfDetector = new PDFDetector(cfg);
            notificationService = new NotificationService(cfg);
            setupServiceCallbacks();
            appendLog("服务已重载完成，新的检测规则将生效");
        }
    }
    
    /**
     * 测试通知
     */
    private void testNotification() {
        CompletableFuture.runAsync(() -> {
            boolean success = notificationService.testNotification();
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    JOptionPane.showMessageDialog(this, 
                        "通知测试成功", 
                        "测试结果", 
                        JOptionPane.INFORMATION_MESSAGE);
                    appendLog("通知测试成功");
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "通知测试失败，请检查BAT脚本配置", 
                        "测试结果", 
                        JOptionPane.ERROR_MESSAGE);
                    appendLog("通知测试失败");
                }
            });
        });
    }
    
    /**
     * 显示关于对话框
     */
    private void showAbout() {
        String message = "TcPDFTool - PDF文件检测工具\n\n" +
                        "版本: 1.0.0\n" +
                        "功能: 自动检测PDF文件内容完整性\n" +
                        "支持: 文件大小检测 + 图像内容分析\n\n" +
                        "© 2024 TcPDFTool";
        
        JOptionPane.showMessageDialog(this, 
            message, 
            "关于 TcPDFTool", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    // 回调方法
    
    /**
     * 新文件发现回调
     */
    private void onNewFileFound(PDFFileInfo fileInfo) {
        SwingUtilities.invokeLater(() -> {
            addFileToTable(fileInfo);
            updateFileCount();
            appendLog("发现新文件: " + fileInfo.getFileName());
            
            // 异步检测
            pdfDetector.detectAsync(fileInfo);
        });
    }
    
    /**
     * 扫描完成回调
     */
    private void onScanCompleted(List<PDFFileInfo> files) {
        SwingUtilities.invokeLater(() -> {
            logger.info("UI收到扫描完成回调，文件数量: {}", files.size());
            
            // 重置批量通知相关状态
            currentScanSuspiciousFiles.clear();
            pendingDetections = files.size();
            totalFilesInCurrentScan = files.size(); // 记录总文件数
            
            for (PDFFileInfo fileInfo : files) {
                logger.info("添加文件到表格: {}", fileInfo.getFileName());
                addFileToTable(fileInfo);
                // 异步检测
                pdfDetector.detectAsync(fileInfo);
            }
            updateFileCount();
            appendLog("扫描完成，发现 " + files.size() + " 个PDF文件");
            logger.info("UI更新完成");
        });
    }
    
    /**
     * 检测完成回调
     */
    private void onDetectionCompleted(PDFFileInfo fileInfo) {
        SwingUtilities.invokeLater(() -> {
            updateFileInTable(fileInfo);
            
            // 减少待检测计数
            pendingDetections--;
            
            if (fileInfo.isSuspiciousEmpty()) {
                appendLog("检测到疑似空文件: " + fileInfo.getFileName());
                // 收集疑似空文件，不立即发送通知
                currentScanSuspiciousFiles.add(fileInfo);
            }
            
            // 如果所有文件都检测完成，统一发送通知
            if (pendingDetections <= 0) {
                sendBatchNotifications();
            }
        });
    }
    
    /**
     * 批量发送通知
     */
    private void sendBatchNotifications() {
        if (currentScanSuspiciousFiles.isEmpty()) {
            logger.info("本轮扫描未发现疑似空文件，无需发送通知");
            return;
        }
        
        logger.info("开始批量发送通知，疑似空文件数量: {}", currentScanSuspiciousFiles.size());
        appendLog("开始发送通知，疑似空文件数量: " + currentScanSuspiciousFiles.size());
        
        // 使用NotificationService的新版批量发送功能，传递总文件数
        CompletableFuture.runAsync(() -> {
            notificationService.sendBatchNotifications(new ArrayList<>(currentScanSuspiciousFiles), totalFilesInCurrentScan);
        }).thenRun(() -> {
            SwingUtilities.invokeLater(() -> {
                // 只更新表格中的通知状态，不显示单个文件的通知结果
                for (PDFFileInfo fileInfo : currentScanSuspiciousFiles) {
                    updateFileInTable(fileInfo);
                }
                // 只显示批量通知的整体结果
                appendLog("本轮批量通知发送完成");
            });
        });
    }
    
    // 表格操作方法
    
    /**
     * 添加文件到表格
     */
    private void addFileToTable(PDFFileInfo fileInfo) {
        logger.info("正在添加文件到表格: {}", fileInfo.getFileName());
        Object[] rowData = {
            tableModel.getRowCount() + 1, // 序号
            fileInfo.getStatusIcon(),
            fileInfo.getFileName(),
            fileInfo.getFormattedFileSize(),
            fileInfo.getShortCreateTime(),
            fileInfo.getDetectionResult().getFullDisplayText(),
            fileInfo.getNotificationStatusText(),
            fileInfo.getFilePath()
        };
        tableModel.addRow(rowData);
        logger.info("文件已添加到表格，当前行数: {}", tableModel.getRowCount());
    }
    
    /**
     * 更新表格中的文件信息
     */
    private void updateFileInTable(PDFFileInfo fileInfo) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String filePath = (String) tableModel.getValueAt(i, 7); // 文件路径列现在是第7列
            if (filePath.equals(fileInfo.getFilePath())) {
                tableModel.setValueAt(fileInfo.getStatusIcon(), i, 1); // 状态列现在是第1列
                tableModel.setValueAt(fileInfo.getDetectionResult().getFullDisplayText(), i, 5); // 检测结果列现在是第5列
                tableModel.setValueAt(fileInfo.getNotificationStatusText(), i, 6); // 通知状态列现在是第6列
                break;
            }
        }
    }
    
    /**
     * 更新文件计数
     */
    private void updateFileCount() {
        int count = tableModel.getRowCount();
        fileCountLabel.setText("文件数量: " + count);
    }
    
    // 右键菜单操作
    
    private void openSelectedFile() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow >= 0) {
            // 将视图行索引转换为模型行索引
            int modelRow = fileTable.convertRowIndexToModel(selectedRow);
            String filePath = (String) tableModel.getValueAt(modelRow, 7); // 文件路径在第7列
            if (filePath != null && !filePath.trim().isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    boolean success = SystemUtil.openFile(file);
                    if (success) {
                        appendLog("打开文件: " + filePath);
                    } else {
                        appendLog("打开文件失败: " + filePath);
                    }
                } else {
                    appendLog("打开文件失败: 文件不存在 - " + filePath);
                }
            } else {
                appendLog("打开文件失败: 文件路径为空");
            }
        }
    }
    
    private void openSelectedFileFolder() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow >= 0) {
            // 将视图行索引转换为模型行索引
            int modelRow = fileTable.convertRowIndexToModel(selectedRow);
            String filePath = (String) tableModel.getValueAt(modelRow, 7); // 文件路径在第7列
            if (filePath != null && !filePath.trim().isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    boolean success = SystemUtil.openFileFolder(file);
                    if (success) {
                        appendLog("打开文件夹: " + file.getParent());
                    } else {
                        appendLog("打开文件夹失败: " + filePath);
                    }
                } else {
                    appendLog("打开文件夹失败: 文件不存在 - " + filePath);
                }
            } else {
                appendLog("打开文件夹失败: 文件路径为空");
            }
        }
    }
    
    /**
     * 添加日志
     */
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    /**
     * 退出应用程序
     */
    private void exitApplication() {
        // 保存窗口配置
        AppConfig config = configManager.getConfig();
        config.setWindowX(getX());
        config.setWindowY(getY());
        config.setWindowWidth(getWidth());
        config.setWindowHeight(getHeight());
        configManager.saveConfig();
        
        // 停止服务
        if (isRunning) {
            stopMonitoring();
        }
        
        // 关闭服务
        pdfDetector.shutdown();
        notificationService.shutdown();
        
        logger.info("应用程序退出");
        System.exit(0);
    }
    
    /**
     * 自定义表格单元格渲染器，为不同状态的文件添加颜色标识
     */
    private class StatusTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // 获取状态列的值（第1列）
            String status = (String) table.getValueAt(row, 1);
            
            if (!isSelected) {
                // 根据状态设置不同的背景颜色
                switch (status) {
                    case "✓": // 正常文件
                        component.setBackground(new Color(230, 255, 230)); // 浅绿色
                        break;
                    case "⚠": // 可疑文件
                        component.setBackground(new Color(255, 245, 230)); // 浅橙色
                        break;
                    case "✗": // 检测失败
                        component.setBackground(new Color(255, 230, 230)); // 浅红色
                        break;
                    case "⏳": // 检测中
                        component.setBackground(new Color(240, 240, 255)); // 浅蓝色
                        break;
                    default:
                        component.setBackground(Color.WHITE);
                        break;
                }
            }
            
            // 为状态列和检测结果列设置更大的字体
            if (column == 1 || column == 5) {
                Font currentFont = component.getFont();
                component.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize() + 1));
            }
            
            return component;
        }
    }
}