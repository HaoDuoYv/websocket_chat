package com.chat.vo;

import lombok.Data;

/**
 * 日志行VO
 */
@Data
public class LogLineVO {
    
    /**
     * 日志行号
     */
    private long lineNumber;
    
    /**
     * 日志内容
     */
    private String content;
    
    /**
     * 日志级别 (INFO, ERROR, WARN, DEBUG)
     */
    private String level;
    
    /**
     * 时间戳
     */
    private String timestamp;
}
