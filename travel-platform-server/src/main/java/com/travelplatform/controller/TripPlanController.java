package com.travelplatform.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.dto.tripplan.AiTripPlanPreviewRequest;
import com.travelplatform.dto.tripplan.AiTripPlanSaveRequest;
import com.travelplatform.dto.tripplan.TripPlanCreateRequest;
import com.travelplatform.dto.tripplan.TripPlanItemCreateRequest;
import com.travelplatform.dto.tripplan.TripPlanItemUpdateRequest;
import com.travelplatform.dto.tripplan.TripPlanUpdateRequest;
import com.travelplatform.service.tripplan.AiTripPlanService;
import com.travelplatform.service.tripplan.TripPlanService;
import com.travelplatform.vo.tripplan.AiTripPlanPreviewVO;
import com.travelplatform.vo.tripplan.TripPlanDetailVO;
import com.travelplatform.vo.tripplan.TripPlanItemVO;
import com.travelplatform.vo.tripplan.TripPlanListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trip-plans")
public class TripPlanController {

    private final TripPlanService tripPlanService;
    private final AiTripPlanService aiTripPlanService;

    public TripPlanController(TripPlanService tripPlanService, AiTripPlanService aiTripPlanService) {
        this.tripPlanService = tripPlanService;
        this.aiTripPlanService = aiTripPlanService;
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

    @Operation(summary = "AI 生成行程预览")
    @PostMapping("/ai-preview")
    public Result<AiTripPlanPreviewVO> previewAiPlan(@Valid @RequestBody AiTripPlanPreviewRequest request) {
        return Result.success(aiTripPlanService.buildPreview(request));
    }

    @Operation(summary = "保存 AI 行程到我的规划")
    @PostMapping("/ai-save")
    public Result<TripPlanDetailVO> saveAiPlan(@Valid @RequestBody AiTripPlanSaveRequest request) {
        return Result.success(aiTripPlanService.savePlan(request));
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
