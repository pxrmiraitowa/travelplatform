package com.travelplatform.order.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.order.dto.OrderCreateRequest;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpProductSnapshotClient implements ProductSnapshotClient {

    private final RestClient restClient;

    public HttpProductSnapshotClient(RestClient.Builder builder,
                                     @Value("${services.product.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public ProductSnapshot getSnapshot(OrderCreateRequest request) {
        String type = request.getProductType().trim().toUpperCase(Locale.ROOT);
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/internal/products/snapshot")
                                .queryParam("productType", type)
                                .queryParam("productId", request.getProductId());
                        if (request.getVariantId() != null) {
                            builder.queryParam("variantId", request.getVariantId());
                        }
                        if (StringUtils.hasText(request.getVariantName())) {
                            builder.queryParam("variantName", request.getVariantName());
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || response.path("data").isMissingNode()) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商品不存在或暂不可预订");
            }
            return convert(request, response.path("data"));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "商品服务暂不可用，请稍后重试");
        }
    }

    private ProductSnapshot convert(OrderCreateRequest request, JsonNode data) {
        return new ProductSnapshot(
                data.path("productId").asLong(request.getProductId()),
                text(data, "productName"),
                text(data, "summary"),
                decimal(data, "currentPrice"),
                data.path("available").asBoolean(false),
                data.path("stock").isMissingNode() || data.path("stock").isNull() ? null : data.path("stock").asInt()
        );
    }

    private String text(JsonNode node, String field) { return node.path(field).asText(""); }
    private BigDecimal decimal(JsonNode node, String field) {
        if (!node.hasNonNull(field)) throw badRequest("商品价格信息不完整");
        return node.path(field).decimalValue();
    }
    private BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }
}
