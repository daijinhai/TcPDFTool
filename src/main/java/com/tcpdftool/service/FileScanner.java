package com.tcpdftool.service;

import com.tcpdftool.config.AppConfig;
import com.tcpdftool.model.PDFFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 文件扫描服务
 * 负责监控指定目录下的PDF文件变化
 */
public class FileScanner {
    
    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);
    
    private final AppConfig config;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, PDFFileInfo> knownFiles;
    private Consumer<PDFFileInfo> onNewFileFound;
    private Consumer<List<PDFFileInfo>> onScanCompleted;
    private boolean isScanning;
    private WatchService watchService;
    private Thread watchThread;
    
    public FileScanner(AppConfig config) {
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.knownFiles = new ConcurrentHashMap<>();
        this.isScanning = false;
    }
    
    /**
     * 设置新文件发现回调
     */
    public void setOnNewFileFound(Consumer<PDFFileInfo> callback) {
        this.onNewFileFound = callback;
    }
    
    /**
     * 设置扫描完成回调
     */
    public void setOnScanCompleted(Consumer<List<PDFFileInfo>> callback) {
        this.onScanCompleted = callback;
    }
    
    /**
     * 开始扫描
     */
    public void startScanning() {
        if (isScanning) {
            logger.warn("文件扫描已在运行中");
            return;
        }
        
        String monitorDir = config.getMonitorDirectory();
        if (monitorDir == null || monitorDir.trim().isEmpty()) {
            logger.error("监控目录未配置");
            return;
        }
        
        File dir = new File(monitorDir);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.error("监控目录不存在或不是目录: {}", monitorDir);
            return;
        }
        
        isScanning = true;
        logger.info("开始扫描目录: {}", monitorDir);
        
        // 初始扫描
        performInitialScan();
        
        // 启动定时扫描
        startScheduledScanning();
        
        // 启动文件监控
        startFileWatching();
    }
    
    /**
     * 停止扫描
     */
    public void stopScanning() {
        if (!isScanning) {
            return;
        }
        
        isScanning = false;
        logger.info("停止文件扫描");
        
        // 停止定时任务
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        // 停止文件监控
        stopFileWatching();
    }
    
    /**
     * 执行初始扫描
     */
    private void performInitialScan() {
        try {
            logger.info("开始执行初始扫描，监控目录: {}", config.getMonitorDirectory());
            List<PDFFileInfo> foundFiles = scanDirectory();
            logger.info("初始扫描完成，发现 {} 个PDF文件", foundFiles.size());
            
            if (onScanCompleted != null) {
                logger.info("调用扫描完成回调");
                onScanCompleted.accept(foundFiles);
            } else {
                logger.warn("扫描完成回调为null");
            }
        } catch (Exception e) {
            logger.error("初始扫描失败", e);
        }
    }
    
    /**
     * 启动定时扫描
     */
    private void startScheduledScanning() {
        int scanInterval = config.getScanInterval();
        logger.info("启动定时扫描 - 扫描间隔: {} 秒", scanInterval);
        
        scheduler.scheduleWithFixedDelay(() -> {
            if (!isScanning) {
                return;
            }
            
            try {
                logger.info("执行定时扫描 - 监控目录: {}", config.getMonitorDirectory());
                List<PDFFileInfo> newFiles = scanForNewFiles();
                
                if (!newFiles.isEmpty()) {
                    logger.info("定时扫描发现 {} 个新文件", newFiles.size());
                    
                    for (PDFFileInfo fileInfo : newFiles) {
                        if (onNewFileFound != null) {
                            onNewFileFound.accept(fileInfo);
                        }
                    }
                } else {
                    logger.info("定时扫描完成 - 未发现新文件，已知文件数: {}", knownFiles.size());
                }
            } catch (Exception e) {
                logger.error("定时扫描失败", e);
            }
        }, scanInterval, scanInterval, TimeUnit.SECONDS);
    }
    
    /**
     * 启动文件监控
     */
    private void startFileWatching() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path monitorPath = Paths.get(config.getMonitorDirectory());
            
            // 注册监控事件
            monitorPath.register(watchService, 
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);
            
            watchThread = new Thread(this::watchForFileChanges, "FileWatcher");
            watchThread.setDaemon(true);
            watchThread.start();
            
            logger.info("文件监控已启动");
        } catch (IOException e) {
            logger.error("启动文件监控失败", e);
        }
    }
    
    /**
     * 停止文件监控
     */
    private void stopFileWatching() {
        try {
            if (watchService != null) {
                watchService.close();
            }
            if (watchThread != null) {
                watchThread.interrupt();
            }
        } catch (IOException e) {
            logger.error("停止文件监控失败", e);
        }
    }
    
    /**
     * 监控文件变化
     */
    private void watchForFileChanges() {
        while (isScanning && !Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    
                    if (isPDFFile(fileName.toString())) {
                        Path fullPath = Paths.get(config.getMonitorDirectory()).resolve(fileName);
                        handleFileEvent(fullPath.toFile(), kind);
                    }
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                // WatchService已关闭，正常退出监控循环
                logger.info("文件监控服务已关闭，停止监控");
                break;
            } catch (Exception e) {
                logger.error("文件监控异常", e);
                // 发生其他异常时，短暂休眠后继续监控
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * 处理文件事件
     */
    private void handleFileEvent(File file, WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            // 新文件创建，延迟处理以确保文件写入完成
            scheduler.schedule(() -> {
                if (file.exists() && isWithinTimeRange(file)) {
                    PDFFileInfo fileInfo = createPDFFileInfo(file);
                    knownFiles.put(file.getAbsolutePath(), fileInfo);
                    
                    if (onNewFileFound != null) {
                        onNewFileFound.accept(fileInfo);
                    }
                    
                    logger.info("检测到新PDF文件: {}", file.getName());
                }
            }, 2, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 扫描目录
     */
    private List<PDFFileInfo> scanDirectory() {
        List<PDFFileInfo> files = new ArrayList<>();
        String monitorDir = config.getMonitorDirectory();
        
        logger.info("扫描目录: {}", monitorDir);
        
        try {
            Path startPath = Paths.get(monitorDir);
            
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    File f = file.toFile();
                    
                    logger.debug("检查文件: {}", f.getName());
                    
                    if (isPDFFile(f.getName()) && isWithinTimeRange(f)) {
                        logger.info("发现PDF文件: {}", f.getAbsolutePath());
                        PDFFileInfo fileInfo = createPDFFileInfo(f);
                        files.add(fileInfo);
                        knownFiles.put(f.getAbsolutePath(), fileInfo);
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // 如果不扫描子目录，只处理根目录
                    if (!config.isIncludeSubdirectories() && !dir.equals(startPath)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("扫描目录失败: {}", monitorDir, e);
        }
        
        logger.info("扫描完成，共找到 {} 个PDF文件", files.size());
        return files;
    }
    
    /**
     * 扫描新文件
     */
    private List<PDFFileInfo> scanForNewFiles() {
        List<PDFFileInfo> newFiles = new ArrayList<>();
        String monitorDir = config.getMonitorDirectory();
        
        try {
            Path startPath = Paths.get(monitorDir);
            
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    File f = file.toFile();
                    String filePath = f.getAbsolutePath();
                    
                    if (isPDFFile(f.getName()) && 
                        isWithinTimeRange(f) && 
                        !knownFiles.containsKey(filePath)) {
                        
                        PDFFileInfo fileInfo = createPDFFileInfo(f);
                        newFiles.add(fileInfo);
                        knownFiles.put(filePath, fileInfo);
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!config.isIncludeSubdirectories() && !dir.equals(startPath)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("扫描新文件失败: {}", monitorDir, e);
        }
        
        return newFiles;
    }
    
    /**
     * 创建PDF文件信息对象
     */
    private PDFFileInfo createPDFFileInfo(File file) {
        Date createTime = new Date(file.lastModified());
        
        return new PDFFileInfo(
            file.getName(),
            file.getAbsolutePath(),
            file.length(),
            createTime
        );
    }
    
    /**
     * 检查是否为PDF文件
     */
    private boolean isPDFFile(String fileName) {
        return fileName.toLowerCase().endsWith(".pdf");
    }
    
    /**
     * 检查文件是否在时间范围内
     */
    private boolean isWithinTimeRange(File file) {
        long timeRangeMinutes = config.getFileTimeRange() * 60; // 小时转换为分钟
        if (timeRangeMinutes <= 0) {
            return true; // 不限制时间范围
        }
        
        long currentTime = System.currentTimeMillis();
        long fileTime = file.lastModified();
        long timeDiff = currentTime - fileTime;
        long timeRangeMillis = timeRangeMinutes * 60 * 1000L;
        
        return timeDiff <= timeRangeMillis;
    }
    
    /**
     * 获取已知文件列表
     */
    public List<PDFFileInfo> getKnownFiles() {
        return new ArrayList<>(knownFiles.values());
    }
    
    /**
     * 清除已知文件缓存
     */
    public void clearKnownFiles() {
        knownFiles.clear();
    }
    
    /**
     * 检查是否正在扫描
     */
    public boolean isScanning() {
        return isScanning;
    }
}