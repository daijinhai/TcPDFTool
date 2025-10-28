package com.tcpdftool.ui;

import com.tcpdftool.config.AppConfig;
import com.tcpdftool.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * 设置对话框
 */
public class SettingsDialog extends JDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(SettingsDialog.class);
    
    private final ConfigManager configManager;
    private AppConfig config;
    private boolean configChanged = false;
    
    // 扫描设置组件
    private JTextField monitorDirField;
    private JSpinner scanIntervalSpinner;
    private JSpinner fileTimeRangeSpinner;
    private JCheckBox includeSubdirsCheckBox;
    private JCheckBox autoStartCheckBox;
    
    // 检测设置组件
    private JSpinner fileSizeThresholdSpinner;
    private JCheckBox enableImageDetectionCheckBox;
    // 原有的单一尺寸拆分为宽/高两个输入
    private JSpinner detectionAreaWidthSpinner;
    private JSpinner detectionAreaHeightSpinner;
    // 新增：水平偏移百分比
    private JSpinner horizontalOffsetSpinner;
    private JSpinner whitePixelThresholdSpinner;
    // 预览面板
    private JPanel imagePreviewPanel;
    
    // 短信通知组件
    private JCheckBox enableSmsNotificationCheckBox;
    private JTextField callintegJarPathField;
    private JTextField smsUsernameField;
    private JTextField smsPhoneNumbersField;
    
    // 重新转换设置组件
    private JCheckBox enableReconversionCheckBox;
    private JTextField reconversionBatPathField;
    
    public SettingsDialog(Frame parent, ConfigManager configManager) {
        super(parent, "设置", true);
        this.configManager = configManager;
        this.config = configManager.getConfig().copy(); // 创建副本
        
        initializeUI();
        loadConfigToUI();
        setupWindow();
        
        logger.info("设置对话框初始化完成");
    }
    
    /**
     * 初始化UI
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // 创建选项卡面板
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // 扫描设置选项卡
        JPanel scanPanel = createScanSettingsPanel();
        tabbedPane.addTab("扫描设置", scanPanel);
        
        // 检测设置选项卡
        JPanel detectionPanel = createDetectionSettingsPanel();
        tabbedPane.addTab("检测设置", detectionPanel);
        
        // 通知设置选项卡
        JPanel notificationPanel = createNotificationSettingsPanel();
        tabbedPane.addTab("通知设置", notificationPanel);
        
        // 重新转换设置选项卡
        JPanel reconversionPanel = createReconversionSettingsPanel();
        tabbedPane.addTab("重新转换设置", reconversionPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 创建扫描设置面板
     */
    private JPanel createScanSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 监控目录
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel("监控目录:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        monitorDirField = new JTextField();
        panel.add(monitorDirField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JButton browseButton = new JButton("浏览");
        browseButton.addActionListener(e -> browseMonitorDirectory());
        panel.add(browseButton, gbc);
        
        // 扫描间隔
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("扫描间隔(秒):"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        scanIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 3600, 5));
        panel.add(scanIntervalSpinner, gbc);
        
        // 文件时间范围
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("文件时间范围(小时):"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        fileTimeRangeSpinner = new JSpinner(new SpinnerNumberModel(24, 1, 168, 1));
        panel.add(fileTimeRangeSpinner, gbc);
        
        // 包含子目录
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        includeSubdirsCheckBox = new JCheckBox("包含子目录");
        panel.add(includeSubdirsCheckBox, gbc);
        
        // 自动开始监控
        gbc.gridy = 4;
        autoStartCheckBox = new JCheckBox("启动时自动开始监控");
        panel.add(autoStartCheckBox, gbc);
        
        // 添加说明文本
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        JTextArea helpText = new JTextArea(
            "说明:\n" +
            "• 监控目录: 指定要监控的PDF文件目录\n" +
            "• 扫描间隔: 定期扫描新文件的时间间隔\n" +
            "• 文件时间范围: 只检测指定时间范围内创建的文件\n" +
            "• 包含子目录: 是否递归扫描子目录\n" +
            "• 自动开始监控: 程序启动时是否自动开始监控"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setFont(helpText.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(helpText, gbc);
        
        return panel;
    }
    
    /**
     * 创建重新转换设置面板
     */
    private JPanel createReconversionSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 启用重新转换
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        enableReconversionCheckBox = new JCheckBox("启用重新转换功能");
        enableReconversionCheckBox.addActionListener(e -> updateReconversionControls());
        panel.add(enableReconversionCheckBox, gbc);
        
        // bat文件路径
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        JLabel batPathLabel = new JLabel("BAT文件路径:");
        panel.add(batPathLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        reconversionBatPathField = new JTextField();
        panel.add(reconversionBatPathField, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton browseBatButton = new JButton("浏览");
        browseBatButton.addActionListener(e -> browseReconversionBat());
        panel.add(browseBatButton, gbc);
        
        // taskId输入和测试按钮
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        JLabel taskIdLabel = new JLabel("测试TaskId:");
        panel.add(taskIdLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JTextField testTaskIdField = new JTextField();
        testTaskIdField.setToolTipText("输入用于测试的TaskId");
        panel.add(testTaskIdField, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton testConversionButton = new JButton("测试转换");
        testConversionButton.addActionListener(e -> testReconversion(testTaskIdField.getText()));
        panel.add(testConversionButton, gbc);
        
        // 添加说明文本
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 5, 5, 5);
        JTextArea helpText = new JTextArea(
            "说明:\n" +
            "• 重新转换功能: 通过执行BAT脚本进行文档重新转换\n" +
            "• BAT文件内容应包含TC_ROOT、TC_DATA设置和dispatcher_util.exe调用\n" +
            "• TaskId动态替换: BAT文件中使用{TASKID}作为占位符，系统会自动替换\n\n" +
            "BAT文件内容模板:\n" +
            "SET TC_ROOT=D:\\Siemens\\Teamcenter13\n" +
            "SET TC_DATA=Z:\\\n" +
            "Z:\\\\tc_profilevars\n" +
            "D:\\Siemens\\Teamcenter13\\bin\\dispatcher_util.exe -u=dcproxy -p=Deptc!2# -g=dba -a=resubmit -force -taskid={TASKID}\n\n" +
            "使用步骤:\n" +
            "1. 选择包含{TASKID}占位符的BAT文件\n" +
            "2. 启用重新转换功能\n" +
            "3. 输入测试TaskId并点击\"测试转换\"验证配置\n\n" +
            "注意:\n" +
            "• 确保BAT文件路径正确且文件存在\n" +
            "• BAT文件必须包含{TASKID}占位符\n" +
            "• 请根据实际环境调整路径和参数\n" +
            "• 测试前请确保Teamcenter环境已正确配置"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setFont(helpText.getFont().deriveFont(12f));
        helpText.setBorder(BorderFactory.createTitledBorder("使用说明"));
        panel.add(helpText, gbc);
        
        return panel;
    }
    
    /**
     * 创建检测设置面板
     */
    private JPanel createDetectionSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 文件大小检测
        TitledBorder sizeBorder = BorderFactory.createTitledBorder("文件大小检测");
        JPanel sizePanel = new JPanel(new GridBagLayout());
        sizePanel.setBorder(sizeBorder);
        
        GridBagConstraints sizeGbc = new GridBagConstraints();
        sizeGbc.gridx = 0; sizeGbc.gridy = 0;
        sizeGbc.anchor = GridBagConstraints.WEST;
        sizeGbc.insets = new Insets(5, 5, 5, 5);
        sizePanel.add(new JLabel("文件大小阈值(KB):"), sizeGbc);
        
        sizeGbc.gridx = 1;
        sizeGbc.fill = GridBagConstraints.HORIZONTAL;
        sizeGbc.weightx = 1.0;
        fileSizeThresholdSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        sizePanel.add(fileSizeThresholdSpinner, sizeGbc);
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(sizePanel, gbc);
        
        // 图像内容检测
        TitledBorder imageBorder = BorderFactory.createTitledBorder("图像内容检测");
        JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setBorder(imageBorder);
        
        GridBagConstraints imageGbc = new GridBagConstraints();
        imageGbc.gridx = 0; imageGbc.gridy = 0;
        imageGbc.gridwidth = 2;
        imageGbc.anchor = GridBagConstraints.WEST;
        imageGbc.insets = new Insets(5, 5, 5, 5);
        enableImageDetectionCheckBox = new JCheckBox("启用图像内容检测");
        enableImageDetectionCheckBox.addActionListener(e -> updateImageDetectionControls());
        imagePanel.add(enableImageDetectionCheckBox, imageGbc);
        
        // 宽
        imageGbc.gridx = 0; imageGbc.gridy = 1;
        imageGbc.gridwidth = 1;
        imageGbc.fill = GridBagConstraints.NONE;
        imagePanel.add(new JLabel("检测区域宽(%):"), imageGbc);
        
        imageGbc.gridx = 1;
        imageGbc.fill = GridBagConstraints.HORIZONTAL;
        imageGbc.weightx = 1.0;
        detectionAreaWidthSpinner = new JSpinner(new SpinnerNumberModel(22.2, 1.0, 100.0, 0.1));
        ((JSpinner.DefaultEditor) detectionAreaWidthSpinner.getEditor()).getTextField().setColumns(6);
        imagePanel.add(detectionAreaWidthSpinner, imageGbc);
        
        // 高
        imageGbc.gridx = 0; imageGbc.gridy = 2;
        imageGbc.fill = GridBagConstraints.NONE;
        imagePanel.add(new JLabel("检测区域高(%):"), imageGbc);
        
        imageGbc.gridx = 1;
        imageGbc.fill = GridBagConstraints.HORIZONTAL;
        imageGbc.weightx = 1.0;
        detectionAreaHeightSpinner = new JSpinner(new SpinnerNumberModel(33.3, 1.0, 100.0, 0.1));
        ((JSpinner.DefaultEditor) detectionAreaHeightSpinner.getEditor()).getTextField().setColumns(6);
        imagePanel.add(detectionAreaHeightSpinner, imageGbc);
        
        // 水平偏移
        imageGbc.gridx = 0; imageGbc.gridy = 3;
        imageGbc.fill = GridBagConstraints.NONE;
        imagePanel.add(new JLabel("水平偏移(%):"), imageGbc);
        
        imageGbc.gridx = 1;
        imageGbc.fill = GridBagConstraints.HORIZONTAL;
        imageGbc.weightx = 1.0;
        horizontalOffsetSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
        ((JSpinner.DefaultEditor) horizontalOffsetSpinner.getEditor()).getTextField().setColumns(6);
        imagePanel.add(horizontalOffsetSpinner, imageGbc);
        
        // 内容密度
        imageGbc.gridx = 0; imageGbc.gridy = 4;
        imageGbc.fill = GridBagConstraints.NONE;
        JLabel contentDensityLabel = new JLabel("内容密度阈值(%):");
        imagePanel.add(contentDensityLabel, imageGbc);
        
        imageGbc.gridx = 1;
        imageGbc.fill = GridBagConstraints.HORIZONTAL;
        imageGbc.weightx = 1.0;
        whitePixelThresholdSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.1, 50.0, 0.1));
        imagePanel.add(whitePixelThresholdSpinner, imageGbc);
        
        // 预览画布（9:6比例）
        imageGbc.gridx = 0; imageGbc.gridy = 5;
        imageGbc.gridwidth = 2;
        imageGbc.fill = GridBagConstraints.BOTH;
        imageGbc.weightx = 1.0; imageGbc.weighty = 1.0;
        imagePreviewPanel = createImagePreviewPanel();
        imagePanel.add(imagePreviewPanel, imageGbc);
        
        // 监听变化，实时刷新预览
        detectionAreaWidthSpinner.addChangeListener(e -> imagePreviewPanel.repaint());
        detectionAreaHeightSpinner.addChangeListener(e -> imagePreviewPanel.repaint());
        horizontalOffsetSpinner.addChangeListener(e -> imagePreviewPanel.repaint());
        whitePixelThresholdSpinner.addChangeListener(e -> imagePreviewPanel.repaint());
        enableImageDetectionCheckBox.addActionListener(e -> imagePreviewPanel.repaint());
        
        gbc.gridy = 1;
        panel.add(imagePanel, gbc);
        
        // 添加说明文本
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        JTextArea helpText = new JTextArea(
            "说明:\n" +
            "• 文件大小阈值: 小于此大小的PDF文件被认为可能是空文件\n" +
            "• 图像内容检测: 通过分析PDF第一页的图像内容来判断是否为空\n" +
            "• 检测区域宽/高(%): 相对图像尺寸的比例，默认约为22.2%/33.3%\n" +
            "• 水平偏移(%): 相对图像中心的水平位移，负左正右，0居中\n" +
            "• 预览画布: 固定参考分辨率900×600，红色矩形为检测区域，红点为中心"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setFont(helpText.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(helpText, gbc);
        
        return panel;
    }
    
    /**
     * 创建通知设置面板
     */
    private JPanel createNotificationSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 启用短信通知
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        enableSmsNotificationCheckBox = new JCheckBox("启用短信通知");
        enableSmsNotificationCheckBox.addActionListener(e -> updateSmsNotificationControls());
        panel.add(enableSmsNotificationCheckBox, gbc);
        
        // callinteg.jar路径
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        JLabel callintegJarLabel = new JLabel("callinteg.jar路径:");
        panel.add(callintegJarLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        callintegJarPathField = new JTextField();
        panel.add(callintegJarPathField, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton browseJarButton = new JButton("浏览");
        browseJarButton.addActionListener(e -> browseCallintegJar());
        panel.add(browseJarButton, gbc);
        
        // 短信用户名
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        JLabel smsUsernameLabel = new JLabel("短信用户名:");
        panel.add(smsUsernameLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        smsUsernameField = new JTextField();
        panel.add(smsUsernameField, gbc);
        
        // 接收手机号码
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        JLabel smsPhoneLabel = new JLabel("接收手机号:");
        panel.add(smsPhoneLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        smsPhoneNumbersField = new JTextField();
        panel.add(smsPhoneNumbersField, gbc);
        
        // 测试按钮
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(10, 5, 5, 5);
        JButton testButton = new JButton("测试通知");
        testButton.addActionListener(e -> testNotification());
        panel.add(testButton, gbc);
        
        // 添加说明文本
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        JTextArea helpText = new JTextArea(
            "说明:\n" +
            "• 短信通知: 通过callinteg.jar发送短信通知\n" +
            "• 接收手机号: 支持多个号码，用逗号分隔，如: 13800138000,13900139000\n" +
            "• 测试通知: 点击可测试通知功能是否正常工作\n\n" +
            "callinteg.jar调用示例:\n" +
            "java -jar callinteg.jar -integname=SendSMSMessage -Username=TC\n" +
            "     -msg=\"检测到空白PDF文件\" -tel=手机号 -SendTime=日期\n\n" +
            "注意:\n" +
            "• 确保callinteg.jar路径正确且文件存在\n" +
            "• 短信用户名需要与系统配置一致\n" +
            "• 手机号码格式需要正确"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setFont(helpText.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(helpText, gbc);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JButton okButton = new JButton("确定");
        okButton.addActionListener(e -> saveAndClose());
        
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> dispose());
        
        JButton applyButton = new JButton("应用");
        applyButton.addActionListener(e -> saveConfig());
        
        JButton resetButton = new JButton("重置");
        resetButton.addActionListener(e -> resetToDefaults());
        
        panel.add(resetButton);
        panel.add(applyButton);
        panel.add(cancelButton);
        panel.add(okButton);
        
        return panel;
    }
    
    /**
     * 设置窗口属性
     */
    private void setupWindow() {
        setSize(600, 500);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
    }
    
    /**
     * 加载配置到UI
     */
    private void loadConfigToUI() {
        // 扫描设置
        monitorDirField.setText(config.getMonitorDirectory());
        scanIntervalSpinner.setValue(config.getScanInterval());
        fileTimeRangeSpinner.setValue(config.getFileTimeRange());
        includeSubdirsCheckBox.setSelected(config.isIncludeSubdirectories());
        autoStartCheckBox.setSelected(config.isAutoStartMonitoring());
        
        // 检测设置
        fileSizeThresholdSpinner.setValue(config.getFileSizeThreshold());
        enableImageDetectionCheckBox.setSelected(config.isEnableImageContentDetection());
        // 使用百分比宽高
        detectionAreaWidthSpinner.setValue(config.getDetectionAreaWidthPercent());
        detectionAreaHeightSpinner.setValue(config.getDetectionAreaHeightPercent());
        horizontalOffsetSpinner.setValue(config.getHorizontalOffsetPercent());
        whitePixelThresholdSpinner.setValue(config.getContentPixelDensityThreshold());
        
        // 短信通知设置
        enableSmsNotificationCheckBox.setSelected(config.isEnableSmsNotification());
        callintegJarPathField.setText(config.getCallintegJarPath());
        smsUsernameField.setText(config.getSmsUsername());
        smsPhoneNumbersField.setText(config.getSmsPhoneNumbers());
        
        // 重新转换设置
        enableReconversionCheckBox.setSelected(config.isEnableReconversion());
        reconversionBatPathField.setText(config.getReconversionBatPath());
        
        // 更新控件状态
        updateImageDetectionControls();
        updateSmsNotificationControls();
        updateReconversionControls();
    }
    
    /**
     * 从UI保存配置
     */
    private void saveConfigFromUI() {
        // 扫描设置
        config.setMonitorDirectory(monitorDirField.getText().trim());
        config.setScanInterval((Integer) scanIntervalSpinner.getValue());
        config.setFileTimeRange((Integer) fileTimeRangeSpinner.getValue());
        config.setIncludeSubdirectories(includeSubdirsCheckBox.isSelected());
        config.setAutoStartMonitoring(autoStartCheckBox.isSelected());
        
        // 检测设置
        config.setFileSizeThreshold((Integer) fileSizeThresholdSpinner.getValue());
        config.setEnableImageContentDetection(enableImageDetectionCheckBox.isSelected());
        // 保存百分比宽高
        config.setDetectionAreaWidthPercent(((Number) detectionAreaWidthSpinner.getValue()).doubleValue());
        config.setDetectionAreaHeightPercent(((Number) detectionAreaHeightSpinner.getValue()).doubleValue());
        config.setHorizontalOffsetPercent((Integer) horizontalOffsetSpinner.getValue());
        config.setContentPixelDensityThreshold((Double) whitePixelThresholdSpinner.getValue());
        
        // 短信通知设置
        config.setEnableSmsNotification(enableSmsNotificationCheckBox.isSelected());
        config.setCallintegJarPath(callintegJarPathField.getText().trim());
        config.setSmsUsername(smsUsernameField.getText().trim());
        config.setSmsPhoneNumbers(smsPhoneNumbersField.getText().trim());
        
        // 重新转换设置
        config.setEnableReconversion(enableReconversionCheckBox.isSelected());
        config.setReconversionBatPath(reconversionBatPathField.getText().trim());
    }
    
    /**
     * 更新图像检测控件状态
     */
    private void updateImageDetectionControls() {
        boolean enabled = enableImageDetectionCheckBox.isSelected();
        detectionAreaWidthSpinner.setEnabled(enabled);
        detectionAreaHeightSpinner.setEnabled(enabled);
        horizontalOffsetSpinner.setEnabled(enabled);
        whitePixelThresholdSpinner.setEnabled(enabled);
        if (imagePreviewPanel != null) {
            imagePreviewPanel.setEnabled(enabled);
            imagePreviewPanel.repaint();
        }
    }
    
    private JPanel createImagePreviewPanel() {
        JPanel preview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int panelW = getWidth();
                int panelH = getHeight();
                int padding = 10;
                double targetRatio = 900.0 / 600.0; // 9:6
                double panelRatio = (double) (panelW - 2 * padding) / (panelH - 2 * padding);

                int borderW, borderH;
                if (panelRatio > targetRatio) {
                    borderH = panelH - 2 * padding;
                    borderW = (int) Math.round(borderH * targetRatio);
                } else {
                    borderW = panelW - 2 * padding;
                    borderH = (int) Math.round(borderW / targetRatio);
                }
                int offsetX = (panelW - borderW) / 2;
                int offsetY = (panelH - borderH) / 2;

                // 背景
                g2.setColor(new Color(245, 245, 245));
                g2.fillRect(offsetX, offsetY, borderW, borderH);
                // 边框
                g2.setColor(new Color(180, 180, 180));
                g2.drawRect(offsetX, offsetY, borderW, borderH);

                // 从控件获取当前参数（百分比）
                double areaWPercent = detectionAreaWidthSpinner != null ? ((Number) detectionAreaWidthSpinner.getValue()).doubleValue() : config.getDetectionAreaWidthPercent();
                double areaHPercent = detectionAreaHeightSpinner != null ? ((Number) detectionAreaHeightSpinner.getValue()).doubleValue() : config.getDetectionAreaHeightPercent();
                int offsetPercent = horizontalOffsetSpinner != null ? (Integer) horizontalOffsetSpinner.getValue() : config.getHorizontalOffsetPercent();

                // 按参考边框直接用百分比计算ROI尺寸
                int roiW = (int) Math.round(borderW * (areaWPercent / 100.0));
                int roiH = (int) Math.round(borderH * (areaHPercent / 100.0));
                int centerX = offsetX + borderW / 2 + (int) Math.round(offsetPercent / 100.0 * (borderW / 2.0));
                int centerY = offsetY + borderH / 2;

                int startX = centerX - roiW / 2;
                int startY = centerY - roiH / 2;
                int endX = startX + roiW;
                int endY = startY + roiH;

                // 计算裁剪后可见区域
                int drawX = Math.max(offsetX, startX);
                int drawY = Math.max(offsetY, startY);
                int drawEndX = Math.min(offsetX + borderW, endX);
                int drawEndY = Math.min(offsetY + borderH, endY);
                int drawW = Math.max(0, drawEndX - drawX);
                int drawH = Math.max(0, drawEndY - drawY);

                // 绘制检测区域
                g2.setColor(new Color(220, 0, 0));
                if (drawW > 0 && drawH > 0) {
                    g2.drawRect(drawX, drawY, drawW, drawH);
                }

                // 中心点（裁剪到边框内）
                int cx = Math.max(offsetX, Math.min(offsetX + borderW - 1, centerX));
                int cy = Math.max(offsetY, Math.min(offsetY + borderH - 1, centerY));
                g2.fillOval(cx - 3, cy - 3, 6, 6);

                // 中线（增强位置感知）
                g2.setColor(new Color(200, 200, 200));
                g2.drawLine(offsetX, offsetY + borderH / 2, offsetX + borderW, offsetY + borderH / 2);
                g2.drawLine(offsetX + borderW / 2, offsetY, offsetX + borderW / 2, offsetY + borderH);

                g2.dispose();
            }
        };
        preview.setPreferredSize(new Dimension(450, 300)); // 9:6比例
        return preview;
    }
    
    private void updateSmsNotificationControls() {
        boolean enabled = enableSmsNotificationCheckBox.isSelected();
        callintegJarPathField.setEnabled(enabled);
        smsUsernameField.setEnabled(enabled);
        smsPhoneNumbersField.setEnabled(enabled);
    }
    
    /**
     * 更新重新转换控件状态
     */
    private void updateReconversionControls() {
        boolean enabled = enableReconversionCheckBox.isSelected();
        reconversionBatPathField.setEnabled(enabled);
    }

    /**
     * 浏览重新转换BAT文件
     */
    private void browseReconversionBat() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("选择重新转换BAT文件");
        
        // 设置文件过滤器，只显示bat文件
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".bat");
            }
            
            @Override
            public String getDescription() {
                return "批处理文件 (*.bat)";
            }
        });
        
        // 如果当前有路径，设置为默认目录
        String currentPath = reconversionBatPathField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                fileChooser.setCurrentDirectory(currentFile.getParentFile());
            }
        }
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            reconversionBatPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * 测试重新转换功能
     */
    private void testReconversion(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "请输入测试TaskId！", 
                "输入错误", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 检查操作系统
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("win")) {
            JOptionPane.showMessageDialog(this, 
                "重新转换功能仅支持Windows系统！\n" +
                "当前系统: " + osName + "\n" +
                "BAT文件是Windows批处理文件，无法在macOS/Linux系统上执行。", 
                "系统不支持", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 临时保存当前UI配置
        saveConfigFromUI();
        
        // 创建临时重新转换服务进行测试
        com.tcpdftool.service.ReconversionService testService = 
            new com.tcpdftool.service.ReconversionService(config);
        
        SwingUtilities.invokeLater(() -> {
            boolean success = testService.testReconversion(taskId.trim());
            if (success) {
                JOptionPane.showMessageDialog(this, 
                    "重新转换测试成功！", 
                    "测试结果", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "重新转换测试失败，请检查BAT文件路径和内容配置。\n" +
                    "确保BAT文件存在且包含{TASKID}占位符。", 
                    "测试结果", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    /**
     * 浏览监控目录
     */
    private void browseMonitorDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择监控目录");
        
        String currentDir = monitorDirField.getText().trim();
        if (!currentDir.isEmpty()) {
            chooser.setCurrentDirectory(new File(currentDir));
        }
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            monitorDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * 浏览callinteg.jar文件
     */
    private void browseCallintegJar() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("选择callinteg.jar文件");
        
        // 设置文件过滤器，只显示jar文件
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
            }
            
            @Override
            public String getDescription() {
                return "JAR文件 (*.jar)";
            }
        });
        
        // 如果当前有路径，设置为默认目录
        String currentPath = callintegJarPathField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                fileChooser.setCurrentDirectory(currentFile.getParentFile());
            }
        }
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            callintegJarPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * 测试通知功能
     */
    private void testNotification() {
        // 临时保存当前UI配置
        saveConfigFromUI();
        
        // 创建临时通知服务进行测试
        com.tcpdftool.service.NotificationService testService = 
            new com.tcpdftool.service.NotificationService(config);
        
        SwingUtilities.invokeLater(() -> {
            boolean success = testService.testNotification();
            if (success) {
                JOptionPane.showMessageDialog(this, 
                    "通知测试成功！", 
                    "测试结果", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "通知测试失败，请检查callinteg.jar路径、用户名和手机号码配置。", 
                    "测试结果", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        try {
            // 验证配置
            if (!validateConfig()) {
                return;
            }
            
            // 保存UI配置到临时对象
            saveConfigFromUI();
            
            // 更新实际配置
            configManager.updateConfig(config);
            configManager.saveConfig();
            
            configChanged = true;
            
            JOptionPane.showMessageDialog(this, 
                "配置已保存", 
                "保存成功", 
                JOptionPane.INFORMATION_MESSAGE);
            
            logger.info("配置已保存");
            
        } catch (Exception e) {
            logger.error("保存配置失败", e);
            JOptionPane.showMessageDialog(this, 
                "保存配置失败: " + e.getMessage(), 
                "保存失败", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 保存并关闭
     */
    private void saveAndClose() {
        saveConfig();
        if (configChanged) {
            dispose();
        }
    }
    
    /**
     * 重置为默认值
     */
    private void resetToDefaults() {
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要重置所有设置为默认值吗？", 
            "确认重置", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            config = new AppConfig(); // 创建新的默认配置
            loadConfigToUI();
        }
    }
    
    /**
     * 验证配置
     */
    private boolean validateConfig() {
        // 验证监控目录
        String monitorDir = monitorDirField.getText().trim();
        if (monitorDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "请指定监控目录", 
                "配置错误", 
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        File dir = new File(monitorDir);
        if (!dir.exists() || !dir.isDirectory()) {
            JOptionPane.showMessageDialog(this, 
                "监控目录不存在或不是有效目录", 
                "配置错误", 
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // 验证短信通知配置（如果启用）
        if (enableSmsNotificationCheckBox.isSelected()) {
            String jarPath = callintegJarPathField.getText().trim();
            if (jarPath.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "启用短信通知时必须指定callinteg.jar路径", 
                    "配置错误", 
                    JOptionPane.WARNING_MESSAGE);
                return false;
            }
            
            File jarFile = new File(jarPath);
            if (!jarFile.exists() || !jarFile.isFile()) {
                JOptionPane.showMessageDialog(this, 
                    "callinteg.jar文件不存在", 
                    "配置错误", 
                    JOptionPane.WARNING_MESSAGE);
                return false;
            }
            
            String username = smsUsernameField.getText().trim();
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "启用短信通知时必须指定用户名", 
                    "配置错误", 
                    JOptionPane.WARNING_MESSAGE);
                return false;
            }
            
            String phoneNumbers = smsPhoneNumbersField.getText().trim();
            if (phoneNumbers.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "启用短信通知时必须指定接收手机号码", 
                    "配置错误", 
                    JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 是否配置已更改
     */
    public boolean isConfigChanged() {
        return configChanged;
    }
}