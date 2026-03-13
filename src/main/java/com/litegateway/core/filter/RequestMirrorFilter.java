package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求镜像过滤器
 * 将请求复制到其他服务进行分析
 */
@Component
public class RequestMirrorFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestMirrorFilter.class);

    private final WebClient webClient;
    
    // 镜像目标配置
    private final ConcurrentHashMap<String, String> mirrorTargets = new ConcurrentHashMap<>();

    public RequestMirrorFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        // 初始化默认的镜像目标
        mirrorTargets.put("default", "http://localhost:8089");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 检查是否需要镜像
        if (request.getHeaders().containsKey("X-Mirror-Target")) {
            String mirrorTarget = request.getHeaders().getFirst("X-Mirror-Target");
            logger.info("Mirroring request to: {}", mirrorTarget);
            
            // 复制请求并发送到镜像目标
            mirrorRequest(exchange, mirrorTarget);
        }
        
        // 继续处理原始请求
        return chain.filter(exchange);
    }

    private void mirrorRequest(ServerWebExchange exchange, String mirrorTarget) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 提取请求信息
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        HttpHeaders headers = request.getHeaders();
        
        // 构建镜像请求 URL
        StringBuilder mirrorUrl = new StringBuilder(mirrorTarget);
        mirrorUrl.append(path);
        if (query != null) {
            mirrorUrl.append("?").append(query);
        }
        
        // 读取请求体
        request.getBody().collectList().subscribe(bodyBuffers -> {
            // 复制请求体
            byte[] bodyBytes = new byte[0];
            for (DataBuffer buffer : bodyBuffers) {
                byte[] bufferBytes = new byte[buffer.readableByteCount()];
                buffer.read(bufferBytes);
                byte[] newBodyBytes = new byte[bodyBytes.length + bufferBytes.length];
                System.arraycopy(bodyBytes, 0, newBodyBytes, 0, bodyBytes.length);
                System.arraycopy(bufferBytes, 0, newBodyBytes, bodyBytes.length, bufferBytes.length);
                bodyBytes = newBodyBytes;
                DataBufferUtils.release(buffer);
            }
            
            // 发送镜像请求
            sendMirrorRequest(mirrorUrl.toString(), method, headers, bodyBytes);
        });
    }

    private void sendMirrorRequest(String url, HttpMethod method, HttpHeaders headers, byte[] body) {
        // 创建一个新的请求头，移除可能导致问题的头
        HttpHeaders mirrorHeaders = new HttpHeaders();
        for (Map.Entry<String, String> entry : headers.toSingleValueMap().entrySet()) {
            String key = entry.getKey();
            // 跳过一些特殊的头
            if (!key.equals("Content-Length") && !key.equals("Host") && !key.equals("Connection")) {
                mirrorHeaders.add(key, entry.getValue());
            }
        }
        
        // 构建并发送镜像请求
        webClient.method(method)
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(mirrorHeaders))
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> logger.info("Mirror request sent successfully, status: {}", response.getStatusCode()),
                        error -> logger.error("Error sending mirror request: {}", error.getMessage())
                );
    }

    @Override
    public int getOrder() {
        // 设置过滤器顺序，确保在其他过滤器之后执行
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    /**
     * 添加镜像目标
     */
    public void addMirrorTarget(String key, String targetUrl) {
        mirrorTargets.put(key, targetUrl);
    }

    /**
     * 移除镜像目标
     */
    public void removeMirrorTarget(String key) {
        mirrorTargets.remove(key);
    }

    /**
     * 获取所有镜像目标
     */
    public Map<String, String> getMirrorTargets() {
        return mirrorTargets;
    }
}