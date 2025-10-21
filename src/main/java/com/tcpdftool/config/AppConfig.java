package com.tcpdftool.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 应用程序配置类
 * 包含所有可配置的参数
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
    
    // 扫描配置
    @JsonProperty("monitorDirectory")
    private String monitorDirectory = System.getProperty("user.home") + "/Documents/PDFs";
    
    @JsonProperty("scanInterval")
    private int scanInterval = 30; // 分钟
    
    @JsonProperty("fileTimeRange")
    private int fileTimeRange = 0; // 文件时间范围(小时), 0=不限制, >0=指定小时数
    
    @JsonProperty("includeSubdirectories")
    private boolean includeSubdirectories = true;
    
    @JsonProperty("autoStartMonitoring")
    private boolean autoStartMonitoring = false;
    
    // 检测配置
    @JsonProperty("enableFileSizeDetection")
    private boolean enableFileSizeDetection = true;
    
    @JsonProperty("fileSizeThreshold")
    private int fileSizeThreshold = 10; // KB
    
    @JsonProperty("enableImageContentDetection")
    private boolean enableImageContentDetection = true;
    
    @JsonProperty("detectionAreaWidth")
    private int detectionAreaWidth = 200; // 像素 - 默认200
    
    @JsonProperty("detectionAreaHeight")
    private int detectionAreaHeight = 200; // 像素 - 默认200
    
    // 新增：百分比宽高（1.0 ~ 100.0），默认等效约200px在900x600参考上
    @JsonProperty("detectionAreaWidthPercent")
    private double detectionAreaWidthPercent = 22.2; // 约 200/900*100
    
    @JsonProperty("detectionAreaHeightPercent")
    private double detectionAreaHeightPercent = 33.3; // 约 200/600*100
    
    // 新增：水平偏移百分比（-100 ~ 100）
    @JsonProperty("horizontalOffsetPercent")
    private int horizontalOffsetPercent = 0;
    
    @JsonProperty("contentPixelDensityThreshold")
    private double contentPixelDensityThreshold = 10.0; // 百分比 - 内容像素密度阈值
    
    // 短信通知配置
    
    // 短信通知配置
    @JsonProperty("enableSmsNotification")
    private boolean enableSmsNotification = false;
    
    @JsonProperty("callintegJarPath")
    private String callintegJarPath = "D:\\Siemens\\Teamcenter13\\bin\\callinteg.jar";
    
    @JsonProperty("smsUsername")
    private String smsUsername = "TC";
    
    @JsonProperty("smsPhoneNumbers")
    private String smsPhoneNumbers = ""; // 多个号码用逗号分隔
    
    // 界面配置
    @JsonProperty("windowWidth")
    private int windowWidth = 1000;
    
    @JsonProperty("windowHeight")
    private int windowHeight = 700;
    
    @JsonProperty("windowX")
    private int windowX = -1; // -1表示居中
    
    @JsonProperty("windowY")
    private int windowY = -1; // -1表示居中
    
    // Getters and Setters
    public String getMonitorDirectory() {
        return monitorDirectory;
    }
    
    public void setMonitorDirectory(String monitorDirectory) {
        this.monitorDirectory = monitorDirectory;
    }
    
    public int getScanInterval() {
        return scanInterval;
    }
    
    public void setScanInterval(int scanInterval) {
        this.scanInterval = scanInterval;
    }
    
    public int getFileTimeRange() {
        return fileTimeRange;
    }
    
    public void setFileTimeRange(int fileTimeRange) {
        this.fileTimeRange = fileTimeRange;
    }
    
    public boolean isIncludeSubdirectories() {
        return includeSubdirectories;
    }
    
    public void setIncludeSubdirectories(boolean includeSubdirectories) {
        this.includeSubdirectories = includeSubdirectories;
    }
    
    public boolean isAutoStartMonitoring() {
        return autoStartMonitoring;
    }
    
    public void setAutoStartMonitoring(boolean autoStartMonitoring) {
        this.autoStartMonitoring = autoStartMonitoring;
    }
    
    public boolean isEnableFileSizeDetection() {
        return enableFileSizeDetection;
    }
    
    public void setEnableFileSizeDetection(boolean enableFileSizeDetection) {
        this.enableFileSizeDetection = enableFileSizeDetection;
    }
    
    public int getFileSizeThreshold() {
        return fileSizeThreshold;
    }
    
    public void setFileSizeThreshold(int fileSizeThreshold) {
        this.fileSizeThreshold = fileSizeThreshold;
    }
    
    public boolean isEnableImageContentDetection() {
        return enableImageContentDetection;
    }
    
    public void setEnableImageContentDetection(boolean enableImageContentDetection) {
        this.enableImageContentDetection = enableImageContentDetection;
    }
    
    public int getDetectionAreaWidth() {
        return detectionAreaWidth;
    }
    
    public void setDetectionAreaWidth(int detectionAreaWidth) {
        this.detectionAreaWidth = detectionAreaWidth;
    }
    
    public int getDetectionAreaHeight() {
        return detectionAreaHeight;
    }
    
    public void setDetectionAreaHeight(int detectionAreaHeight) {
        this.detectionAreaHeight = detectionAreaHeight;
    }
    
    public double getDetectionAreaWidthPercent() {
        return detectionAreaWidthPercent;
    }
    
    public void setDetectionAreaWidthPercent(double detectionAreaWidthPercent) {
        this.detectionAreaWidthPercent = detectionAreaWidthPercent;
    }
    
    public double getDetectionAreaHeightPercent() {
        return detectionAreaHeightPercent;
    }
    
    public void setDetectionAreaHeightPercent(double detectionAreaHeightPercent) {
        this.detectionAreaHeightPercent = detectionAreaHeightPercent;
    }
    
    public int getHorizontalOffsetPercent() {
        return horizontalOffsetPercent;
    }
    
    public void setHorizontalOffsetPercent(int horizontalOffsetPercent) {
        this.horizontalOffsetPercent = horizontalOffsetPercent;
    }
    
    public double getContentPixelDensityThreshold() {
        return contentPixelDensityThreshold;
    }

    public void setContentPixelDensityThreshold(double contentPixelDensityThreshold) {
        this.contentPixelDensityThreshold = contentPixelDensityThreshold;
    }
    
    public boolean isEnableSmsNotification() {
        return enableSmsNotification;
    }
    
    public void setEnableSmsNotification(boolean enableSmsNotification) {
        this.enableSmsNotification = enableSmsNotification;
    }
    
    public String getCallintegJarPath() {
        return callintegJarPath;
    }
    
    public void setCallintegJarPath(String callintegJarPath) {
        this.callintegJarPath = callintegJarPath;
    }
    
    public String getSmsUsername() {
        return smsUsername;
    }
    
    public void setSmsUsername(String smsUsername) {
        this.smsUsername = smsUsername;
    }
    
    public String getSmsPhoneNumbers() {
        return smsPhoneNumbers;
    }
    
    public void setSmsPhoneNumbers(String smsPhoneNumbers) {
        this.smsPhoneNumbers = smsPhoneNumbers;
    }
    
    public int getWindowWidth() {
        return windowWidth;
    }
    
    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }
    
    public int getWindowHeight() {
        return windowHeight;
    }
    
    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }
    
    public int getWindowX() {
        return windowX;
    }
    
    public void setWindowX(int windowX) {
        this.windowX = windowX;
    }
    
    public int getWindowY() {
        return windowY;
    }
    
    public void setWindowY(int windowY) {
        this.windowY = windowY;
    }
    
    /**
     * 创建配置的副本
     */
    public AppConfig copy() {
        AppConfig copy = new AppConfig();
        copy.monitorDirectory = this.monitorDirectory;
        copy.scanInterval = this.scanInterval;
        copy.fileTimeRange = this.fileTimeRange;
        copy.includeSubdirectories = this.includeSubdirectories;
        copy.autoStartMonitoring = this.autoStartMonitoring;
        copy.enableFileSizeDetection = this.enableFileSizeDetection;
        copy.fileSizeThreshold = this.fileSizeThreshold;
        copy.enableImageContentDetection = this.enableImageContentDetection;
        copy.detectionAreaWidth = this.detectionAreaWidth;
        copy.detectionAreaHeight = this.detectionAreaHeight;
        copy.detectionAreaWidthPercent = this.detectionAreaWidthPercent;
        copy.detectionAreaHeightPercent = this.detectionAreaHeightPercent;
        copy.horizontalOffsetPercent = this.horizontalOffsetPercent;
        copy.contentPixelDensityThreshold = this.contentPixelDensityThreshold;
        copy.enableSmsNotification = this.enableSmsNotification;
        copy.callintegJarPath = this.callintegJarPath;
        copy.smsUsername = this.smsUsername;
        copy.smsPhoneNumbers = this.smsPhoneNumbers;
        copy.windowWidth = this.windowWidth;
        copy.windowHeight = this.windowHeight;
        copy.windowX = this.windowX;
        copy.windowY = this.windowY;
        return copy;
    }
}