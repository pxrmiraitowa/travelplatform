package com.travelplatform.contenttrip.service.product.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.common.constant.OrderBizTypeConstant;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.contenttrip.service.product.ProductSnapshot;
import com.travelplatform.contenttrip.service.product.ProductSnapshotService;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class HttpProductSnapshotServiceImpl implements ProductSnapshotService {

    private final RestClient restClient;

    public HttpProductSnapshotServiceImpl(RestClient.Builder builder,
                                          @Value("${services.product.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public ProductSnapshot getProductSnapshot(String productType, Long productId) {
        String normalizedType = normalizeProductType(productType);
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/internal/products/snapshot")
                            .queryParam("productType", normalizedType)
                            .queryParam("productId", productId)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || response.path("data").isMissingNode()) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商品不存在或暂不可用");
            }
            JsonNode data = response.path("data");
            return new ProductSnapshot(
                    data.path("productType").asText(normalizedType),
                    data.path("productId").asLong(productId),
                    data.path("productName").asText("商品信息"),
                    decimal(data, "currentPrice")
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "商品服务暂不可用，请稍后重试");
        }
    }

    @Override
    public String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "产品类型不能为空");
        }
        String value = productType.trim().toUpperCase(Locale.ROOT);
        if (!OrderBizTypeConstant.HOTEL.equals(value)
                && !OrderBizTypeConstant.FLIGHT.equals(value)
                && !OrderBizTypeConstant.TRAIN.equals(value)
                && !OrderBizTypeConstant.TOUR.equals(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的产品类型");
        }
        return value;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品价格信息不完整");
        }
        return node.path(field).decimalValue();
    }
}
