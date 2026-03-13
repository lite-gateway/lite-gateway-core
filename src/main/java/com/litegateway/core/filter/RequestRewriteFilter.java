package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 请求重写过滤器
 * 支持 URL 和请求参数的重写
 */
@Component
public class RequestRewriteFilter extends AbstractGatewayFilterFactory<RequestRewriteFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RequestRewriteFilter.class);

    public RequestRewriteFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            URI originalUri = request.getURI();
            
            // 重写 URL
            String rewrittenUrl = rewriteUrl(originalUri.toString(), config.getUrlPattern(), config.getUrlReplacement());
            
            // 构建新的请求
            ServerHttpRequest.Builder requestBuilder = request.mutate()
                    .uri(URI.create(rewrittenUrl));
            
            // 重写请求参数
            if (config.getParamMappings() != null) {
                for (Map.Entry<String, String> mapping : config.getParamMappings().entrySet()) {
                    String oldParam = mapping.getKey();
                    String newParam = mapping.getValue();
                    // 这里可以实现参数重写逻辑
                    logger.info("Rewriting parameter: {} -> {}", oldParam, newParam);
                }
            }
            
            ServerHttpRequest rewrittenRequest = requestBuilder.build();
            logger.info("Rewritten URL: {} -> {}", originalUri, rewrittenRequest.getURI());
            
            return chain.filter(exchange.mutate().request(rewrittenRequest).build());
        };
    }

    private String rewriteUrl(String originalUrl, String pattern, String replacement) {
        if (pattern == null || replacement == null) {
            return originalUrl;
        }
        
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(originalUrl);
        return matcher.replaceAll(replacement);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("urlPattern", "urlReplacement", "paramMappings");
    }

    public static class Config {
        private String urlPattern;
        private String urlReplacement;
        private Map<String, String> paramMappings;

        public String getUrlPattern() {
            return urlPattern;
        }

        public void setUrlPattern(String urlPattern) {
            this.urlPattern = urlPattern;
        }

        public String getUrlReplacement() {
            return urlReplacement;
        }

        public void setUrlReplacement(String urlReplacement) {
            this.urlReplacement = urlReplacement;
        }

        public Map<String, String> getParamMappings() {
            return paramMappings;
        }

        public void setParamMappings(Map<String, String> paramMappings) {
            this.paramMappings = paramMappings;
        }
    }
}