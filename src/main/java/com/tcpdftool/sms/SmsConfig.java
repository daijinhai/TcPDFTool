package com.tcpdftool.sms;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 短信配置类
 */
public class SmsConfig {
    
    @JsonProperty("provider")
    private String provider = "aliyun"; // aliyun, tencent, huawei
    
    @JsonProperty("accessKeyId")
    private String accessKeyId;
    
    @JsonProperty("accessKeySecret")
    private String accessKeySecret;
    
    @JsonProperty("signName")
    private String signName;
    
    @JsonProperty("templateCode")
    private String templateCode;
    
    @JsonProperty("phoneNumbers")
    private String phoneNumbers; // 多个号码用逗号分隔
    
    @JsonProperty("timeout")
    private int timeout = 30000; // 超时时间（毫秒）
    
    @JsonProperty("retryCount")
    private int retryCount = 3; // 重试次数
    
    // Getters and Setters
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getAccessKeyId() {
        return accessKeyId;
    }
    
    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }
    
    public String getAccessKeySecret() {
        return accessKeySecret;
    }
    
    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }
    
    public String getSignName() {
        return signName;
    }
    
    public void setSignName(String signName) {
        this.signName = signName;
    }
    
    public String getTemplateCode() {
        return templateCode;
    }
    
    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }
    
    public String getPhoneNumbers() {
        return phoneNumbers;
    }
    
    public void setPhoneNumbers(String phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}