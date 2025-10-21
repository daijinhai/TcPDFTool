package com.tcpdftool;

import com.tcpdftool.config.ConfigManager;
import com.tcpdftool.ui.MainFrame;
import com.tcpdftool.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * 主应用程序入口
 */
public class TcPDFToolApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(TcPDFToolApplication.class);
    
    public static void main(String[] args) {
        try {
            // 初始化日志
            LoggerUtil.initializeLogging();
            logger.info("TcPDFTool 应用程序启动");
            
            // 设置系统Look and Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // 初始化配置管理器
            ConfigManager configManager = ConfigManager.getInstance();
            configManager.loadConfig();
            
            // 启动主界面
            SwingUtilities.invokeLater(() -> {
                try {
                    MainFrame mainFrame = new MainFrame(configManager);
                    mainFrame.setVisible(true);
                    logger.info("主界面启动完成");
                } catch (Exception e) {
                    logger.error("主界面启动失败", e);
                    JOptionPane.showMessageDialog(null, 
                        "应用程序启动失败: " + e.getMessage(), 
                        "启动错误", 
                        JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            });
            
        } catch (Exception e) {
            logger.error("应用程序启动失败", e);
            JOptionPane.showMessageDialog(null, 
                "应用程序启动失败: " + e.getMessage(), 
                "启动错误", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}