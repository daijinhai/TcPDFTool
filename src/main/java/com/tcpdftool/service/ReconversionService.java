package com.tcpdftool.service;

import com.tcpdftool.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 重新转换服务：执行bat文件并处理taskId替换
 */
public class ReconversionService {
    private static final Logger logger = LoggerFactory.getLogger(ReconversionService.class);

    private final AppConfig config;
    private final ExecutorService executorService;

    // UI日志回调
    private java.util.function.Consumer<String> uiLogCallback;

    public void setUiLogCallback(java.util.function.Consumer<String> callback) {
        this.uiLogCallback = callback;
    }

    private void uiLog(String message) {
        if (uiLogCallback != null) {
            try {
                uiLogCallback.accept(message);
            } catch (Exception ignored) {
            }
        }
    }

    public ReconversionService(AppConfig config) {
        this.config = config;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 异步执行重新转换
     */
    public CompletableFuture<Boolean> executeReconversionAsync(String taskId) {
        return CompletableFuture.supplyAsync(() -> executeReconversion(taskId), executorService);
    }

    /**
     * 同步执行重新转换
     */
    public boolean executeReconversion(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            logger.warn("executeReconversion传入taskId为空");
            uiLog("错误：taskId不能为空");
            return false;
        }

        try {
            boolean ok = executeBatFile(taskId.trim());
            if (ok) {
                logger.info("重新转换执行成功，taskId: {}", taskId);
                uiLog("重新转换执行成功，taskId: " + taskId);
            } else {
                logger.warn("重新转换执行失败，taskId: {}", taskId);
                uiLog("重新转换执行失败，taskId: " + taskId);
            }
            return ok;
        } catch (Exception e) {
            logger.error("重新转换执行异常，taskId: {}", taskId, e);
            uiLog("重新转换执行异常，taskId: " + taskId + "，错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 执行bat文件
     */
    private boolean executeBatFile(String taskId) {
        String batPath = config.getReconversionBatPath();

        // 验证配置
        if (batPath == null || batPath.trim().isEmpty()) {
            logger.warn("bat文件路径未配置");
            uiLog("错误：bat文件路径未配置");
            return false;
        }

        File batFile = new File(batPath);
        if (!batFile.exists()) {
            logger.error("bat文件不存在: {}", batPath);
            uiLog("错误：bat文件不存在: " + batPath);
            return false;
        }

        try {
            // 读取bat文件内容
            Path batFilePath = Paths.get(batPath);
            byte[] bytes = Files.readAllBytes(batFilePath);
            String batContent = new String(bytes, StandardCharsets.UTF_8);
            
            // 替换taskId占位符
            String processedContent = batContent.replace("{TASKID}", taskId);
            
            // 创建临时bat文件
            Path tempBatPath = Files.createTempFile("reconversion_", ".bat");
            Files.write(tempBatPath, processedContent.getBytes(StandardCharsets.UTF_8));
            
            logger.info("创建临时bat文件: {}", tempBatPath);
            uiLog("创建临时bat文件: " + tempBatPath);
            
            // 构建命令
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            // 检查操作系统，BAT文件只能在Windows系统上执行
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                processBuilder.command("cmd", "/c", tempBatPath.toString());
            } else {
                // 非Windows系统不支持BAT文件执行
                logger.error("重新转换功能仅支持Windows系统，当前系统: {}", osName);
                uiLog("错误：重新转换功能仅支持Windows系统，当前系统: " + osName);
                uiLog("提示：BAT文件是Windows批处理文件，无法在macOS/Linux系统上执行");
                
                // 清理临时文件
                try {
                    Files.deleteIfExists(tempBatPath);
                } catch (IOException e) {
                    logger.warn("删除临时bat文件失败: {}", tempBatPath, e);
                }
                return false;
            }

            logger.info("执行重新转换命令: {}", String.join(" ", processBuilder.command()));
            uiLog("执行重新转换命令: " + String.join(" ", processBuilder.command()));

            // 设置工作目录为bat文件所在目录
            processBuilder.directory(batFile.getParentFile());
            processBuilder.redirectErrorStream(true);

            // 启动进程
            Process process = processBuilder.start();

            // 读取进程输出
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("重新转换命令输出: {}", line);
                    uiLog("重新转换命令输出: " + line);
                }
            }

            // 等待进程结束并获取退出码
            int exitCode = process.waitFor();
            boolean success = (exitCode == 0);
            
            if (success) {
                logger.info("重新转换命令执行成功，退出码: {}", exitCode);
                uiLog("重新转换命令执行成功，退出码: " + exitCode);
            } else {
                logger.warn("重新转换命令执行失败，退出码: {}", exitCode);
                uiLog("重新转换命令执行失败，退出码: " + exitCode);
            }

            // 清理临时文件
            try {
                Files.deleteIfExists(tempBatPath);
                logger.debug("已删除临时bat文件: {}", tempBatPath);
            } catch (IOException e) {
                logger.warn("删除临时bat文件失败: {}", tempBatPath, e);
            }

            return success;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("执行重新转换命令异常", e);
            uiLog("执行重新转换命令异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 测试重新转换功能
     */
    public boolean testReconversion(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            uiLog("错误：测试taskId不能为空");
            return false;
        }
        
        uiLog("开始测试重新转换，taskId: " + taskId);
        boolean ok = executeReconversion(taskId);
        uiLog("测试重新转换完成，结果: " + (ok ? "成功" : "失败"));
        return ok;
    }

    /**
     * 验证bat文件配置是否有效
     */
    public boolean validateBatFileConfig() {
        String batPath = config.getReconversionBatPath();
        
        // 检查操作系统
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("win")) {
            logger.warn("重新转换功能仅支持Windows系统，当前系统: {}", osName);
            return false;
        }
        
        if (batPath == null || batPath.trim().isEmpty()) {
            logger.warn("bat文件路径未配置");
            return false;
        }

        File batFile = new File(batPath);
        if (!batFile.exists()) {
            logger.warn("bat文件不存在: {}", batPath);
            return false;
        }

        try {
            // 检查文件内容是否包含{TASKID}占位符
            Path batFilePath = Paths.get(batPath);
            byte[] bytes = Files.readAllBytes(batFilePath);
            String content = new String(bytes, StandardCharsets.UTF_8);
            
            if (!content.contains("{TASKID}")) {
                logger.warn("bat文件不包含{TASKID}占位符: {}", batPath);
                return false;
            }
            
            return true;
        } catch (IOException e) {
            logger.error("读取bat文件失败: {}", batPath, e);
            return false;
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}