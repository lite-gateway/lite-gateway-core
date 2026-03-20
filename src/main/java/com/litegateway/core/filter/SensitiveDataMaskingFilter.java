package com.litegateway.core.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litegateway.core.dto.FeatureConfigDTO;
import com.litegateway.core.manager.GatewayFeatureManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.reactivestreams.Publisher;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏过滤器
 * 对请求和响应中的敏感数据进行脱敏处理
 * 支持动态配置
 */
@Component
public class SensitiveDataMaskingFilter implements GlobalFilter, Ordered, ConfigurableFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveDataMaskingFilter.class);

    private static final String FEATURE_CODE = "sensitive_data_masking";

    @Autowired
    private GatewayFeatureManager gatewayFeatureManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 默认脱敏策略
    private final Map<String, Pattern> defaultMaskingPatterns = new HashMap<>();

    public SensitiveDataMaskingFilter() {
        // 初始化默认脱敏正则表达式
        defaultMaskingPatterns.put("phone", Pattern.compile("1[3-9]\\d{9}"));
        defaultMaskingPatterns.put("idCard", Pattern.compile("[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]"));
        defaultMaskingPatterns.put("bankCard", Pattern.compile("[1-9]\\d{15,18}"));
        defaultMaskingPatterns.put("email", Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"));
    }

    @Override
    public String getFeatureCode() {
        return FEATURE_CODE;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取当前路由
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "default";

        // 检查功能是否启用
        if (!gatewayFeatureManager.isFeatureEnabled(FEATURE_CODE, routeId)) {
            logger.debug("Sensitive data masking is disabled for route: {}", routeId);
            return chain.filter(exchange);
        }

        // 获取动态配置
        MaskingConfig maskingConfig = loadMaskingConfig();

        // 处理请求
        ServerHttpRequest decoratedRequest = decorateRequest(exchange, exchange.getRequest(), maskingConfig);

        // 处理响应
        ServerHttpResponse decoratedResponse = decorateResponse(exchange.getResponse(), maskingConfig);

        return chain.filter(exchange.mutate()
                .request(decoratedRequest)
                .response(decoratedResponse)
                .build());
    }

    private MaskingConfig loadMaskingConfig() {
        FeatureConfigDTO config = gatewayFeatureManager.getFeatureConfig(FEATURE_CODE);
        if (config == null || StringUtils.isBlank(config.getConfigJson())) {
            return MaskingConfig.defaultConfig();
        }
        try {
            return parseConfig(config.getConfigJson());
        } catch (Exception e) {
            logger.warn("Failed to parse masking config, using default", e);
            return MaskingConfig.defaultConfig();
        }
    }

    private MaskingConfig parseConfig(String configJson) throws Exception {
        MaskingConfig config = new MaskingConfig();
        JsonNode root = objectMapper.readTree(configJson);

        // 解析字段列表
        if (root.has("fields") && root.get("fields").isArray()) {
            List<String> fields = new ArrayList<>();
            root.get("fields").forEach(node -> fields.add(node.asText()));
            config.setFields(fields);
        }

        // 解析规则
        if (root.has("rules") && root.get("rules").isObject()) {
            Map<String, String> rules = new HashMap<>();
            JsonNode rulesNode = root.get("rules");
            rulesNode.fields().forEachRemaining(entry -> {
                rules.put(entry.getKey(), entry.getValue().asText());
            });
            config.setRules(rules);
        }

        // 解析是否脱敏请求
        if (root.has("maskRequest")) {
            config.setMaskRequest(root.get("maskRequest").asBoolean(true));
        }

        // 解析是否脱敏响应
        if (root.has("maskResponse")) {
            config.setMaskResponse(root.get("maskResponse").asBoolean(true));
        }

        return config;
    }

    private ServerHttpRequest decorateRequest(ServerWebExchange exchange, ServerHttpRequest request, MaskingConfig config) {
        if (!config.isMaskRequest()) {
            return request;
        }
        if (request.getMethod() != HttpMethod.POST && request.getMethod() != HttpMethod.PUT) {
            return request;
        }

        return new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody()
                        .flatMap(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);

                            String body = new String(bytes, StandardCharsets.UTF_8);
                            String maskedBody = maskSensitiveData(body, config);

                            logger.debug("Original request body: {}", body);
                            logger.debug("Masked request body: {}", maskedBody);

                            return Flux.just(exchange.getResponse().bufferFactory().wrap(maskedBody.getBytes(StandardCharsets.UTF_8)));
                        });
            }
        };
    }

    private ServerHttpResponse decorateResponse(ServerHttpResponse response, MaskingConfig config) {
        if (!config.isMaskResponse()) {
            return response;
        }

        return new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                MediaType contentType = getDelegate().getHeaders().getContentType();

                if (contentType != null && (contentType.includes(MediaType.APPLICATION_JSON) ||
                        contentType.includes(MediaType.TEXT_PLAIN))) {
                    return super.writeWith(Flux.from(body)
                            .flatMap(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);

                                String bodyStr = new String(bytes, StandardCharsets.UTF_8);
                                String maskedBody = maskSensitiveData(bodyStr, config);

                                logger.debug("Original response body: {}", bodyStr);
                                logger.debug("Masked response body: {}", maskedBody);

                                return Flux.just(getDelegate().bufferFactory().wrap(maskedBody.getBytes(StandardCharsets.UTF_8)));
                            }));
                }
                return super.writeWith(body);
            }
        };
    }

    private String maskSensitiveData(String data, MaskingConfig config) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        String maskedData = data;

        // 根据配置的规则进行脱敏
        Map<String, String> rules = config.getRules();
        if (rules != null) {
            // 脱敏手机号
            if (rules.containsKey("phone")) {
                maskedData = maskPattern(maskedData, "phone", rules.get("phone"));
            }
            // 脱敏身份证号
            if (rules.containsKey("idCard")) {
                maskedData = maskPattern(maskedData, "idCard", rules.get("idCard"));
            }
            // 脱敏银行卡号
            if (rules.containsKey("bankCard")) {
                maskedData = maskPattern(maskedData, "bankCard", rules.get("bankCard"));
            }
            // 脱敏邮箱
            if (rules.containsKey("email")) {
                maskedData = maskEmail(maskedData, rules.get("email"));
            }
        }

        // 脱敏密码
        maskedData = maskPassword(maskedData);

        return maskedData;
    }

    private String maskPattern(String data, String patternKey, String replacement) {
        Pattern pattern = defaultMaskingPatterns.get(patternKey);
        if (pattern != null && StringUtils.isNotBlank(replacement)) {
            Matcher matcher = pattern.matcher(data);
            return matcher.replaceAll(replacement);
        }
        return data;
    }

    private String maskEmail(String data, String replacement) {
        Pattern pattern = defaultMaskingPatterns.get("email");
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            return matcher.replaceAll(match -> {
                if (StringUtils.isNotBlank(replacement)) {
                    return replacement;
                }
                String email = match.group();
                int atIndex = email.indexOf('@');
                if (atIndex > 2) {
                    return email.substring(0, 2) + "****" + email.substring(atIndex);
                }
                return email;
            });
        }
        return data;
    }

    private String maskPassword(String data) {
        String result = data;
        String[] passwordKeywords = {"password", "passwd", "pwd"};

        for (String keyword : passwordKeywords) {
            int index = result.toLowerCase().indexOf(keyword + ":\"");
            while (index != -1) {
                int startIndex = result.indexOf("\"", index - 1);
                int endIndex = result.indexOf("\"", index + keyword.length() + 2);
                if (startIndex != -1 && endIndex != -1) {
                    String maskedPart = result.substring(0, startIndex + 1) + keyword + ":\"****\"" + result.substring(endIndex + 1);
                    result = maskedPart;
                }
                index = result.toLowerCase().indexOf(keyword + ":\"", index + 1);
            }
        }
        return result;
    }

    @Override
    public int getOrder() {
        // 设置过滤器顺序，确保在其他过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE + getPriority();
    }

    /**
     * 脱敏配置类
     */
    private static class MaskingConfig {
        private List<String> fields = Arrays.asList("password", "phone", "idCard", "bankCard", "email");
        private Map<String, String> rules = new HashMap<>();
        private boolean maskRequest = true;
        private boolean maskResponse = true;

        public MaskingConfig() {
            // 设置默认规则
            rules.put("phone", "138****8888");
            rules.put("idCard", "1101**********1234");
            rules.put("bankCard", "**** **** **** 1234");
        }

        public static MaskingConfig defaultConfig() {
            return new MaskingConfig();
        }

        public List<String> getFields() { return fields; }
        public void setFields(List<String> fields) { this.fields = fields; }
        public Map<String, String> getRules() { return rules; }
        public void setRules(Map<String, String> rules) { this.rules = rules; }
        public boolean isMaskRequest() { return maskRequest; }
        public void setMaskRequest(boolean maskRequest) { this.maskRequest = maskRequest; }
        public boolean isMaskResponse() { return maskResponse; }
        public void setMaskResponse(boolean maskResponse) { this.maskResponse = maskResponse; }
    }
}
