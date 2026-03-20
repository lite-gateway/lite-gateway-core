package com.litegateway.core.listener;

import com.litegateway.core.cache.WhiteListCache;
import com.litegateway.core.client.AdminConfigClient;
import com.litegateway.core.dto.GatewayConfigDTO;
import com.litegateway.core.dto.WhiteListDTO;
import com.litegateway.core.listener.event.WhiteListRefreshEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 白名单刷新事件监听器
 * 监听 WhiteListRefreshEvent 事件，从 Admin 模块同步最新的白名单配置
 */
@Slf4j
@Component
public class WhiteListApplicationEventListener {

    @Autowired
    private AdminConfigClient adminConfigClient;


    /**
     * 监听白名单刷新事件
     * WhiteListRefreshEvent 发布后，触发 listenEvent 方法，从 Admin 拉取最新配置
     */
    @EventListener(classes = WhiteListRefreshEvent.class)
    public void listenEvent() {
        log.info("Received WhiteListRefreshEvent, syncing whitelist from admin...");
        
        try {
            // 从 Admin 模块获取完整配置
            GatewayConfigDTO config = adminConfigClient.getGatewayConfig();
            
            if (config != null && config.getWhiteList() != null) {
                List<WhiteListDTO> whiteList = config.getWhiteList();
                
                // 清空并重新加载白名单缓存
                WhiteListCache.clear();
                for (WhiteListDTO item : whiteList) {
                    WhiteListCache.put(item.getPath(), item.getDescription());
                }
                
                log.info("Whitelist refreshed successfully, loaded {} items", whiteList.size());
            } else {
                log.warn("Failed to fetch whitelist from admin, config is null or empty");
            }
        } catch (Exception e) {
            log.error("Error refreshing whitelist", e);
        }
    }


}
