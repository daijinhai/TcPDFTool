package com.tcpdftool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * 跨平台系统工具类
 * 提供文件和文件夹打开功能的跨平台支持
 */
public class SystemUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemUtil.class);
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_MAC = OS_NAME.contains("mac");
    private static final boolean IS_LINUX = OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");
    
    /**
     * 打开文件
     * @param file 要打开的文件
     * @return 是否成功打开
     */
    public static boolean openFile(File file) {
        if (file == null || !file.exists()) {
            logger.warn("文件不存在: {}", file);
            return false;
        }
        
        try {
            // 首先尝试使用Desktop API
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(file);
                    logger.info("使用Desktop API打开文件: {}", file.getAbsolutePath());
                    return true;
                }
            }
            
            // 如果Desktop API不支持，使用系统特定的命令
            return openFileWithSystemCommand(file);
            
        } catch (Exception e) {
            logger.warn("Desktop API打开文件失败，尝试系统命令: {}", e.getMessage());
            return openFileWithSystemCommand(file);
        }
    }
    
    /**
     * 打开文件所在的文件夹
     * @param file 文件
     * @return 是否成功打开
     */
    public static boolean openFileFolder(File file) {
        if (file == null || !file.exists()) {
            logger.warn("文件不存在: {}", file);
            return false;
        }
        
        File parentFile = file.getParentFile();
        if (parentFile == null || !parentFile.exists()) {
            logger.warn("父目录不存在: {}", file);
            return false;
        }
        
        try {
            // 首先尝试使用Desktop API
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(parentFile);
                    logger.info("使用Desktop API打开文件夹: {}", parentFile.getAbsolutePath());
                    return true;
                }
            }
            
            // 如果Desktop API不支持，使用系统特定的命令
            return openFolderWithSystemCommand(file);
            
        } catch (Exception e) {
            logger.warn("Desktop API打开文件夹失败，尝试系统命令: {}", e.getMessage());
            return openFolderWithSystemCommand(file);
        }
    }
    
    /**
     * 使用系统命令打开文件
     */
    private static boolean openFileWithSystemCommand(File file) {
        try {
            ProcessBuilder pb;
            
            if (IS_WINDOWS) {
                // Windows: 使用 cmd /c start
                pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", "\"" + file.getAbsolutePath() + "\"");
            } else if (IS_MAC) {
                // macOS: 使用 open
                pb = new ProcessBuilder("open", file.getAbsolutePath());
            } else if (IS_LINUX) {
                // Linux: 尝试 xdg-open
                pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
            } else {
                logger.error("不支持的操作系统: {}", OS_NAME);
                return false;
            }
            
            Process process = pb.start();
            
            // 等待一小段时间检查是否成功启动
            Thread.sleep(100);
            
            if (process.isAlive() || process.exitValue() == 0) {
                logger.info("使用系统命令打开文件成功: {}", file.getAbsolutePath());
                return true;
            } else {
                logger.warn("系统命令执行失败，退出码: {}", process.exitValue());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("使用系统命令打开文件失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用系统命令打开文件夹并选中文件
     */
    private static boolean openFolderWithSystemCommand(File file) {
        try {
            ProcessBuilder pb;
            
            if (IS_WINDOWS) {
                // Windows: 使用 explorer /select 选中文件
                pb = new ProcessBuilder("explorer", "/select,", file.getAbsolutePath());
            } else if (IS_MAC) {
                // macOS: 使用 open -R 在Finder中显示文件
                pb = new ProcessBuilder("open", "-R", file.getAbsolutePath());
            } else if (IS_LINUX) {
                // Linux: 打开父目录（大多数文件管理器不支持选中特定文件）
                File parentFile = file.getParentFile();
                pb = new ProcessBuilder("xdg-open", parentFile.getAbsolutePath());
            } else {
                logger.error("不支持的操作系统: {}", OS_NAME);
                return false;
            }
            
            Process process = pb.start();
            
            // 等待一小段时间检查是否成功启动
            Thread.sleep(100);
            
            if (process.isAlive() || process.exitValue() == 0) {
                logger.info("使用系统命令打开文件夹成功: {}", file.getAbsolutePath());
                return true;
            } else {
                logger.warn("系统命令执行失败，退出码: {}", process.exitValue());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("使用系统命令打开文件夹失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取操作系统信息
     */
    public static String getOSInfo() {
        return String.format("OS: %s, Windows: %b, Mac: %b, Linux: %b", 
                OS_NAME, IS_WINDOWS, IS_MAC, IS_LINUX);
    }
}