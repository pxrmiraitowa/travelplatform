package com.travelplatform.product.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.product.service.tour.TourService;
import com.travelplatform.product.vo.tour.TourDetailVO;
import com.travelplatform.product.vo.tour.TourListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/tours")
public class TourController {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @Operation(summary = "查询旅游产品列表")
    @GetMapping
    public Result<PageResult<TourListItemVO>> listTours(@RequestParam(required = false) String destination,
                                                        @RequestParam(required = false) Integer pageNum,
                                                        @RequestParam(required = false) Integer pageSize) {
        return Result.success(tourService.listTours(destination, pageNum, pageSize));
    }

    @Operation(summary = "查询旅游产品详情")
    @GetMapping("/{id}")
    public Result<TourDetailVO> getTourDetail(@PathVariable Long id) {
        return Result.success(tourService.getTourDetail(id));
    }
}
