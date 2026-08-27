package com.travelplatform.contenttrip.service.tripplan.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.contenttrip.dto.tripplan.AiTripPlanPreviewRequest;
import com.travelplatform.contenttrip.dto.tripplan.AiTripPlanSaveDayRequest;
import com.travelplatform.contenttrip.dto.tripplan.AiTripPlanSaveRequest;
import com.travelplatform.contenttrip.entity.Attraction;
import com.travelplatform.contenttrip.entity.TripPlan;
import com.travelplatform.contenttrip.entity.TripPlanItem;
import com.travelplatform.contenttrip.mapper.AttractionMapper;
import com.travelplatform.contenttrip.mapper.TripPlanItemMapper;
import com.travelplatform.contenttrip.mapper.TripPlanMapper;
import com.travelplatform.contenttrip.security.CurrentUserProvider;
import com.travelplatform.contenttrip.service.tripplan.AiTripPlanService;
import com.travelplatform.contenttrip.vo.tripplan.AiTripPlanAttractionVO;
import com.travelplatform.contenttrip.vo.tripplan.AiTripPlanPreviewDayVO;
import com.travelplatform.contenttrip.vo.tripplan.AiTripPlanPreviewVO;
import com.travelplatform.contenttrip.vo.tripplan.TripPlanDetailVO;
import com.travelplatform.contenttrip.vo.tripplan.TripPlanItemVO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiTripPlanServiceImpl implements AiTripPlanService {

    private static final String SOURCE_TYPE_AI = "AI";
    private static final String GENERATION_MODE_LOCAL = "LOCAL_FALLBACK";
    private static final int CANDIDATE_LIMIT = 30;
    private static final int MAX_ATTRACTIONS_PER_DAY = 5;
    private static final Map<String, List<String>> PREFERENCE_KEYWORDS = buildPreferenceKeywords();

    private final AttractionMapper attractionMapper;
    private final TripPlanMapper tripPlanMapper;
    private final TripPlanItemMapper tripPlanItemMapper;
    private final CurrentUserProvider currentUserProvider;

    public AiTripPlanServiceImpl(AttractionMapper attractionMapper,
                                 TripPlanMapper tripPlanMapper,
                                 TripPlanItemMapper tripPlanItemMapper,
                                 CurrentUserProvider currentUserProvider) {
        this.attractionMapper = attractionMapper;
        this.tripPlanMapper = tripPlanMapper;
        this.tripPlanItemMapper = tripPlanItemMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public AiTripPlanPreviewVO buildPreview(AiTripPlanPreviewRequest request) {
        String destination = normalizeCity(request.getDestination());
        List<String> preferences = sanitizePreferences(request.getPreferences());
        List<Attraction> candidates = loadCandidateAttractions(destination, preferences);
        if (candidates.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "当前目的地暂无可用于规划的景点数据");
        }

        List<DayPlan> localPlan = buildLocalPlan(candidates, request.getTotalDays(), preferences);
        AiTripPlanPreviewVO preview = new AiTripPlanPreviewVO();
        preview.setPlanName(buildPlanName(destination, request.getTotalDays(), preferences));
        preview.setDestination(destination);
        preview.setTotalDays(request.getTotalDays());
        preview.setStartDate(request.getStartDate());
        preview.setPreferences(preferences);
        preview.setSourceType(SOURCE_TYPE_AI);
        preview.setGenerationMode(GENERATION_MODE_LOCAL);
        preview.setDays(localPlan.stream().map(this::toPreviewDayVO).toList());
        return preview;
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

        validateSaveDays(days, request.getTotalDays(), destination, attractionMap);

        TripPlan plan = new TripPlan();
        plan.setUserId(currentUserProvider.getCurrentUserId());
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

        TripPlanDetailVO detail = new TripPlanDetailVO();
        detail.setId(plan.getId());
        detail.setPlanName(plan.getPlanName());
        detail.setTotalDays(plan.getTotalDays());
        detail.setStartDate(plan.getStartDate());
        detail.setRemark(plan.getRemark());
        detail.setSourceType(plan.getSourceType());
        detail.setCreateTime(plan.getCreateTime());
        detail.setItems(savedItems);
        return detail;
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
                .limit(CANDIDATE_LIMIT)
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
        int perDay = Math.max(1, Math.min(MAX_ATTRACTIONS_PER_DAY, (int) Math.ceil((double) ordered.size() / dayCount)));
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

    private void validateSaveDays(List<AiTripPlanSaveDayRequest> days,
                                  Integer totalDays,
                                  String destination,
                                  Map<Long, Attraction> attractionMap) {
        Set<Integer> dayNos = new HashSet<>();
        Set<Long> usedAttractionIds = new HashSet<>();
        for (AiTripPlanSaveDayRequest day : days) {
            if (day.getDayNo() == null || day.getDayNo() < 1 || day.getDayNo() > totalDays) {
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
        AiTripPlanAttractionVO vo = new AiTripPlanAttractionVO();
        vo.setId(attraction.getId());
        vo.setCity(attraction.getCity());
        vo.setDistrict(attraction.getDistrict());
        vo.setAttractionName(attraction.getAttractionName());
        vo.setAttractionType(attraction.getAttractionType());
        vo.setTags(splitTags(attraction.getTags()));
        vo.setDescription(attraction.getDescription());
        vo.setSuggestedDuration(attraction.getSuggestedDuration());
        return vo;
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

    private String buildFallbackReason(int dayNo, List<String> preferences, List<Attraction> attractions) {
        String prefix = preferences.isEmpty()
                ? "这一天优先安排当地代表性景点，便于快速建立整体印象。"
                : "这一天围绕“" + String.join("、", preferences) + "”偏好挑选景点，兼顾代表性与游玩节奏。";
        if (attractions.isEmpty()) {
            return prefix;
        }
        return "Day " + dayNo + "：" + prefix + " 推荐景点包括"
                + attractions.stream().map(Attraction::getAttractionName).collect(Collectors.joining("、")) + "。";
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
        mapping.put("亲子", List.of("亲子", "乐园", "互动", "博物馆", "科普"));
        mapping.put("休闲", List.of("休闲", "公园", "湖泊", "街区", "园林", "夜景"));
        mapping.put("城市地标", List.of("地标", "建筑", "观景", "夜景", "城市"));
        return mapping;
    }

    private record DayPlan(Integer dayNo, List<Attraction> attractions, String reason) {
    }
}
