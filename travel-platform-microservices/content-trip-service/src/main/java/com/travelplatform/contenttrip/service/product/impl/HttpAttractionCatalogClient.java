package com.travelplatform.contenttrip.service.product.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.contenttrip.service.product.AttractionCatalogClient;
import com.travelplatform.contenttrip.service.product.AttractionSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpAttractionCatalogClient implements AttractionCatalogClient {
    private final RestClient restClient;

    public HttpAttractionCatalogClient(RestClient.Builder builder,
                                       @Value("${services.product.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public List<AttractionSnapshot> listByCity(String city) {
        return request("city", city);
    }

    @Override
    public List<AttractionSnapshot> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return request("ids", ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    private List<AttractionSnapshot> request(String parameter, String value) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/internal/attractions").queryParam(parameter, value).build())
                    .retrieve().body(JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || !response.path("data").isArray()) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "景点数据查询失败");
            }
            List<AttractionSnapshot> result = new ArrayList<>();
            response.path("data").forEach(node -> result.add(convert(node)));
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "商品服务暂不可用，请稍后重试");
        }
    }

    private AttractionSnapshot convert(JsonNode node) {
        AttractionSnapshot item = new AttractionSnapshot();
        item.setId(node.path("id").asLong());
        item.setCity(node.path("city").asText(""));
        item.setDistrict(node.path("district").asText(null));
        item.setAttractionName(node.path("attractionName").asText(""));
        item.setAttractionType(node.path("attractionType").asText(null));
        item.setTags(node.path("tags").asText(null));
        item.setDescription(node.path("description").asText(null));
        item.setSuggestedDuration(node.path("suggestedDuration").asText(null));
        item.setPriority(node.path("priority").isNumber() ? node.path("priority").asInt() : null);
        item.setStatus(node.path("status").asInt(1));
        return item;
    }
}
