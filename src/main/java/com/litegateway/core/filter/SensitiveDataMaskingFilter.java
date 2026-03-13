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
 */
@Component
public class SensitiveDataMaskingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveDataMaskingFilter.class);

    // 敏感字段列表
    private final Set<String> sensitiveFields = new HashSet<>(Arrays.asList(
            "password", "passwd", "pwd",
            "phone", "mobile", "telephone",
            "idCard", "idcard", "id_card", "id",
            "bankCard", "bankcard", "bank_card", "card",
            "creditCard", "creditcard", "credit_card",
            "email", "emailAddress", "email_address",
            "address", "location",
            "name", "fullName", "full_name"
    ));

    // 脱敏策略
    private final Map<String, Pattern> maskingPatterns = new HashMap<>();

    public SensitiveDataMaskingFilter() {
        // 初始化脱敏正则表达式
        maskingPatterns.put("phone", Pattern.compile("1[3-9]\\d{9}"));
        maskingPatterns.put("idCard", Pattern.compile("[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]"));
        maskingPatterns.put("bankCard", Pattern.compile("[1-9]\\d{15,18}"));
        maskingPatterns.put("email", Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 处理请求
        ServerHttpRequest decoratedRequest = decorateRequest(exchange, exchange.getRequest());
        
        // 处理响应
        ServerHttpResponse decoratedResponse = decorateResponse(exchange.getResponse());
        
        return chain.filter(exchange.mutate()
                .request(decoratedRequest)
                .response(decoratedResponse)
                .build());
    }

    private ServerHttpRequest decorateRequest(ServerWebExchange exchange, ServerHttpRequest request) {
        if (request.getMethod() == HttpMethod.POST || request.getMethod() == HttpMethod.PUT) {
            return new ServerHttpRequestDecorator(request) {
                @Override
                public Flux<DataBuffer> getBody() {
                    return super.getBody()
                            .flatMap(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                
                                String body = new String(bytes, StandardCharsets.UTF_8);
                                String maskedBody = maskSensitiveData(body);
                                
                                logger.debug("Original request body: {}", body);
                                logger.debug("Masked request body: {}", maskedBody);
                                
                                return Flux.just(exchange.getResponse().bufferFactory().wrap(maskedBody.getBytes(StandardCharsets.UTF_8)));
                            });
                }
            };
        }
        return request;
    }

    private ServerHttpResponse decorateResponse(ServerHttpResponse response) {
        return new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                HttpHeaders headers = getDelegate().getHeaders();
                MediaType contentType = headers.getContentType();
                
                if (contentType != null && (contentType.includes(MediaType.APPLICATION_JSON) || 
                        contentType.includes(MediaType.APPLICATION_JSON_UTF8) ||
                        contentType.includes(MediaType.TEXT_PLAIN))) {
                    return super.writeWith(Flux.from(body)
                            .flatMap(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                
                                String bodyStr = new String(bytes, StandardCharsets.UTF_8);
                                String maskedBody = maskSensitiveData(bodyStr);
                                
                                logger.debug("Original response body: {}", bodyStr);
                                logger.debug("Masked response body: {}", maskedBody);
                                
                                return Flux.just(getDelegate().bufferFactory().wrap(maskedBody.getBytes(StandardCharsets.UTF_8)));
                            }));
                }
                return super.writeWith(body);
            }
        };
    }

    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        String maskedData = data;
        
        // 脱敏手机号
        maskedData = maskPattern(maskedData, "phone", "138****8888");
        
        // 脱敏身份证号
        maskedData = maskPattern(maskedData, "idCard", "1101**********1234");
        
        // 脱敏银行卡号
        maskedData = maskPattern(maskedData, "bankCard", "**** **** **** 1234");
        
        // 脱敏邮箱
        maskedData = maskEmail(maskedData);
        
        // 脱敏密码
        maskedData = maskPassword(maskedData);
        
        return maskedData;
    }

    private String maskPattern(String data, String patternKey, String replacement) {
        Pattern pattern = maskingPatterns.get(patternKey);
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            return matcher.replaceAll(replacement);
        }
        return data;
    }

    private String maskEmail(String data) {
        Pattern pattern = maskingPatterns.get("email");
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            return matcher.replaceAll(match -> {
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
        // 简单的密码脱敏实现
        String result = data;
        String[] passwordKeywords = {"password", "passwd", "pwd"};
        
        for (String keyword : passwordKeywords) {
            int index = result.toLowerCase().indexOf(keyword + ":\"" );
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
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}