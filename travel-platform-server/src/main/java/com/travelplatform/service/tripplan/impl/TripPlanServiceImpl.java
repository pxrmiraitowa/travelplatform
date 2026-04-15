package com.travelplatform.service.tripplan.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.tripplan.TripPlanCreateRequest;
import com.travelplatform.dto.tripplan.TripPlanItemCreateRequest;
import com.travelplatform.dto.tripplan.TripPlanItemUpdateRequest;
import com.travelplatform.dto.tripplan.TripPlanUpdateRequest;
import com.travelplatform.entity.TripPlan;
import com.travelplatform.entity.TripPlanItem;
import com.travelplatform.mapper.TripPlanItemMapper;
import com.travelplatform.mapper.TripPlanMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.tripplan.TripPlanService;
import com.travelplatform.vo.tripplan.TripPlanDetailVO;
import com.travelplatform.vo.tripplan.TripPlanItemVO;
import com.travelplatform.vo.tripplan.TripPlanListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TripPlanServiceImpl implements TripPlanService {

    private static final String SOURCE_TYPE_MANUAL = "MANUAL";

    private final TripPlanMapper tripPlanMapper;
    private final TripPlanItemMapper tripPlanItemMapper;

    public TripPlanServiceImpl(TripPlanMapper tripPlanMapper, TripPlanItemMapper tripPlanItemMapper) {
        this.tripPlanMapper = tripPlanMapper;
        this.tripPlanItemMapper = tripPlanItemMapper;
    }

    @Override
    public List<TripPlanListItemVO> listCurrentUserPlans() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TripPlan> plans = tripPlanMapper.selectList(new LambdaQueryWrapper<TripPlan>()
                .eq(TripPlan::getUserId, userId)
                .orderByDesc(TripPlan::getId));

        List<Long> planIds = plans.stream().map(TripPlan::getId).toList();
        Map<Long, Long> itemCountMap = planIds.isEmpty() ? Map.of() :
                tripPlanItemMapper.selectList(new LambdaQueryWrapper<TripPlanItem>()
                                .in(TripPlanItem::getPlanId, planIds))
                        .stream()
                        .collect(Collectors.groupingBy(TripPlanItem::getPlanId, Collectors.counting()));

        return plans.stream().map(plan -> toListItemVO(plan, itemCountMap)).toList();
    }

    @Override
    @Transactional
    public TripPlanDetailVO createPlan(TripPlanCreateRequest request) {
        TripPlan plan = new TripPlan();
        plan.setUserId(SecurityUtils.getCurrentUserId());
        fillPlan(plan, request.getPlanName(), request.getTotalDays(), request.getStartDate(), request.getRemark());
        plan.setSourceType(SOURCE_TYPE_MANUAL);
        tripPlanMapper.insert(plan);
        return buildDetailVO(plan, List.of());
    }

    @Override
    public TripPlanDetailVO getCurrentUserPlanDetail(Long id) {
        TripPlan plan = getOwnedPlan(id);
        return buildDetailVO(plan, listPlanItems(plan.getId()));
    }

    @Override
    @Transactional
    public TripPlanDetailVO updatePlan(Long id, TripPlanUpdateRequest request) {
        TripPlan plan = getOwnedPlan(id);
        int maxDayNo = getMaxDayNo(plan.getId());
        if (request.getTotalDays() != null && request.getTotalDays() < maxDayNo) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "出行总天数不能小于已存在的每日安排天数");
        }
        fillPlan(plan, request.getPlanName(), request.getTotalDays(), request.getStartDate(), request.getRemark());
        tripPlanMapper.updateById(plan);
        return buildDetailVO(plan, listPlanItems(plan.getId()));
    }

    @Override
    @Transactional
    public void deletePlan(Long id) {
        TripPlan plan = getOwnedPlan(id);
        tripPlanItemMapper.delete(new LambdaQueryWrapper<TripPlanItem>()
                .eq(TripPlanItem::getPlanId, plan.getId()));
        tripPlanMapper.deleteById(plan.getId());
    }

    @Override
    @Transactional
    public TripPlanItemVO createPlanItem(Long planId, TripPlanItemCreateRequest request) {
        TripPlan plan = getOwnedPlan(planId);
        validateDayNo(plan, request.getDayNo());
        ensureDayNoUnique(plan.getId(), request.getDayNo(), null);

        TripPlanItem item = new TripPlanItem();
        item.setPlanId(plan.getId());
        fillPlanItem(item, request.getDayNo(), request.getDestination(), request.getHotel(), request.getTransportType(), request.getRemark());
        tripPlanItemMapper.insert(item);
        return toItemVO(item);
    }

    @Override
    @Transactional
    public TripPlanItemVO updatePlanItem(Long planId, Long itemId, TripPlanItemUpdateRequest request) {
        TripPlan plan = getOwnedPlan(planId);
        TripPlanItem item = getOwnedPlanItem(plan.getId(), itemId);
        validateDayNo(plan, request.getDayNo());
        ensureDayNoUnique(plan.getId(), request.getDayNo(), item.getId());

        fillPlanItem(item, request.getDayNo(), request.getDestination(), request.getHotel(), request.getTransportType(), request.getRemark());
        tripPlanItemMapper.updateById(item);
        return toItemVO(item);
    }

    @Override
    @Transactional
    public void deletePlanItem(Long planId, Long itemId) {
        TripPlan plan = getOwnedPlan(planId);
        TripPlanItem item = getOwnedPlanItem(plan.getId(), itemId);
        tripPlanItemMapper.deleteById(item.getId());
    }

    private TripPlan getOwnedPlan(Long planId) {
        Long userId = SecurityUtils.getCurrentUserId();
        TripPlan plan = tripPlanMapper.selectById(planId);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "行程计划不存在");
        }
        return plan;
    }

    private TripPlanItem getOwnedPlanItem(Long planId, Long itemId) {
        TripPlanItem item = tripPlanItemMapper.selectById(itemId);
        if (item == null || !planId.equals(item.getPlanId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "每日安排不存在");
        }
        return item;
    }

    private void validateDayNo(TripPlan plan, Integer dayNo) {
        if (dayNo == null || dayNo < 1 || dayNo > plan.getTotalDays()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "每日安排天数必须在计划总天数范围内");
        }
    }

    private void ensureDayNoUnique(Long planId, Integer dayNo, Long excludeItemId) {
        List<TripPlanItem> items = tripPlanItemMapper.selectList(new LambdaQueryWrapper<TripPlanItem>()
                .eq(TripPlanItem::getPlanId, planId)
                .eq(TripPlanItem::getDayNo, dayNo));
        boolean exists = items.stream().anyMatch(item -> !item.getId().equals(excludeItemId));
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该天的行程安排已存在，请改为编辑原安排");
        }
    }

    private int getMaxDayNo(Long planId) {
        return listPlanItems(planId).stream()
                .map(TripPlanItemVO::getDayNo)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private List<TripPlanItemVO> listPlanItems(Long planId) {
        return tripPlanItemMapper.selectList(new LambdaQueryWrapper<TripPlanItem>()
                        .eq(TripPlanItem::getPlanId, planId)
                        .orderByAsc(TripPlanItem::getDayNo)
                        .orderByAsc(TripPlanItem::getId))
                .stream()
                .map(this::toItemVO)
                .toList();
    }

    private void fillPlan(TripPlan plan, String planName, Integer totalDays, java.time.LocalDate startDate, String remark) {
        plan.setPlanName(planName);
        plan.setTotalDays(totalDays);
        plan.setStartDate(startDate);
        plan.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
    }

    private void fillPlanItem(TripPlanItem item, Integer dayNo, String destination, String hotel, String transportType, String remark) {
        item.setDayNo(dayNo);
        item.setDestination(destination);
        item.setHotel(StringUtils.hasText(hotel) ? hotel.trim() : null);
        item.setTransportType(StringUtils.hasText(transportType) ? transportType.trim() : null);
        item.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
    }

    private TripPlanListItemVO toListItemVO(TripPlan plan, Map<Long, Long> itemCountMap) {
        TripPlanListItemVO vo = new TripPlanListItemVO();
        vo.setId(plan.getId());
        vo.setPlanName(plan.getPlanName());
        vo.setTotalDays(plan.getTotalDays());
        vo.setStartDate(plan.getStartDate());
        vo.setRemark(plan.getRemark());
        vo.setSourceType(plan.getSourceType());
        vo.setItemCount(itemCountMap.getOrDefault(plan.getId(), 0L).intValue());
        vo.setCreateTime(plan.getCreateTime());
        return vo;
    }

    private TripPlanDetailVO buildDetailVO(TripPlan plan, List<TripPlanItemVO> items) {
        TripPlanDetailVO vo = new TripPlanDetailVO();
        vo.setId(plan.getId());
        vo.setPlanName(plan.getPlanName());
        vo.setTotalDays(plan.getTotalDays());
        vo.setStartDate(plan.getStartDate());
        vo.setRemark(plan.getRemark());
        vo.setSourceType(plan.getSourceType());
        vo.setCreateTime(plan.getCreateTime());
        vo.setItems(items);
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
}
