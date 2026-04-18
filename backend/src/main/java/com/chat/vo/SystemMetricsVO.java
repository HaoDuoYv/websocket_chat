package com.chat.vo;

import lombok.Data;

/**
 * 系统监控数据VO
 */
@Data
public class SystemMetricsVO {
    
    /**
     * CPU使用率 (%)
     */
    private double cpuUsage;
    
    /**
     * CPU核心数
     */
    private int cpuCores;
    
    /**
     * 内存总量 (MB)
     */
    private long totalMemory;
    
    /**
     * 已用内存 (MB)
     */
    private long usedMemory;
    
    /**
     * 剩余内存 (MB)
     */
    private long freeMemory;
    
    /**
     * 内存使用率 (%)
     */
    private double memoryUsage;
    
    /**
     * JVM内存总量 (MB)
     */
    private long jvmTotalMemory;
    
    /**
     * JVM已用内存 (MB)
     */
    private long jvmUsedMemory;
    
    /**
     * JVM剩余内存 (MB)
     */
    private long jvmFreeMemory;
    
    /**
     * JVM内存使用率 (%)
     */
    private double jvmMemoryUsage;
    
    /**
     * 系统运行时间 (秒)
     */
    private long uptime;
    
    /**
     * 时间戳
     */
    private long timestamp;
}
