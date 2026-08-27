package com.travelplatform.contenttrip.service.user.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.contenttrip.service.user.UserBasicClient;
import com.travelplatform.contenttrip.service.user.UserBasicInfo;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpUserBasicClient implements UserBasicClient {

    private final RestClient restClient;

    public HttpUserBasicClient(RestClient.Builder builder,
                               @Value("${services.user.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Map<Long, UserBasicInfo> listBasicUsers(Collection<Long> userIds) {
        String ids = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (ids.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/internal/users/basic")
                            .queryParam("ids", ids)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || !response.path("data").isArray()) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "用户基础信息查询失败");
            }
            return response.path("data").findValuesAsText("id").stream().collect(Collectors.toMap(Long::valueOf, id -> {
                JsonNode user = findById(response.path("data"), Long.parseLong(id));
                UserBasicInfo info = new UserBasicInfo();
                info.setId(user.path("id").asLong());
                info.setUsername(user.path("username").asText(""));
                info.setNickname(user.path("nickname").asText(""));
                info.setAvatar(user.path("avatar").isNull() ? null : user.path("avatar").asText(null));
                return info;
            }));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "用户服务暂不可用，请稍后重试");
        }
    }

    private JsonNode findById(JsonNode users, Long id) {
        for (JsonNode user : users) {
            if (user.path("id").asLong() == id) {
                return user;
            }
        }
        throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "用户基础信息响应不完整");
    }
}
