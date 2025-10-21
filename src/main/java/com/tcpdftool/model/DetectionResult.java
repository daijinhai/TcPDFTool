package com.tcpdftool.model;

/**
 * PDF文件检测结果枚举
 */
public enum DetectionResult {
    
    /**
     * 待检测
     */
    PENDING("待检测", ""),
    
    /**
     * 正常文件
     */
    NORMAL("正常", "文件内容正常"),
    
    /**
     * 疑似空文件 - 文件大小过小
     */
    SUSPICIOUS_EMPTY_SIZE("疑似空", "低于文件大小阈值"),
    
    /**
     * 疑似空文件 - 内容密度过低
     */
    SUSPICIOUS_EMPTY_PIXELS("疑似空", "内容密度低于阈值"),
    
    /**
     * 疑似空文件 - 同时满足大小和内容密度条件
     */
    SUSPICIOUS_EMPTY_BOTH("疑似空", "低于文件大小阈值且内容密度低于阈值"),
    
    /**
     * 检测失败
     */
    DETECTION_FAILED("检测失败", "检测过程中发生错误");
    
    private final String displayName;
    private final String reason;
    
    DetectionResult(String displayName, String reason) {
        this.displayName = displayName;
        this.reason = reason;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getReason() {
        return reason;
    }
    
    /**
     * 获取完整的显示文本（包含原因）
     */
    public String getFullDisplayText() {
        if (reason.isEmpty()) {
            return displayName;
        }
        return displayName + " - " + reason;
    }
    
    @Override
    public String toString() {
        return getFullDisplayText();
    }
}