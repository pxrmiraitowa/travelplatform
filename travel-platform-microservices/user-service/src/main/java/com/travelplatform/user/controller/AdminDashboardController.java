package com.travelplatform.user.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.user.mapper.UserMapper;
import com.travelplatform.user.vo.admin.AdminDashboardVO;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String productServiceUrl;
    private final String orderServiceUrl;

    public AdminDashboardController(UserMapper userMapper,
                                    @Value("${PRODUCT_SERVICE_URL:http://localhost:8102}") String productServiceUrl,
                                    @Value("${ORDER_SERVICE_URL:http://localhost:8103}") String orderServiceUrl) {
        this.userMapper = userMapper;
        this.productServiceUrl = productServiceUrl;
        this.orderServiceUrl = orderServiceUrl;
    }

    @GetMapping
    public Result<AdminDashboardVO> dashboard() {
        AdminDashboardVO dashboard = new AdminDashboardVO();
        dashboard.setUserCount(userMapper.selectCount(null));
        Map<?, ?> productStats = stats(productServiceUrl + "/api/internal/products/stats");
        Map<?, ?> orderStats = stats(orderServiceUrl + "/api/internal/orders/stats");
        dashboard.setProductCount(number(productStats, "productCount"));
        dashboard.setOrderCount(number(orderStats, "orderCount"));
        dashboard.setRecentOrderCount(number(orderStats, "recentOrderCount"));
        return Result.success(dashboard);
    }

    private Map<?, ?> stats(String url) {
        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            return response != null && response.get("data") instanceof Map<?, ?> data ? data : Map.of();
        } catch (RestClientException exception) {
            return Map.of();
        }
    }

    private long number(Map<?, ?> data, String key) {
        Object value = data.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
