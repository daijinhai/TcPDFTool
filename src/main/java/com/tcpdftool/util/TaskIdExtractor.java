package com.tcpdftool.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 任务ID提取工具类
 * 从PDF文件路径中提取TASKID（祖父目录名）
 */
public class TaskIdExtractor {
    
    /**
     * 从文件路径中提取TASKID
     * 
     * 规则：TASKID为PDF文件路径的祖父目录名
     * 例如：d:\DC\U261EA21XXXXXX\result\hello.pdf -> U261EA21XXXXXX
     * 
     * @param filePath PDF文件的完整路径
     * @return 提取的TASKID，如果无法提取则返回null
     */
    public static String extractTaskId(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 规范化路径，处理不同操作系统的路径分隔符
            Path path = Paths.get(filePath).normalize();
            
            // 获取文件的父目录（通常是result目录）
            Path parentDir = path.getParent();
            if (parentDir == null) {
                return null;
            }
            
            // 获取祖父目录（TASKID目录）
            Path grandParentDir = parentDir.getParent();
            if (grandParentDir == null) {
                return null;
            }
            
            // 返回祖父目录的名称作为TASKID
            String taskId = grandParentDir.getFileName().toString();
            
            // 验证TASKID不为空且不是根目录标识
            if (taskId.isEmpty() || taskId.equals("/") || taskId.equals("\\") || taskId.matches("^[A-Za-z]:$")) {
                return null;
            }
            
            return taskId;
            
        } catch (Exception e) {
            // 路径解析异常，返回null
            return null;
        }
    }
    
    /**
     * 验证提取的TASKID是否有效
     * 
     * @param taskId 要验证的TASKID
     * @return 如果TASKID有效返回true，否则返回false
     */
    public static boolean isValidTaskId(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return false;
        }
        
        // 基本验证：不能是特殊目录名
        String trimmedTaskId = taskId.trim();
        if (trimmedTaskId.equals(".") || trimmedTaskId.equals("..") || 
            trimmedTaskId.equals("/") || trimmedTaskId.equals("\\") ||
            trimmedTaskId.matches("^[A-Za-z]:$")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 从文件路径中提取并验证TASKID
     * 
     * @param filePath PDF文件的完整路径
     * @return 有效的TASKID，如果无法提取或无效则返回null
     */
    public static String extractAndValidateTaskId(String filePath) {
        String taskId = extractTaskId(filePath);
        return isValidTaskId(taskId) ? taskId : null;
    }
    
    /**
     * 获取文件路径的详细信息（用于调试）
     * 
     * @param filePath 文件路径
     * @return 路径分析信息
     */
    public static String getPathAnalysis(String filePath) {
        if (filePath == null) {
            return "文件路径为null";
        }
        
        try {
            Path path = Paths.get(filePath).normalize();
            StringBuilder analysis = new StringBuilder();
            
            analysis.append("原始路径: ").append(filePath).append("\n");
            analysis.append("规范化路径: ").append(path.toString()).append("\n");
            analysis.append("文件名: ").append(path.getFileName()).append("\n");
            
            Path parent = path.getParent();
            if (parent != null) {
                analysis.append("父目录: ").append(parent.getFileName()).append("\n");
                
                Path grandParent = parent.getParent();
                if (grandParent != null) {
                    analysis.append("祖父目录: ").append(grandParent.getFileName()).append("\n");
                    analysis.append("提取的TASKID: ").append(extractTaskId(filePath)).append("\n");
                } else {
                    analysis.append("祖父目录: 不存在\n");
                }
            } else {
                analysis.append("父目录: 不存在\n");
            }
            
            return analysis.toString();
            
        } catch (Exception e) {
            return "路径解析异常: " + e.getMessage();
        }
    }
}