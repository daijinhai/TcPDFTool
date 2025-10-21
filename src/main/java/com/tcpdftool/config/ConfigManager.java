package com.tcpdftool.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置管理器
 * 负责配置文件的加载、保存和管理
 */
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.tcpdftool";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
    
    private static ConfigManager instance;
    private AppConfig config;
    private ObjectMapper objectMapper;
    
    private ConfigManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.config = new AppConfig();
    }
    
    /**
     * 获取配置管理器单例实例
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        try {
            // 确保配置目录存在
            createConfigDirectoryIfNotExists();
            
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                config = objectMapper.readValue(configFile, AppConfig.class);
                logger.info("配置文件加载成功: {}", CONFIG_FILE);
            } else {
                // 配置文件不存在，使用默认配置并保存
                saveConfig();
                logger.info("使用默认配置并创建配置文件: {}", CONFIG_FILE);
            }
        } catch (IOException e) {
            logger.error("加载配置文件失败，使用默认配置", e);
            config = new AppConfig();
        }
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            // 确保配置目录存在
            createConfigDirectoryIfNotExists();
            
            File configFile = new File(CONFIG_FILE);
            objectMapper.writeValue(configFile, config);
            logger.info("配置文件保存成功: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("保存配置文件失败", e);
            throw new RuntimeException("保存配置文件失败", e);
        }
    }
    
    /**
     * 获取当前配置
     */
    public AppConfig getConfig() {
        return config;
    }
    
    /**
     * 更新配置
     */
    public void updateConfig(AppConfig newConfig) {
        this.config = newConfig;
        saveConfig();
    }
    
    /**
     * 重置为默认配置
     */
    public void resetToDefault() {
        this.config = new AppConfig();
        saveConfig();
        logger.info("配置已重置为默认值");
    }
    
    /**
     * 创建配置目录（如果不存在）
     */
    private void createConfigDirectoryIfNotExists() throws IOException {
        Path configDir = Paths.get(CONFIG_DIR);
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
            logger.info("创建配置目录: {}", CONFIG_DIR);
        }
    }
    
    /**
     * 获取配置文件路径
     */
    public String getConfigFilePath() {
        return CONFIG_FILE;
    }
    
    /**
     * 检查配置文件是否存在
     */
    public boolean configFileExists() {
        return new File(CONFIG_FILE).exists();
    }
    
    /**
     * 导出配置到指定文件
     */
    public void exportConfig(String filePath) throws IOException {
        File exportFile = new File(filePath);
        objectMapper.writeValue(exportFile, config);
        logger.info("配置已导出到: {}", filePath);
    }
    
    /**
     * 从指定文件导入配置
     */
    public void importConfig(String filePath) throws IOException {
        File importFile = new File(filePath);
        if (!importFile.exists()) {
            throw new IOException("配置文件不存在: " + filePath);
        }
        
        AppConfig importedConfig = objectMapper.readValue(importFile, AppConfig.class);
        updateConfig(importedConfig);
        logger.info("配置已从文件导入: {}", filePath);
    }
}