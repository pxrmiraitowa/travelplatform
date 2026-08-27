package com.travelplatform.contenttrip.service.order.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.service.order.OrderReviewClient;
import com.travelplatform.contenttrip.service.order.OrderReviewContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpOrderReviewClient implements OrderReviewClient {

    private final RestClient restClient;

    public HttpOrderReviewClient(RestClient.Builder builder,
                                 @Value("${services.order.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public OrderReviewContext getReviewContext(Long orderId, Long userId) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/internal/orders/{orderId}/review-context")
                            .queryParam("userId", userId)
                            .build(orderId))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || response.path("data").isMissingNode()) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
            }
            return toContext(response.path("data"));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "订单服务暂不可用，请稍后重试");
        }
    }

    @Override
    public PageResult<OrderReviewContext> listReviewableOrders(Long userId, int pageNum, int pageSize) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/internal/orders/reviewable")
                            .queryParam("userId", userId)
                            .queryParam("pageNum", pageNum)
                            .queryParam("pageSize", pageSize)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || response.path("data").isMissingNode()) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "可评价订单查询失败");
            }
            return toPageResult(response.path("data"));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "订单服务暂不可用，请稍后重试");
        }
    }

    private PageResult<OrderReviewContext> toPageResult(JsonNode data) {
        PageResult<OrderReviewContext> result = new PageResult<>();
        List<OrderReviewContext> records = new ArrayList<>();
        data.path("records").forEach(item -> records.add(toContext(item)));
        result.setRecords(records);
        result.setTotal(data.path("total").asLong(0));
        result.setPageNum(data.path("pageNum").asInt(1));
        result.setPageSize(data.path("pageSize").asInt(records.size()));
        return result;
    }

    private OrderReviewContext toContext(JsonNode data) {
        OrderReviewContext context = new OrderReviewContext();
        context.setOrderId(data.path("orderId").asLong());
        context.setOrderNo(data.path("orderNo").asText(""));
        context.setUserId(data.path("userId").asLong());
        context.setBizType(data.path("bizType").asText(""));
        context.setBizId(data.path("bizId").isMissingNode() || data.path("bizId").isNull() ? null : data.path("bizId").asLong());
        context.setTravelDate(parseDate(data.path("travelDate").asText(null)));
        context.setSummaryTitle(data.path("summaryTitle").asText("订单信息"));
        context.setSummarySubtitle(data.path("summarySubtitle").asText(""));
        context.setCompleted(data.path("completed").asBoolean(false));
        return context;
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
