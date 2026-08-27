package com.travelplatform.order.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpProductSnapshotClient implements ProductSnapshotClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    @Override
    public CouponSettlement settleCoupon(String productType, Long couponId, BigDecimal originalAmount) {
        if (couponId == null) {
            return new CouponSettlement(null, null, originalAmount, BigDecimal.ZERO, originalAmount, false);
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/internal/coupons/settlement")
                            .queryParam("productType", productType)
                            .queryParam("couponId", couponId)
                            .queryParam("originalAmount", originalAmount)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || response.path("data").isMissingNode()) {
                throw badRequest(response == null ? "优惠券不可用" : response.path("message").asText("优惠券不可用"));
            }
            JsonNode data = response.path("data");
            return new CouponSettlement(
                    data.hasNonNull("couponId") ? data.path("couponId").asLong() : null,
                    data.hasNonNull("couponName") ? data.path("couponName").asText() : null,
                    decimal(data, "originalAmount"),
                    decimal(data, "discountAmount"),
                    decimal(data, "payableAmount"),
                    data.path("used").asBoolean(false));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw badRequest(errorMessage(exception, "优惠券不可用"));
            }
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "优惠券服务暂不可用，请稍后重试");
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "优惠券服务暂不可用，请稍后重试");
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

    private String errorMessage(RestClientResponseException exception, String fallback) {
        try {
            return OBJECT_MAPPER.readTree(exception.getResponseBodyAsString()).path("message").asText(fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
