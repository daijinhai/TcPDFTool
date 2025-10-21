package com.tcpdftool.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * PDF文件信息模型
 */
public class PDFFileInfo {
    
    private String fileName;
    private String filePath;
    private long fileSize;
    private Date createTime;
    private Date modifyTime;
    private DetectionResult detectionResult;
    private boolean notificationSent;
    private String errorMessage;
    
    public PDFFileInfo(String fileName, String filePath, long fileSize, Date createTime) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.createTime = createTime;
        this.modifyTime = createTime;
        this.detectionResult = DetectionResult.PENDING;
        this.notificationSent = false;
    }
    
    // Getters and Setters
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Date getModifyTime() {
        return modifyTime;
    }
    
    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }
    
    public DetectionResult getDetectionResult() {
        return detectionResult;
    }
    
    public void setDetectionResult(DetectionResult detectionResult) {
        this.detectionResult = detectionResult;
    }
    
    public boolean isNotificationSent() {
        return notificationSent;
    }
    
    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    // 工具方法
    
    /**
     * 获取格式化的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 获取短格式的创建时间
     */
    public String getShortCreateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm");
        return formatter.format(createTime);
    }
    
    /**
     * 获取完整格式的创建时间
     */
    public String getFullCreateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(createTime);
    }
    
    /**
     * 是否为疑似空文件
     */
    public boolean isSuspiciousEmpty() {
        return detectionResult == DetectionResult.SUSPICIOUS_EMPTY_SIZE ||
               detectionResult == DetectionResult.SUSPICIOUS_EMPTY_PIXELS ||
               detectionResult == DetectionResult.SUSPICIOUS_EMPTY_BOTH;
    }
    
    /**
     * 是否检测正常
     */
    public boolean isNormal() {
        return detectionResult == DetectionResult.NORMAL;
    }
    
    /**
     * 是否检测失败
     */
    public boolean isDetectionFailed() {
        return detectionResult == DetectionResult.DETECTION_FAILED;
    }
    
    /**
     * 获取状态图标
     */
    public String getStatusIcon() {
        switch (detectionResult) {
            case NORMAL:
                return "✓";
            case SUSPICIOUS_EMPTY_SIZE:
            case SUSPICIOUS_EMPTY_PIXELS:
            case SUSPICIOUS_EMPTY_BOTH:
                return "⚠";
            case DETECTION_FAILED:
                return "✗";
            case PENDING:
            default:
                return "⏳";
        }
    }
    
    /**
     * 获取通知状态文本
     */
    public String getNotificationStatusText() {
        if (isSuspiciousEmpty()) {
            return notificationSent ? "已通知" : "未通知";
        }
        return "-";
    }
    
    @Override
    public String toString() {
        return "PDFFileInfo{" +
                "fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", createTime=" + createTime +
                ", detectionResult=" + detectionResult +
                ", notificationSent=" + notificationSent +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        PDFFileInfo that = (PDFFileInfo) o;
        return filePath.equals(that.filePath);
    }
    
    @Override
    public int hashCode() {
        return filePath.hashCode();
    }
}