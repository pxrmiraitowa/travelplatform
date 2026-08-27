package com.travelplatform.contenttrip.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.dto.review.ReviewCreateRequest;
import com.travelplatform.contenttrip.service.review.ReviewService;
import com.travelplatform.contenttrip.vo.review.ReviewVO;
import com.travelplatform.contenttrip.vo.review.ReviewableOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "提交订单评价")
    @PostMapping("/reviews")
    public Result<ReviewVO> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        return Result.success(reviewService.createReview(request));
    }

    @Operation(summary = "可评价订单列表")
    @GetMapping("/orders/reviewable")
    public Result<PageResult<ReviewableOrderVO>> listReviewableOrders(@RequestParam(required = false) Integer pageNum,
                                                                      @RequestParam(required = false) Integer pageSize) {
        return Result.success(reviewService.listReviewableOrders(pageNum, pageSize));
    }

    @Operation(summary = "查看当前用户订单评价")
    @GetMapping("/orders/{id}/review")
    public Result<ReviewVO> getCurrentUserOrderReview(@PathVariable Long id) {
        return Result.success(reviewService.getCurrentUserOrderReview(id));
    }
}
