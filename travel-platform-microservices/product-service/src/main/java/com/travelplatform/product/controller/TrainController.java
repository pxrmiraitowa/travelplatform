package com.travelplatform.product.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.product.dto.train.TrainQueryRequest;
import com.travelplatform.product.service.train.TrainService;
import com.travelplatform.product.vo.train.TrainDetailVO;
import com.travelplatform.product.vo.train.TrainListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/trains")
public class TrainController {

    private final TrainService trainService;

    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    @Operation(summary = "搜索车次")
    @GetMapping
    public Result<PageResult<TrainListItemVO>> searchTrains(TrainQueryRequest request) {
        return Result.success(trainService.searchTrains(request));
    }

    @Operation(summary = "查询车次详情")
    @GetMapping("/{id}")
    public Result<TrainDetailVO> getTrainDetail(@PathVariable Long id) {
        return Result.success(trainService.getTrainDetail(id));
    }
}
