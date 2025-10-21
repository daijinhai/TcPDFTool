package com.tcpdftool.ui;

import com.tcpdftool.model.PDFFileInfo;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF文件表格模型
 */
public class PDFFileTableModel extends AbstractTableModel {
    
    private static final String[] COLUMN_NAMES = {
        "状态", "文件名", "大小", "创建时间", "检测结果", "通知状态"
    };
    
    private static final Class<?>[] COLUMN_CLASSES = {
        String.class, String.class, String.class, String.class, String.class, String.class
    };
    
    private List<PDFFileInfo> fileList = new ArrayList<>();
    
    @Override
    public int getRowCount() {
        return fileList.size();
    }
    
    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }
    
    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_CLASSES[columnIndex];
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= fileList.size()) {
            return null;
        }
        
        PDFFileInfo fileInfo = fileList.get(rowIndex);
        
        switch (columnIndex) {
            case 0: // 状态图标
                return fileInfo.getStatusIcon();
            case 1: // 文件名
                return fileInfo.getFileName();
            case 2: // 文件大小
                return fileInfo.getFormattedFileSize();
            case 3: // 创建时间
                return fileInfo.getShortCreateTime();
            case 4: // 检测结果
                return fileInfo.getDetectionResult().getDisplayName();
            case 5: // 通知状态
                return fileInfo.getNotificationStatusText();
            default:
                return null;
        }
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // 所有单元格都不可编辑
    }
    
    /**
     * 添加文件
     */
    public void addFile(PDFFileInfo fileInfo) {
        if (fileInfo != null && !fileList.contains(fileInfo)) {
            fileList.add(fileInfo);
            int row = fileList.size() - 1;
            fireTableRowsInserted(row, row);
        }
    }
    
    /**
     * 更新文件
     */
    public void updateFile(PDFFileInfo fileInfo) {
        if (fileInfo != null) {
            int index = fileList.indexOf(fileInfo);
            if (index >= 0) {
                fileList.set(index, fileInfo);
                fireTableRowsUpdated(index, index);
            }
        }
    }
    
    /**
     * 移除文件
     */
    public void removeFile(PDFFileInfo fileInfo) {
        if (fileInfo != null) {
            int index = fileList.indexOf(fileInfo);
            if (index >= 0) {
                fileList.remove(index);
                fireTableRowsDeleted(index, index);
            }
        }
    }
    
    /**
     * 清空所有文件
     */
    public void clearAll() {
        int size = fileList.size();
        if (size > 0) {
            fileList.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }
    
    /**
     * 获取指定行的文件信息
     */
    public PDFFileInfo getFileAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < fileList.size()) {
            return fileList.get(rowIndex);
        }
        return null;
    }
    
    /**
     * 获取所有文件列表
     */
    public List<PDFFileInfo> getAllFiles() {
        return new ArrayList<>(fileList);
    }
    
    /**
     * 获取疑似空文件列表
     */
    public List<PDFFileInfo> getSuspiciousFiles() {
        return fileList.stream()
                .filter(PDFFileInfo::isSuspiciousEmpty)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 获取文件总数
     */
    public int getFileCount() {
        return fileList.size();
    }
    
    /**
     * 获取疑似空文件数量
     */
    public int getSuspiciousFileCount() {
        return (int) fileList.stream().filter(PDFFileInfo::isSuspiciousEmpty).count();
    }
    
    /**
     * 刷新表格数据
     */
    public void refresh() {
        fireTableDataChanged();
    }
}