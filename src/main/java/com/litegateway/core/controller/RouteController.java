package com.litegateway.core.controller;

import com.litegateway.core.common.web.Result;
import com.litegateway.core.dto.RouteDTO;
import com.litegateway.core.route.DynamicRouteDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 路由管理控制器
 * 提供API接口来管理动态路由
 */
@RestController
@RequestMapping("/gateway/route")
public class RouteController {

    private static final Logger logger = LoggerFactory.getLogger(RouteController.class);

    @Autowired
    private DynamicRouteDefinitionRepository routeRepository;

    /**
     * 分页查询路由列表
     */
    @GetMapping("/page")
    public Result<List<RouteDefinition>> getRoutePage(@RequestParam(defaultValue = "1") int pageNum, 
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        List<RouteDefinition> routes = routeRepository.getRouteDefinitions().collectList().block();
        // 简单实现，实际应该从数据库分页查询
        return Result.success(routes);
    }

    /**
     * 查询路由列表
     */
    @GetMapping("/list")
    public Result<List<RouteDefinition>> getRouteList() {
        List<RouteDefinition> routes = routeRepository.getRouteDefinitions().collectList().block();
        return Result.success(routes);
    }

    /**
     * 根据ID获取路由详情
     */
    @GetMapping("/{id}")
    public Result<RouteDefinition> getRouteById(@PathVariable String id) {
        RouteDefinition route = routeRepository.getRouteDefinition(id).block();
        if (route != null) {
            return Result.success(route);
        } else {
            return Result.fail("Route not found");
        }
    }

    /**
     * 添加路由
     */
    @PostMapping
    public Result<Void> addRoute(@RequestBody RouteDefinition route) {
        try {
            routeRepository.save(Mono.just(route)).block();
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to add route: {}", e.getMessage());
            return Result.fail("Failed to add route: " + e.getMessage());
        }
    }

    /**
     * 更新路由
     */
    @PutMapping("/{id}")
    public Result<Void> updateRoute(@PathVariable String id, @RequestBody RouteDefinition route) {
        try {
            // 确保路由ID一致
            route.setId(id);
            routeRepository.save(Mono.just(route)).block();
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to update route: {}", e.getMessage());
            return Result.fail("Failed to update route: " + e.getMessage());
        }
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRoute(@PathVariable String id) {
        try {
            routeRepository.delete(Mono.just(id)).block();
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to delete route: {}", e.getMessage());
            return Result.fail("Failed to delete route: " + e.getMessage());
        }
    }

    /**
     * 修改路由状态
     */
    @PatchMapping("/{id}/status")
    public Result<Void> updateRouteStatus(@PathVariable String id, @RequestParam String status) {
        try {
            // 实际实现应该更新路由状态
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to update route status: {}", e.getMessage());
            return Result.fail("Failed to update route status: " + e.getMessage());
        }
    }

    /**
     * 刷新配置
     */
    @PostMapping("/reload")
    public Result<Void> reloadConfig() {
        try {
            // 实际实现应该重新加载网关配置
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to reload config: {}", e.getMessage());
            return Result.fail("Failed to reload config: " + e.getMessage());
        }
    }

    /**
     * 获取服务所有实例
     */
    @GetMapping("/instances")
    public Result<List<Object>> getInstances(@RequestParam String serviceName) {
        try {
            // 实际实现应该从服务发现获取实例
            return Result.success(List.of());
        } catch (Exception e) {
            logger.error("Failed to get instances: {}", e.getMessage());
            return Result.fail("Failed to get instances: " + e.getMessage());
        }
    }

    /**
     * 分页获取服务实例
     */
    @GetMapping("/instances/page")
    public Result<List<Object>> getInstancesPage(@RequestParam(defaultValue = "1") int pageNum, 
                                               @RequestParam(defaultValue = "10") int pageSize) {
        try {
            // 实际实现应该从服务发现分页获取实例
            return Result.success(List.of());
        } catch (Exception e) {
            logger.error("Failed to get instances page: {}", e.getMessage());
            return Result.fail("Failed to get instances page: " + e.getMessage());
        }
    }

    /**
     * 更新实例权重
     */
    @PostMapping("/instances/weight")
    public Result<Void> updateInstanceWeight(@RequestBody Object instance) {
        try {
            // 实际实现应该更新实例权重
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to update instance weight: {}", e.getMessage());
            return Result.fail("Failed to update instance weight: " + e.getMessage());
        }
    }

    /**
     * 更新实例启用状态
     */
    @PostMapping("/instances/enabled")
    public Result<Void> updateInstanceEnabled(@RequestBody Object instance) {
        try {
            // 实际实现应该更新实例启用状态
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to update instance enabled: {}", e.getMessage());
            return Result.fail("Failed to update instance enabled: " + e.getMessage());
        }
    }

    /**
     * 获取服务所有接口
     */
    @GetMapping("/{id}/interfaces")
    public Result<List<Object>> getInterfaces(@PathVariable String id) {
        try {
            // 实际实现应该从Swagger获取接口信息
            return Result.success(List.of());
        } catch (Exception e) {
            logger.error("Failed to get interfaces: {}", e.getMessage());
            return Result.fail("Failed to get interfaces: " + e.getMessage());
        }
    }
}
