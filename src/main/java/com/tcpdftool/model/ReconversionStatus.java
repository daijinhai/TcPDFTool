package com.tcpdftool.model;

/**
 * 重新转换状态枚举
 */
public enum ReconversionStatus {
    
    /**
     * 无需转换 - 检测结果正常
     */
    NOT_NEEDED("无需转换", "检测结果正常"),
    
    /**
     * 待转换 - 检测到疑似空文件，等待转换
     */
    PENDING("待转换", "检测到疑似空文件"),
    
    /**
     * 转换中 - 正在执行重新转换
     */
    IN_PROGRESS("转换中", "正在执行重新转换"),
    
    /**
     * 转换成功 - 重新转换完成
     */
    SUCCESS("转换成功", "重新转换完成"),
    
    /**
     * 转换失败 - 重新转换过程中发生错误
     */
    FAILED("转换失败", "重新转换过程中发生错误"),
    
    /**
     * 跳过转换 - 重新转换功能未启用或不满足条件
     */
    SKIPPED("跳过转换", "重新转换功能未启用或不满足条件");
    
    private final String displayName;
    private final String description;
    
    ReconversionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取完整的显示文本（包含描述）
     */
    public String getFullDisplayText() {
        return displayName + " - " + description;
    }
    
    /**
     * 获取状态图标
     */
    public String getStatusIcon() {
        switch (this) {
            case NOT_NEEDED:
                return "-";
            case PENDING:
                return "⏳";
            case IN_PROGRESS:
                return "🔄";
            case SUCCESS:
                return "✓";
            case FAILED:
                return "✗";
            case SKIPPED:
                return "⏭";
            default:
                return "?";
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}