package com.travelplatform.contenttrip.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.contenttrip.dto.tripplan.TripPlanCreateRequest;
import com.travelplatform.contenttrip.dto.tripplan.TripPlanItemCreateRequest;
import com.travelplatform.contenttrip.dto.tripplan.TripPlanItemUpdateRequest;
import com.travelplatform.contenttrip.dto.tripplan.TripPlanUpdateRequest;
import com.travelplatform.contenttrip.service.tripplan.TripPlanService;
import com.travelplatform.contenttrip.vo.tripplan.TripPlanDetailVO;
import com.travelplatform.contenttrip.vo.tripplan.TripPlanItemVO;
import com.travelplatform.contenttrip.vo.tripplan.TripPlanListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trip-plans")
public class TripPlanController {

    private final TripPlanService tripPlanService;

    public TripPlanController(TripPlanService tripPlanService) {
        this.tripPlanService = tripPlanService;
    }

    @Operation(summary = "查询当前用户行程计划列表")
    @GetMapping
    public Result<List<TripPlanListItemVO>> listPlans() {
        return Result.success(tripPlanService.listCurrentUserPlans());
    }

    @Operation(summary = "创建行程计划")
    @PostMapping
    public Result<TripPlanDetailVO> createPlan(@Valid @RequestBody TripPlanCreateRequest request) {
        return Result.success(tripPlanService.createPlan(request));
    }

    @Operation(summary = "查询行程计划详情")
    @GetMapping("/{id}")
    public Result<TripPlanDetailVO> getPlanDetail(@PathVariable Long id) {
        return Result.success(tripPlanService.getCurrentUserPlanDetail(id));
    }

    @Operation(summary = "更新行程计划")
    @PutMapping("/{id}")
    public Result<TripPlanDetailVO> updatePlan(@PathVariable Long id,
                                               @Valid @RequestBody TripPlanUpdateRequest request) {
        return Result.success(tripPlanService.updatePlan(id, request));
    }

    @Operation(summary = "删除行程计划")
    @DeleteMapping("/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        tripPlanService.deletePlan(id);
        return Result.success();
    }

    @Operation(summary = "新增每日安排")
    @PostMapping("/{id}/items")
    public Result<TripPlanItemVO> createPlanItem(@PathVariable Long id,
                                                 @Valid @RequestBody TripPlanItemCreateRequest request) {
        return Result.success(tripPlanService.createPlanItem(id, request));
    }

    @Operation(summary = "更新每日安排")
    @PutMapping("/{planId}/items/{itemId}")
    public Result<TripPlanItemVO> updatePlanItem(@PathVariable Long planId,
                                                 @PathVariable Long itemId,
                                                 @Valid @RequestBody TripPlanItemUpdateRequest request) {
        return Result.success(tripPlanService.updatePlanItem(planId, itemId, request));
    }

    @Operation(summary = "删除每日安排")
    @DeleteMapping("/{planId}/items/{itemId}")
    public Result<Void> deletePlanItem(@PathVariable Long planId, @PathVariable Long itemId) {
        tripPlanService.deletePlanItem(planId, itemId);
        return Result.success();
    }
}
