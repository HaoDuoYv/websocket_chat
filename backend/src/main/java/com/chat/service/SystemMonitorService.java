package com.chat.service;

import com.chat.vo.SystemMetricsVO;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

/**
 * 系统监控服务
 */
@Service
public class SystemMonitorService {

    /**
     * 获取系统监控数据
     */
    public SystemMetricsVO getSystemMetrics() {
        SystemMetricsVO metrics = new SystemMetricsVO();
        
        // 获取操作系统MXBean
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        // CPU信息
        metrics.setCpuCores(osBean.getAvailableProcessors());
        
        // 获取系统负载（近似CPU使用率）
        double systemLoad = osBean.getSystemLoadAverage();
        if (systemLoad >= 0) {
            // 将系统负载转换为百分比（基于CPU核心数）
            double cpuUsage = (systemLoad / metrics.getCpuCores()) * 100;
            metrics.setCpuUsage(Math.min(cpuUsage, 100));
        } else {
            // 如果无法获取系统负载，使用Process CPU Load（Java 8+）
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                metrics.setCpuUsage(sunOsBean.getProcessCpuLoad() * 100);
            } else {
                metrics.setCpuUsage(0);
            }
        }
        
        // 内存信息（转换为MB）
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        
        metrics.setTotalMemory(maxMemory);
        metrics.setUsedMemory(usedMemory);
        metrics.setFreeMemory(maxMemory - usedMemory);
        metrics.setMemoryUsage(((double) usedMemory / maxMemory) * 100);
        
        // JVM内存信息
        metrics.setJvmTotalMemory(totalMemory);
        metrics.setJvmFreeMemory(freeMemory);
        metrics.setJvmUsedMemory(usedMemory);
        metrics.setJvmMemoryUsage(((double) usedMemory / totalMemory) * 100);
        
        // 系统运行时间
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        metrics.setUptime(runtimeMXBean.getUptime() / 1000);
        
        // 时间戳
        metrics.setTimestamp(System.currentTimeMillis());
        
        return metrics;
    }
}
