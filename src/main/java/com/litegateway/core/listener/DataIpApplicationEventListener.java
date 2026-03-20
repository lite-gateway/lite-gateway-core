package com.litegateway.core.listener;


import com.litegateway.core.cache.IpListCache;
import com.litegateway.core.client.AdminConfigClient;
import com.litegateway.core.dto.GatewayConfigDTO;
import com.litegateway.core.dto.IpBlackDTO;
import com.litegateway.core.listener.event.DataIpRefreshEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * IP黑名单刷新事件监听器
 * 监听 DataIpRefreshEvent 事件，从 Admin 模块同步最新的 IP 黑名单配置
 */
@Slf4j
@Component
public class DataIpApplicationEventListener {

    @Autowired
    private AdminConfigClient adminConfigClient;


    /**
     * 监听 IP 黑名单刷新事件
     * DataIpRefreshEvent 发布后，触发 listenEvent 方法，从 Admin 拉取最新配置
     */
    @EventListener(classes = DataIpRefreshEvent.class)
    public void listenEvent() {
        log.info("Received DataIpRefreshEvent, syncing IP blacklist from admin...");
        
        try {
            // 从 Admin 模块获取完整配置
            GatewayConfigDTO config = adminConfigClient.getGatewayConfig();
            
            if (config != null && config.getIpBlacklist() != null) {
                List<IpBlackDTO> ipBlacklist = config.getIpBlacklist();
                
                // 清空并重新加载 IP 黑名单缓存
                IpListCache.clear();
                for (IpBlackDTO item : ipBlacklist) {
                    IpListCache.put(item.getIp(), item.getRemark());
                }
                
                log.info("IP blacklist refreshed successfully, loaded {} IPs", ipBlacklist.size());
            } else {
                log.warn("Failed to fetch IP blacklist from admin, config is null or empty");
            }
        } catch (Exception e) {
            log.error("Error refreshing IP blacklist", e);
        }
    }


}
