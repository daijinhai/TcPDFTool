package com.tcpdftool.service;

import com.tcpdftool.config.AppConfig;
import com.tcpdftool.model.DetectionResult;
import com.tcpdftool.model.PDFFileInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * PDF检测服务
 * 实现混合检测算法：文件大小 + 图像内容分析
 */
public class PDFDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(PDFDetector.class);
    
    private final AppConfig config;
    private final ExecutorService executorService;
    private Consumer<PDFFileInfo> onDetectionCompleted;
    
    public PDFDetector(AppConfig config) {
        this.config = config;
        this.executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 设置检测完成回调
     */
    public void setOnDetectionCompleted(Consumer<PDFFileInfo> callback) {
        this.onDetectionCompleted = callback;
    }
    
    /**
     * 异步检测PDF文件
     */
    public CompletableFuture<DetectionResult> detectAsync(PDFFileInfo fileInfo) {
        return CompletableFuture.supplyAsync(() -> {
            DetectionResult result = detectPDF(fileInfo);
            fileInfo.setDetectionResult(result);
            
            if (onDetectionCompleted != null) {
                onDetectionCompleted.accept(fileInfo);
            }
            
            return result;
        }, executorService);
    }
    
    /**
     * 同步检测PDF文件
     */
    public DetectionResult detectPDF(PDFFileInfo fileInfo) {
        try {
            logger.debug("开始检测PDF文件: {}", fileInfo.getFileName());
            
            File file = new File(fileInfo.getFilePath());
            if (!file.exists()) {
                logger.warn("文件不存在: {}", fileInfo.getFilePath());
                fileInfo.setErrorMessage("文件不存在");
                return DetectionResult.DETECTION_FAILED;
            }
            
            // 第一步：文件大小检测
            boolean isSuspiciousBySize = isSuspiciousBySize(fileInfo.getFileSize());
            
            // 第二步：图像内容检测
            DetectionResult imageResult = detectByImageContent(file);
            
            // 根据两个检测结果确定最终结果
            if (isSuspiciousBySize && imageResult == DetectionResult.SUSPICIOUS_EMPTY_PIXELS) {
                logger.info("检测到疑似空PDF文件: {} (大小: {}) - 同时满足大小和内容密度条件", 
                    fileInfo.getFileName(), fileInfo.getFormattedFileSize());
                return DetectionResult.SUSPICIOUS_EMPTY_BOTH;
            } else if (isSuspiciousBySize) {
                logger.info("检测到疑似空PDF文件: {} (大小: {}) - 文件大小过小", 
                    fileInfo.getFileName(), fileInfo.getFormattedFileSize());
                return DetectionResult.SUSPICIOUS_EMPTY_SIZE;
            } else if (imageResult == DetectionResult.SUSPICIOUS_EMPTY_PIXELS) {
                logger.info("检测到疑似空PDF文件: {} - 内容密度过低", fileInfo.getFileName());
                return DetectionResult.SUSPICIOUS_EMPTY_PIXELS;
            }
            
            logger.debug("PDF文件检测正常: {}", fileInfo.getFileName());
            return DetectionResult.NORMAL;
            
        } catch (Exception e) {
            logger.error("检测PDF文件失败: {}", fileInfo.getFileName(), e);
            fileInfo.setErrorMessage("检测失败: " + e.getMessage());
            return DetectionResult.DETECTION_FAILED;
        }
    }
    
    /**
     * 根据文件大小判断是否疑似空文件
     */
    private boolean isSuspiciousBySize(long fileSize) {
        long sizeThresholdBytes = config.getFileSizeThreshold() * 1024L; // 转换为字节
        return fileSize <= sizeThresholdBytes;
    }
    
    /**
     * 通过图像内容检测
     */
    private DetectionResult detectByImageContent(File pdfFile) {
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile);
            
            if (document.getNumberOfPages() == 0) {
                logger.debug("PDF文件无页面: {}", pdfFile.getName());
                return DetectionResult.SUSPICIOUS_EMPTY_PIXELS;
            }
            
            // 检测第一页的图像内容
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 72); // 第一页，72 DPI
            
            return analyzeImageContent(image, pdfFile.getName());
            
        } catch (IOException e) {
            logger.error("读取PDF文件失败: {}", pdfFile.getName(), e);
            return DetectionResult.DETECTION_FAILED;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    logger.warn("关闭PDF文档失败: {}", pdfFile.getName(), e);
                }
            }
        }
    }
    
    /**
     * 分析图像内容
     */
    private DetectionResult analyzeImageContent(BufferedImage image, String fileName) {
        if (image == null) {
            logger.warn("无法渲染PDF页面: {}", fileName);
            return DetectionResult.DETECTION_FAILED;
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 计算检测区域
        DetectionArea area = calculateDetectionArea(width, height);
        
        // 统计内容像素（反向判断：统计非白色像素）
        int contentPixelCount = 0;
        int totalPixels = area.getPixelCount();
        
        for (int y = area.startY; y < area.endY; y++) {
            for (int x = area.startX; x < area.endX; x++) {
                if (isContentPixel(image.getRGB(x, y))) {
                    contentPixelCount++;
                }
            }
        }
        
        // 计算内容像素密度（内容像素占总像素的比例）
        double contentPixelDensity = (double) contentPixelCount / totalPixels;
        double threshold = config.getContentPixelDensityThreshold() / 100.0;
        
        logger.info("图像内容分析 - 文件: {}, 内容像素密度: {}%, 阈值: {}%", 
            fileName, String.format("%.2f", contentPixelDensity * 100), String.format("%.2f", threshold * 100));
        
        // 反向判断：如果内容像素密度低于阈值，则认为是空白或内容稀少的文档
        if (contentPixelDensity < threshold) {
            logger.info("判断依据: 内容像素密度 {}% < 阈值 {}%，判定为疑似空文件", 
                String.format("%.2f", contentPixelDensity * 100), String.format("%.2f", threshold * 100));
            return DetectionResult.SUSPICIOUS_EMPTY_PIXELS;
        } else {
            logger.info("判断依据: 内容像素密度 {}% >= 阈值 {}%，判定为正常文件", 
                String.format("%.2f", contentPixelDensity * 100), String.format("%.2f", threshold * 100));
            return DetectionResult.NORMAL;
        }
    }
    
    /**
     * 计算检测区域
     */
    private DetectionArea calculateDetectionArea(int width, int height) {
        // 使用百分比配置计算ROI尺寸
        double areaWidthPercent = config.getDetectionAreaWidthPercent();
        double areaHeightPercent = config.getDetectionAreaHeightPercent();
        
        int areaWidth = (int) Math.round(width * (areaWidthPercent / 100.0));
        int areaHeight = (int) Math.round(height * (areaHeightPercent / 100.0));
        
        // 约束到图像尺寸范围，至少1像素
        areaWidth = Math.max(1, Math.min(areaWidth, width));
        areaHeight = Math.max(1, Math.min(areaHeight, height));
        
        // 计算中心点：水平偏移相对图像中心，-100左，+100右，0居中
        int offsetPercent = config.getHorizontalOffsetPercent();
        int centerX = (int) Math.round(width / 2.0 + (offsetPercent / 100.0) * (width / 2.0));
        int centerY = height / 2;
        
        // 以中心点为基准计算区域，超出边界时仅显示被遮挡部分（通过边界裁剪实现）
        int startX = centerX - areaWidth / 2;
        int startY = centerY - areaHeight / 2;
        int endX = startX + areaWidth;
        int endY = startY + areaHeight;
        
        // 边界裁剪，保证采样坐标有效
        int clippedStartX = Math.max(0, startX);
        int clippedStartY = Math.max(0, startY);
        int clippedEndX = Math.min(width, endX);
        int clippedEndY = Math.min(height, endY);
        
        logger.debug("检测区域计算 - 图像尺寸: {}x{}, 百分比: {}%x{}%, 偏移%: {}, 实际像素: {}x{}, 原始位置: ({},{})->({},{}), 裁剪后: ({},{})->({},{})",
            width, height,
            String.format("%.2f", areaWidthPercent), String.format("%.2f", areaHeightPercent), offsetPercent,
            areaWidth, areaHeight,
            startX, startY, endX, endY,
            clippedStartX, clippedStartY, clippedEndX, clippedEndY);
        
        return new DetectionArea(clippedStartX, clippedStartY, clippedEndX, clippedEndY);
    }
    
    /**
     * 判断是否为内容像素（非白色/背景像素）
     * 反向判断：检测有实际内容的像素
     */
    private boolean isContentPixel(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        
        // 内容像素：RGB值不全是接近白色的像素
        // 使用更宽松的阈值来识别各种内容（文字、线条、图像等）
        return !(red > 240 && green > 240 && blue > 240);
    }
    
    /**
     * 关闭检测服务
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * 检测区域内部类
     */
    private static class DetectionArea {
        final int startX, startY, endX, endY;
        
        DetectionArea(int startX, int startY, int endX, int endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
        
        int getPixelCount() {
            return (endX - startX) * (endY - startY);
        }
    }
}