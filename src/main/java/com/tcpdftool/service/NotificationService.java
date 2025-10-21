package com.tcpdftool.service;

import com.tcpdftool.config.AppConfig;
import com.tcpdftool.model.PDFFileInfo;
import com.tcpdftool.model.DetectionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 通知服务：执行短信集成jar并把日志同步到UI
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

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

    public NotificationService(AppConfig config) {
        this.config = config;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 异步发送通知（保留但推荐使用同步或批量）
     */
    public CompletableFuture<Boolean> sendNotificationAsync(PDFFileInfo fileInfo) {
        return CompletableFuture.supplyAsync(() -> sendNotification(fileInfo), executorService);
    }

    /**
     * 同步发送单条通知
     */
    public boolean sendNotification(PDFFileInfo fileInfo) {
        if (fileInfo == null) {
            logger.warn("sendNotification传入fileInfo为空");
            return false;
        }
        try {
            boolean ok = executeSmsNotification(fileInfo);
            fileInfo.setNotificationSent(ok);
            if (ok) {
                logger.info("通知发送成功: {}", fileInfo.getFileName());
                uiLog("通知发送成功: " + fileInfo.getFileName());
            } else {
                logger.warn("通知发送失败: {}", fileInfo.getFileName());
                uiLog("通知发送失败: " + fileInfo.getFileName());
            }
            return ok;
        } catch (Exception e) {
            logger.error("通知发送异常: {}", fileInfo.getFileName(), e);
            uiLog("通知发送异常: " + fileInfo.getFileName() + "，错误: " + e.getMessage());
            fileInfo.setNotificationSent(false);
            fileInfo.setErrorMessage(e.getMessage());
            return false;
        }
    }

    /**
     * 执行短信通知
     */
    private boolean executeSmsNotification(PDFFileInfo fileInfo) {
        String callintegJarPath = config.getCallintegJarPath();
        String smsUsername = config.getSmsUsername();
        String smsPhoneNumbers = config.getSmsPhoneNumbers();

        // 验证配置
        if (callintegJarPath == null || callintegJarPath.trim().isEmpty()) {
            logger.warn("callinteg.jar路径未配置");
            return false;
        }
        if (smsUsername == null || smsUsername.trim().isEmpty()) {
            logger.warn("短信用户名未配置");
            return false;
        }
        if (smsPhoneNumbers == null || smsPhoneNumbers.trim().isEmpty()) {
            logger.warn("接收手机号码未配置");
            return false;
        }

        File jarFile = new File(callintegJarPath);
        if (!jarFile.exists()) {
            logger.error("callinteg.jar文件不存在: {}", callintegJarPath);
            return false;
        }

        try {
            // 解析是否为批量消息（createBatchNotificationInfo将filePath设为“批量通知”）
            boolean isBatchMessage = "批量通知".equals(fileInfo.getFilePath());

            // 构建短信消息
            String message;
            if (isBatchMessage) {
                // 批量：直接使用构建好的整段消息（不追加大小、不再前缀“检测到…”）
                message = fileInfo.getFileName();
            } else {
                // 单文件：使用前缀+文件名+大小
                message = String.format("检测到疑似空白PDF文件: %s (大小: %s)",
                        fileInfo.getFileName(), fileInfo.getFormattedFileSize());
            }

            // 获取当前日期
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            String sendTime = dateFormat.format(new Date());

            // 解析当前运行时的java路径，避免依赖外部PATH（跨平台更稳健）
            String osName = System.getProperty("os.name").toLowerCase();
            boolean isWindows = osName.contains("win");
            String javaHome = System.getProperty("java.home");
            String javaPath = javaHome != null
                    ? new File(new File(javaHome, "bin"), isWindows ? "java.exe" : "java").getAbsolutePath()
                    : "java";

            // 使用参数数组构造命令，避免手工引号与空格问题
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(
                    javaPath,
                    "-jar", callintegJarPath,
                    "-integname=SendSMSMessage",
                    "-Username=" + smsUsername,
                    "-msg=" + message,
                    "-tel=" + smsPhoneNumbers,
                    "-SendTime=" + sendTime
            );

            // 友好显示：为-msg值加英文双引号，便于直观核对
            String friendlyCommand = String.format(
                    "%s -jar %s -integname=SendSMSMessage -Username=%s -msg=\"%s\" -tel=%s -SendTime=%s",
                    javaPath,
                    callintegJarPath,
                    smsUsername,
                    message,
                    smsPhoneNumbers,
                    sendTime
            );
            logger.info("执行短信命令: {}", friendlyCommand);
            uiLog("执行短信命令: " + friendlyCommand);

            // 同时记录实际参数列表（用于定位真实执行内容——不带引号）
            String actualArgs = String.join(" ", processBuilder.command());
            logger.info("实际执行参数: {}", actualArgs);
            uiLog("实际执行参数: " + actualArgs);

            // 设置工作目录并合并输出流（便于读取错误输出）
            processBuilder.directory(jarFile.getParentFile());
            processBuilder.redirectErrorStream(true);

            // 启动进程
            Process process = processBuilder.start();

            // 读取子进程输出并同步到日志/UI
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("短信命令输出: {}", line);
                    uiLog("短信命令输出: " + line);
                }
            }

            // 等待进程结束并获取退出码
            int exitCode = process.waitFor();
            boolean success = (exitCode == 0);
            if (success) {
                logger.info("短信命令执行成功，退出码: {}", exitCode);
                uiLog("短信命令执行成功，退出码: " + exitCode);
            } else {
                logger.warn("短信命令执行失败，退出码: {}", exitCode);
                uiLog("短信命令执行失败，退出码: " + exitCode);
            }
            return success;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("执行短信命令异常", e);
            uiLog("执行短信命令异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 测试通知（用于菜单“测试通知”）
     */
    public boolean testNotification() {
        PDFFileInfo fake = new PDFFileInfo("测试文件.PDF", "测试路径", 0L, new Date());
        fake.setDetectionResult(DetectionResult.SUSPICIOUS_EMPTY_SIZE);
        uiLog("开始测试通知...");
        boolean ok = sendNotification(fake);
        uiLog("测试通知完成，结果: " + (ok ? "成功" : "失败"));
        return ok;
    }

    /**
     * 新版批量通知：构建整段消息并发送（含总文件数）
     */
    public void sendBatchNotifications(java.util.List<PDFFileInfo> suspiciousFiles, int totalFileCount) {
        if (suspiciousFiles == null || suspiciousFiles.isEmpty()) {
            logger.info("没有疑似空文件，无需批量通知");
            return;
        }
        uiLog("开始批量通知，疑似空文件数量: " + suspiciousFiles.size());
        logger.info("开始批量通知，疑似空文件数量: {}", suspiciousFiles.size());

        String notificationMessage = buildBatchNotificationMessage(suspiciousFiles, totalFileCount);

        PDFFileInfo batchNotificationInfo = createBatchNotificationInfo(totalFileCount, suspiciousFiles.size(),
                suspiciousFiles.stream().map(PDFFileInfo::getFileName).collect(Collectors.joining("、")));

        // 将消息设置为文件名，这样BAT脚本可以获取到完整的通知内容
        batchNotificationInfo.setFileName(notificationMessage);

        logger.info("准备发送批量通知，使用的消息内容: {}", notificationMessage);
        uiLog("准备发送批量通知，使用的消息内容: " + notificationMessage);

        // 发送批量通知
        boolean success = sendNotification(batchNotificationInfo);

        // 更新所有文件的通知状态
        for (PDFFileInfo fileInfo : suspiciousFiles) {
            fileInfo.setNotificationSent(success);
        }

        if (success) {
            logger.info("批量通知发送成功，涉及 {} 个疑似空文件", suspiciousFiles.size());
            uiLog("批量通知发送成功，涉及 " + suspiciousFiles.size() + " 个疑似空文件");
        } else {
            logger.warn("批量通知发送失败，涉及 {} 个疑似空文件", suspiciousFiles.size());
            uiLog("批量通知发送失败，涉及 " + suspiciousFiles.size() + " 个疑似空文件");
        }
    }

    /**
     * 构建批量通知消息
     */
    private String buildBatchNotificationMessage(java.util.List<PDFFileInfo> suspiciousFiles, int totalFileCount) {
        // 获取当前时间
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String detectionTime = formatter.format(now);

        // 构建文件名列表
        String fileNames = suspiciousFiles.stream()
                .map(PDFFileInfo::getFileName)
                .collect(Collectors.joining("、"));

        // 构建完整消息
        return String.format("本轮共检测%d个PDF，其中疑似%d个PDF为空，PDF名称为：%s。检测时间：%s",
                totalFileCount, suspiciousFiles.size(), fileNames, detectionTime);
    }

    /**
     * 创建批量通知用的临时PDFFileInfo对象
     */
    private PDFFileInfo createBatchNotificationInfo(int totalFiles, int suspiciousCount, String fileNames) {
        // 使用正确的构造函数参数
        PDFFileInfo batchInfo = new PDFFileInfo(
                "批量通知",
                "批量通知",
                0L,
                new Date()
        );
        batchInfo.setDetectionResult(DetectionResult.SUSPICIOUS_EMPTY_SIZE);
        return batchInfo;
    }

    /**
     * 旧版批量通知兼容（未使用）
     */
    public void sendBatchNotifications(java.util.List<PDFFileInfo> fileInfoList) {
        if (fileInfoList == null || fileInfoList.isEmpty()) return;
        for (PDFFileInfo info : fileInfoList) {
            sendNotification(info);
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}