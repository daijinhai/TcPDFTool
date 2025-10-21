package com.tcpdftool.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 日志工具类
 * 负责日志系统的初始化和配置
 */
public class LoggerUtil {
    
    private static final String LOG_DIR = System.getProperty("user.home") + "/.tcpdftool/logs";
    private static final String LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n";
    
    /**
     * 初始化日志系统
     */
    public static void initializeLogging() {
        try {
            // 创建日志目录
            createLogDirectoryIfNotExists();
            
            // 获取LoggerContext
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // 清理现有的appender，避免重复配置
            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAndStopAllAppenders();
            
            // 配置控制台输出
            configureConsoleAppender(context);
            
            // 配置文件输出
            configureFileAppender(context);
            
            // 设置根日志级别为DEBUG，以便显示所有调试信息
            rootLogger.setLevel(Level.DEBUG);
            
        } catch (Exception e) {
            System.err.println("初始化日志系统失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 配置控制台输出
     */
    private static void configureConsoleAppender(LoggerContext context) {
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setName("console");
        
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(LOG_PATTERN);
        encoder.start();
        
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();
        
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(consoleAppender);
    }
    
    /**
     * 配置文件输出
     */
    private static void configureFileAppender(LoggerContext context) {
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setName("file");
        
        // 使用日期作为日志文件名
        String logFileName = LOG_DIR + "/tcpdftool-" + 
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log";
        fileAppender.setFile(logFileName);
        
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(LOG_PATTERN);
        encoder.start();
        
        fileAppender.setEncoder(encoder);
        fileAppender.start();
        
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(fileAppender);
    }
    
    /**
     * 创建日志目录（如果不存在）
     */
    private static void createLogDirectoryIfNotExists() throws IOException {
        Path logDir = Paths.get(LOG_DIR);
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }
    }
    
    /**
     * 获取日志目录路径
     */
    public static String getLogDirectory() {
        return LOG_DIR;
    }
    
    /**
     * 获取当前日志文件路径
     */
    public static String getCurrentLogFile() {
        return LOG_DIR + "/tcpdftool-" + 
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log";
    }
    
    /**
     * 清理旧日志文件（保留最近7天）
     */
    public static void cleanupOldLogFiles() {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                return;
            }
            
            File[] logFiles = logDir.listFiles((dir, name) -> 
                name.startsWith("tcpdftool-") && name.endsWith(".log"));
            
            if (logFiles == null) {
                return;
            }
            
            long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
            
            for (File logFile : logFiles) {
                if (logFile.lastModified() < sevenDaysAgo) {
                    if (logFile.delete()) {
                        System.out.println("删除旧日志文件: " + logFile.getName());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("清理旧日志文件失败: " + e.getMessage());
        }
    }
}