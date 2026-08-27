package com.travelplatform.order.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.order.dto.OrderCreateRequest;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
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
        String path = switch (type) {
            case "FLIGHT" -> "/api/public/flights/{id}";
            case "TRAIN" -> "/api/public/trains/{id}";
            case "HOTEL" -> "/api/public/hotels/{id}";
            case "TOUR" -> "/api/public/tours/{id}";
            default -> throw badRequest("不支持的商品类型");
        };
        try {
            JsonNode response = restClient.get().uri(path, request.getProductId()).retrieve().body(JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || response.path("data").isMissingNode()) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商品不存在或暂不可预订");
            }
            return convert(type, request, response.path("data"));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "商品服务暂不可用，请稍后重试");
        }
    }

    private ProductSnapshot convert(String type, OrderCreateRequest request, JsonNode data) {
        return switch (type) {
            case "FLIGHT" -> snapshot(request.getProductId(), text(data, "flightNo"),
                    text(data, "departureCity") + " → " + text(data, "arrivalCity"),
                    decimal(data, "price"), data.path("status").asInt(1) == 1, data.path("stock").asInt());
            case "TRAIN" -> trainSnapshot(request, data);
            case "HOTEL" -> hotelSnapshot(request, data);
            case "TOUR" -> snapshot(request.getProductId(), text(data, "packageName"),
                    text(data, "departureCity") + " → " + text(data, "destination"),
                    decimal(data, "price"), true, data.path("stock").asInt());
            default -> throw badRequest("不支持的商品类型");
        };
    }

    private ProductSnapshot trainSnapshot(OrderCreateRequest request, JsonNode data) {
        JsonNode selected = selectByName(data.path("seatOptions"), "seatType", request.getVariantName());
        if (selected == null) {
            throw badRequest("没有可预订的席别");
        }
        String summary = text(data, "departureCity") + " → " + text(data, "arrivalCity")
                + " / " + text(selected, "seatType");
        return snapshot(request.getProductId(), text(data, "trainNo"), summary, decimal(selected, "price"),
                data.path("status").asInt(1) == 1 && selected.path("available").asBoolean(true),
                selected.path("stock").asInt());
    }

    private ProductSnapshot hotelSnapshot(OrderCreateRequest request, JsonNode data) {
        JsonNode selected = selectRoom(data.path("roomList"), request.getVariantId(), request.getVariantName());
        if (selected == null) {
            throw badRequest("没有可预订的房型");
        }
        String name = text(data, "hotelName");
        return snapshot(request.getProductId(), name, text(selected, "roomName"), decimal(selected, "price"),
                selected.path("stock").asInt() > 0, selected.path("stock").asInt());
    }

    private JsonNode selectRoom(JsonNode rooms, Long variantId, String variantName) {
        if (!rooms.isArray()) return null;
        for (JsonNode room : rooms) {
            boolean idMatches = variantId == null || room.path("id").asLong() == variantId;
            boolean nameMatches = !StringUtils.hasText(variantName) || variantName.equals(room.path("roomName").asText());
            if (idMatches && nameMatches && room.path("stock").asInt() > 0) return room;
        }
        return null;
    }

    private JsonNode selectByName(JsonNode items, String field, String expected) {
        if (!items.isArray()) return null;
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if ((!StringUtils.hasText(expected) || expected.equals(item.path(field).asText()))
                    && item.path("stock").asInt() > 0) return item;
        }
        return null;
    }

    private ProductSnapshot snapshot(Long id, String name, String summary, BigDecimal price,
                                     boolean enabled, int stock) {
        return new ProductSnapshot(id, name, summary, price, enabled && stock > 0, stock);
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
