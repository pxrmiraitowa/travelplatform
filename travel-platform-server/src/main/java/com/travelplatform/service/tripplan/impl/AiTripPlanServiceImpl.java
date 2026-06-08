package com.travelplatform.service.tripplan.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.config.AiPlannerProperties;
import com.travelplatform.dto.tripplan.AiTripPlanPreviewRequest;
import com.travelplatform.dto.tripplan.AiTripPlanSaveDayRequest;
import com.travelplatform.dto.tripplan.AiTripPlanSaveRequest;
import com.travelplatform.entity.Attraction;
import com.travelplatform.entity.TripPlan;
import com.travelplatform.entity.TripPlanItem;
import com.travelplatform.mapper.AttractionMapper;
import com.travelplatform.mapper.TripPlanItemMapper;
import com.travelplatform.mapper.TripPlanMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.tripplan.AiTripPlanService;
import com.travelplatform.vo.tripplan.AiTripPlanAttractionVO;
import com.travelplatform.vo.tripplan.AiTripPlanPreviewDayVO;
import com.travelplatform.vo.tripplan.AiTripPlanPreviewVO;
import com.travelplatform.vo.tripplan.TripPlanDetailVO;
import com.travelplatform.vo.tripplan.TripPlanItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AiTripPlanServiceImpl implements AiTripPlanService {

    private static final Logger log = LoggerFactory.getLogger(AiTripPlanServiceImpl.class);
    private static final String SOURCE_TYPE_AI = "AI";
    private static final String GENERATION_MODE_AI = "AI_ENHANCED";
    private static final String GENERATION_MODE_FALLBACK = "LOCAL_FALLBACK";
    private static final String RESPONSE_SCHEMA_NAME = "trip_plan_days";
    private static final Map<String, List<String>> PREFERENCE_KEYWORDS = buildPreferenceKeywords();

    private final AttractionMapper attractionMapper;
    private final TripPlanMapper tripPlanMapper;
    private final TripPlanItemMapper tripPlanItemMapper;
    private final AiPlannerProperties properties;
    private final ObjectMapper objectMapper;

    public AiTripPlanServiceImpl(AttractionMapper attractionMapper,
                                 TripPlanMapper tripPlanMapper,
                                 TripPlanItemMapper tripPlanItemMapper,
                                 AiPlannerProperties properties,
                                 ObjectMapper objectMapper) {
        this.attractionMapper = attractionMapper;
        this.tripPlanMapper = tripPlanMapper;
        this.tripPlanItemMapper = tripPlanItemMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiTripPlanPreviewVO buildPreview(AiTripPlanPreviewRequest request) {
        String normalizedDestination = normalizeCity(request.getDestination());
        List<String> preferences = sanitizePreferences(request.getPreferences());
        List<Attraction> candidates = loadCandidateAttractions(normalizedDestination, preferences);
        if (candidates.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "当前目的地暂无可用于规划的景点数据");
        }

        List<DayPlan> localPlan = buildLocalPlan(candidates, request.getTotalDays(), preferences);
        List<DayPlan> finalPlan = localPlan;
        String generationMode = GENERATION_MODE_FALLBACK;
        if (isAiAvailable()) {
            try {
                List<DayPlan> aiPlan = requestAiOptimization(request, candidates, localPlan, preferences);
                if (!aiPlan.isEmpty()) {
                    finalPlan = aiPlan;
                    generationMode = GENERATION_MODE_AI;
                }
            } catch (Exception exception) {
                log.warn("AI trip plan preview fallback triggered. destination={}, model={}, message={}",
                        normalizedDestination, properties.getModel(), exception.getMessage(), exception);
            }
        }

        AiTripPlanPreviewVO previewVO = new AiTripPlanPreviewVO();
        previewVO.setPlanName(buildPlanName(normalizedDestination, request.getTotalDays(), preferences));
        previewVO.setDestination(normalizedDestination);
        previewVO.setTotalDays(request.getTotalDays());
        previewVO.setStartDate(request.getStartDate());
        previewVO.setPreferences(preferences);
        previewVO.setSourceType(SOURCE_TYPE_AI);
        previewVO.setGenerationMode(generationMode);
        previewVO.setDays(finalPlan.stream().map(this::toPreviewDayVO).toList());
        return previewVO;
    }

    @Override
    @Transactional
    public TripPlanDetailVO savePlan(AiTripPlanSaveRequest request) {
        List<AiTripPlanSaveDayRequest> days = request.getDays() == null ? List.of() : request.getDays();
        if (request.getTotalDays() == null || request.getTotalDays() < 1 || request.getTotalDays() > 10) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "停留天数必须在 1 到 10 天之间");
        }
        if (days.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预览数据不能为空");
        }

        List<String> preferences = sanitizePreferences(request.getPreferences());
        String destination = normalizeCity(request.getDestination());
        List<Long> attractionIds = days.stream()
                .map(AiTripPlanSaveDayRequest::getAttractionIds)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<Long, Attraction> attractionMap = attractionIds.isEmpty()
                ? Map.of()
                : attractionMapper.selectBatchIds(attractionIds).stream()
                .collect(Collectors.toMap(Attraction::getId, attraction -> attraction));

        Set<Integer> dayNos = new HashSet<>();
        Set<Long> usedAttractionIds = new HashSet<>();
        for (AiTripPlanSaveDayRequest day : days) {
            if (day.getDayNo() == null || day.getDayNo() < 1 || day.getDayNo() > request.getTotalDays()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预览天数超出停留天数范围");
            }
            if (!dayNos.add(day.getDayNo())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预览中存在重复的天数");
            }
            if (day.getAttractionIds() == null || day.getAttractionIds().isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "每天至少需要保留一个景点");
            }
            for (Long attractionId : day.getAttractionIds()) {
                if (!usedAttractionIds.add(attractionId)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "同一景点不能在多天重复保存");
                }
                Attraction attraction = attractionMap.get(attractionId);
                if (attraction == null || !Integer.valueOf(1).equals(attraction.getStatus())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预览中的景点已失效，请重新生成");
                }
                if (!normalizeCity(attraction.getCity()).equals(destination)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预览中的景点与当前目的地不匹配");
                }
            }
        }

        TripPlan plan = new TripPlan();
        plan.setUserId(SecurityUtils.getCurrentUserId());
        plan.setPlanName(request.getPlanName().trim());
        plan.setTotalDays(request.getTotalDays());
        plan.setStartDate(request.getStartDate());
        plan.setRemark(buildPlanRemark(destination, preferences));
        plan.setSourceType(SOURCE_TYPE_AI);
        tripPlanMapper.insert(plan);

        List<TripPlanItemVO> savedItems = new ArrayList<>();
        days.stream()
                .sorted(Comparator.comparing(AiTripPlanSaveDayRequest::getDayNo))
                .forEach(day -> {
                    List<Attraction> dayAttractions = day.getAttractionIds().stream()
                            .map(attractionMap::get)
                            .filter(Objects::nonNull)
                            .toList();
                    TripPlanItem item = new TripPlanItem();
                    item.setPlanId(plan.getId());
                    item.setDayNo(day.getDayNo());
                    item.setDestination(buildDayDestination(dayAttractions));
                    item.setHotel(null);
                    item.setTransportType(null);
                    item.setRemark(buildItemRemark(dayAttractions, day.getReason()));
                    tripPlanItemMapper.insert(item);
                    savedItems.add(toItemVO(item));
                });

        TripPlanDetailVO detailVO = new TripPlanDetailVO();
        detailVO.setId(plan.getId());
        detailVO.setPlanName(plan.getPlanName());
        detailVO.setTotalDays(plan.getTotalDays());
        detailVO.setStartDate(plan.getStartDate());
        detailVO.setRemark(plan.getRemark());
        detailVO.setSourceType(plan.getSourceType());
        detailVO.setCreateTime(plan.getCreateTime());
        detailVO.setItems(savedItems);
        return detailVO;
    }

    private List<Attraction> loadCandidateAttractions(String destination, List<String> preferences) {
        List<Attraction> allAttractions = attractionMapper.selectList(new LambdaQueryWrapper<Attraction>()
                .eq(Attraction::getStatus, 1)
                .orderByDesc(Attraction::getPriority)
                .orderByAsc(Attraction::getId));

        List<Attraction> cityMatches = allAttractions.stream()
                .filter(attraction -> normalizeCity(attraction.getCity()).equals(destination))
                .toList();
        if (cityMatches.isEmpty()) {
            cityMatches = allAttractions.stream()
                    .filter(attraction -> normalizeCity(attraction.getCity()).contains(destination)
                            || destination.contains(normalizeCity(attraction.getCity())))
                    .toList();
        }

        return cityMatches.stream()
                .sorted(Comparator.comparingInt((Attraction attraction) -> scoreAttraction(attraction, preferences)).reversed()
                        .thenComparing(Attraction::getPriority, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Attraction::getId))
                .limit(properties.getCandidateLimit())
                .toList();
    }

    private int scoreAttraction(Attraction attraction, List<String> preferences) {
        int baseScore = attraction.getPriority() == null ? 0 : attraction.getPriority() * 10;
        if (preferences.isEmpty()) {
            return baseScore;
        }
        Set<String> attractionKeywords = toKeywordSet(attraction.getTags(), attraction.getAttractionType(), attraction.getDescription());
        int preferenceScore = 0;
        for (String preference : preferences) {
            List<String> keywords = PREFERENCE_KEYWORDS.getOrDefault(preference, List.of(preference));
            for (String keyword : keywords) {
                if (attractionKeywords.contains(keyword.toLowerCase(Locale.ROOT))) {
                    preferenceScore += 35;
                    break;
                }
            }
        }
        return baseScore + preferenceScore;
    }

    private List<DayPlan> buildLocalPlan(List<Attraction> candidates, Integer totalDays, List<String> preferences) {
        List<Attraction> ordered = new ArrayList<>(candidates);
        int dayCount = Math.max(1, totalDays == null ? 1 : totalDays);
        int perDay = Math.max(1, Math.min(properties.getMaxAttractionsPerDay(),
                (int) Math.ceil((double) ordered.size() / dayCount)));
        List<DayPlan> dayPlans = new ArrayList<>();
        int cursor = 0;
        for (int dayNo = 1; dayNo <= dayCount; dayNo++) {
            List<Attraction> dayAttractions = new ArrayList<>();
            while (cursor < ordered.size() && dayAttractions.size() < perDay) {
                dayAttractions.add(ordered.get(cursor));
                cursor++;
            }
            if (dayAttractions.isEmpty() && !ordered.isEmpty()) {
                dayAttractions.add(ordered.get((dayNo - 1) % ordered.size()));
            }
            dayPlans.add(new DayPlan(dayNo, dayAttractions, buildFallbackReason(dayNo, preferences, dayAttractions)));
        }
        return dayPlans;
    }

    private String buildFallbackReason(int dayNo, List<String> preferences, List<Attraction> attractions) {
        String prefix;
        if (preferences.isEmpty()) {
            prefix = "这一天优先安排当地代表性景点，便于首次到访快速建立整体印象。";
        } else {
            prefix = "这一天围绕“" + String.join("、", preferences) + "”偏好挑选景点，兼顾代表性与游玩节奏。";
        }
        if (attractions.isEmpty()) {
            return prefix;
        }
        return "Day " + dayNo + "：" + prefix + " 推荐景点包括" +
                attractions.stream().map(Attraction::getAttractionName).collect(Collectors.joining("、")) + "。";
    }

    private List<DayPlan> requestAiOptimization(AiTripPlanPreviewRequest request,
                                                List<Attraction> candidates,
                                                List<DayPlan> localPlan,
                                                List<String> preferences) throws Exception {
        String prompt = buildPrompt(request, candidates, localPlan);
        RestClient restClient = buildRestClient();
        try {
            return requestAiOptimization(restClient, request, candidates, preferences, prompt,
                    properties.isUseJsonSchemaResponseFormat());
        } catch (Exception exception) {
            if (properties.isUseJsonSchemaResponseFormat()) {
                log.warn("AI structured response attempt failed, retrying without response_format. destination={}, model={}, message={}",
                        normalizeCity(request.getDestination()), properties.getModel(), exception.getMessage());
                return requestAiOptimization(restClient, request, candidates, preferences, prompt, false);
            }
            throw exception;
        }
    }

    private List<DayPlan> requestAiOptimization(RestClient restClient,
                                                AiTripPlanPreviewRequest request,
                                                List<Attraction> candidates,
                                                List<String> preferences,
                                                String prompt,
                                                boolean useResponseFormat) throws Exception {
        JsonNode responseNode = requestChatCompletion(restClient, prompt, useResponseFormat);
        String content = extractChatContent(responseNode);
        return parseAiPlan(content, request.getTotalDays(), candidates, preferences);
    }

    private RestClient buildRestClient() {
        RestClient restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .requestFactory(buildRequestFactory())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey().trim())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        return restClient;
    }

    private JsonNode requestChatCompletion(RestClient restClient, String prompt, boolean useResponseFormat) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("temperature", 0.3);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", "你是一个严谨的旅行规划助手，只能使用给定候选景点，并输出合法 JSON。"),
                Map.of("role", "user", "content", prompt)
        ));
        if (useResponseFormat) {
            payload.put("response_format", buildJsonSchemaResponseFormat());
        }

        return restClient.post()
                .uri(buildChatCompletionsUri())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }

    private String buildPrompt(AiTripPlanPreviewRequest request,
                               List<Attraction> candidates,
                               List<DayPlan> localPlan) throws Exception {
        Map<String, Object> promptRoot = new LinkedHashMap<>();
        promptRoot.put("destination", normalizeCity(request.getDestination()));
        promptRoot.put("totalDays", request.getTotalDays());
        promptRoot.put("preferences", sanitizePreferences(request.getPreferences()));
        promptRoot.put("constraints", List.of(
                "只能使用 candidates 中出现的 attractionId",
                "每天推荐 1 到 " + properties.getMaxAttractionsPerDay() + " 个景点",
                "不要重复景点",
                "reason 使用简体中文，长度控制在 50 字以内",
                "输出必须是 JSON，格式为 {\"days\":[{\"dayNo\":1,\"attractionIds\":[1,2],\"reason\":\"...\"}]}"
        ));
        promptRoot.put("candidates", candidates.stream().map(attraction -> Map.of(
                "id", attraction.getId(),
                "name", attraction.getAttractionName(),
                "district", defaultString(attraction.getDistrict()),
                "type", defaultString(attraction.getAttractionType()),
                "tags", splitTags(attraction.getTags()),
                "description", defaultString(attraction.getDescription()),
                "suggestedDuration", defaultString(attraction.getSuggestedDuration())
        )).toList());
        promptRoot.put("localDraft", localPlan.stream().map(dayPlan -> Map.of(
                "dayNo", dayPlan.dayNo(),
                "attractionIds", dayPlan.attractions().stream().map(Attraction::getId).toList(),
                "reason", dayPlan.reason()
        )).toList());
        return objectMapper.writeValueAsString(promptRoot);
    }

    private String extractChatContent(JsonNode responseNode) {
        if (responseNode == null) {
            throw new IllegalStateException("AI response is empty");
        }
        JsonNode choices = responseNode.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("AI response choices missing");
        }
        String content = extractMessageContent(choices.get(0).path("message").path("content"));
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("AI content missing");
        }
        return content.trim();
    }

    private String extractMessageContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : contentNode) {
                String text = part.path("text").asText(null);
                if (StringUtils.hasText(text)) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(text.trim());
                }
            }
            return builder.toString();
        }
        return contentNode.toString();
    }

    private List<DayPlan> parseAiPlan(String content,
                                      Integer totalDays,
                                      List<Attraction> candidates,
                                      List<String> preferences) throws Exception {
        String jsonContent = stripMarkdownJsonFence(content);
        JsonNode root = objectMapper.readTree(jsonContent);
        ArrayNode daysNode = root.has("days") && root.get("days").isArray()
                ? (ArrayNode) root.get("days")
                : null;
        if (daysNode == null || daysNode.isEmpty()) {
            return List.of();
        }

        Map<Long, Attraction> candidateMap = candidates.stream()
                .collect(Collectors.toMap(Attraction::getId, attraction -> attraction));
        Set<Long> usedAttractionIds = new HashSet<>();
        Set<Integer> dayNos = new HashSet<>();
        List<DayPlan> dayPlans = new ArrayList<>();

        for (JsonNode dayNode : daysNode) {
            int dayNo = dayNode.path("dayNo").asInt(0);
            if (dayNo < 1 || dayNo > totalDays || !dayNos.add(dayNo)) {
                return List.of();
            }
            JsonNode attractionIdsNode = dayNode.path("attractionIds");
            if (!attractionIdsNode.isArray() || attractionIdsNode.isEmpty()
                    || attractionIdsNode.size() > properties.getMaxAttractionsPerDay()) {
                return List.of();
            }
            List<Attraction> dayAttractions = new ArrayList<>();
            for (JsonNode attractionIdNode : attractionIdsNode) {
                long attractionId = attractionIdNode.asLong(-1);
                Attraction attraction = candidateMap.get(attractionId);
                if (attraction == null || !usedAttractionIds.add(attractionId)) {
                    return List.of();
                }
                dayAttractions.add(attraction);
            }
            String reason = dayNode.path("reason").asText("");
            if (!StringUtils.hasText(reason)) {
                reason = buildFallbackReason(dayNo, preferences, dayAttractions);
            }
            dayPlans.add(new DayPlan(dayNo, dayAttractions, truncate(reason.trim(), 255)));
        }

        if (dayPlans.size() != totalDays) {
            return List.of();
        }
        dayPlans.sort(Comparator.comparing(DayPlan::dayNo));
        return dayPlans;
    }

    private AiTripPlanPreviewDayVO toPreviewDayVO(DayPlan dayPlan) {
        AiTripPlanPreviewDayVO dayVO = new AiTripPlanPreviewDayVO();
        dayVO.setDayNo(dayPlan.dayNo());
        dayVO.setDestination(buildDayDestination(dayPlan.attractions()));
        dayVO.setReason(dayPlan.reason());
        dayVO.setAttractions(dayPlan.attractions().stream().map(this::toAttractionVO).toList());
        return dayVO;
    }

    private AiTripPlanAttractionVO toAttractionVO(Attraction attraction) {
        AiTripPlanAttractionVO attractionVO = new AiTripPlanAttractionVO();
        attractionVO.setId(attraction.getId());
        attractionVO.setCity(attraction.getCity());
        attractionVO.setDistrict(attraction.getDistrict());
        attractionVO.setAttractionName(attraction.getAttractionName());
        attractionVO.setAttractionType(attraction.getAttractionType());
        attractionVO.setTags(splitTags(attraction.getTags()));
        attractionVO.setDescription(attraction.getDescription());
        attractionVO.setSuggestedDuration(attraction.getSuggestedDuration());
        return attractionVO;
    }

    private TripPlanItemVO toItemVO(TripPlanItem item) {
        TripPlanItemVO vo = new TripPlanItemVO();
        vo.setId(item.getId());
        vo.setDayNo(item.getDayNo());
        vo.setDestination(item.getDestination());
        vo.setHotel(item.getHotel());
        vo.setTransportType(item.getTransportType());
        vo.setRemark(item.getRemark());
        return vo;
    }

    private String buildDayDestination(List<Attraction> attractions) {
        if (attractions.isEmpty()) {
            return "待定";
        }
        Attraction first = attractions.get(0);
        if (StringUtils.hasText(first.getDistrict())) {
            return first.getDistrict() + "片区";
        }
        return first.getAttractionName();
    }

    private String buildItemRemark(List<Attraction> attractions, String reason) {
        String attractionSummary = attractions.stream()
                .map(Attraction::getAttractionName)
                .collect(Collectors.joining("、"));
        StringBuilder builder = new StringBuilder();
        builder.append("推荐景点：").append(attractionSummary);
        if (StringUtils.hasText(reason)) {
            builder.append("。推荐理由：").append(reason.trim());
        }
        return truncate(builder.toString(), 255);
    }

    private String buildPlanName(String destination, Integer totalDays, List<String> preferences) {
        if (preferences.isEmpty()) {
            return destination + totalDays + "日精选行程";
        }
        return destination + totalDays + "日" + preferences.get(0) + "行程";
    }

    private String buildPlanRemark(String destination, List<String> preferences) {
        if (preferences.isEmpty()) {
            return "AI 生成行程，目的地：" + destination;
        }
        return "AI 生成行程，目的地：" + destination + "，偏好：" + String.join("、", preferences);
    }

    private boolean isAiAvailable() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getBaseUrl())
                && StringUtils.hasText(properties.getModel());
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private SimpleClientHttpRequestFactory buildRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) TimeUnit.SECONDS.toMillis(Math.max(1, properties.getTimeoutSeconds()));
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }

    private URI buildChatCompletionsUri() {
        String path = properties.getChatCompletionsPath();
        if (!StringUtils.hasText(path)) {
            path = "/chat/completions";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return URI.create(path);
        }
        String baseUrl = trimTrailingSlash(properties.getBaseUrl());
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(baseUrl + normalizedPath);
    }

    private Map<String, Object> buildJsonSchemaResponseFormat() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("days"));
        schema.put("properties", Map.of(
                "days", Map.of(
                        "type", "array",
                        "minItems", 1,
                        "items", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "required", List.of("dayNo", "attractionIds", "reason"),
                                "properties", Map.of(
                                        "dayNo", Map.of("type", "integer", "minimum", 1),
                                        "attractionIds", Map.of(
                                                "type", "array",
                                                "minItems", 1,
                                                "items", Map.of("type", "integer", "minimum", 1)
                                        ),
                                        "reason", Map.of("type", "string")
                                )
                        )
                )
        ));

        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", RESPONSE_SCHEMA_NAME,
                        "strict", true,
                        "schema", schema
                )
        );
    }

    private String normalizeCity(String city) {
        String safeCity = city == null ? "" : city.trim();
        return safeCity.replace("市", "").replace("特别行政区", "").replace("自治区", "").trim();
    }

    private List<String> sanitizePreferences(List<String> preferences) {
        if (preferences == null) {
            return List.of();
        }
        return preferences.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        return List.of(tags.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private Set<String> toKeywordSet(String... rawValues) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String rawValue : rawValues) {
            if (!StringUtils.hasText(rawValue)) {
                continue;
            }
            for (String piece : rawValue.split("[,，、\\s]+")) {
                if (StringUtils.hasText(piece)) {
                    keywords.add(piece.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return keywords;
    }

    private String stripMarkdownJsonFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineBreak > -1 && lastFence > firstLineBreak) {
                return trimmed.substring(firstLineBreak + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static Map<String, List<String>> buildPreferenceKeywords() {
        Map<String, List<String>> mapping = new HashMap<>();
        mapping.put("自然风光", List.of("自然风光", "自然", "山水", "湖泊", "公园", "园林", "海滨", "湿地", "森林"));
        mapping.put("人文历史", List.of("人文历史", "历史", "文化", "古镇", "古城", "博物馆", "遗址", "寺庙", "建筑"));
        mapping.put("美食", List.of("美食", "街区", "夜市", "商圈", "生活方式"));
        mapping.put("亲子", List.of("亲子", "乐园", "动物", "互动", "博物馆", "科普"));
        mapping.put("休闲", List.of("休闲", "公园", "湖泊", "街区", "园林", "夜景"));
        mapping.put("城市地标", List.of("地标", "建筑", "观景", "夜景", "城市"));
        return mapping;
    }

    private record DayPlan(Integer dayNo, List<Attraction> attractions, String reason) {
    }
}
