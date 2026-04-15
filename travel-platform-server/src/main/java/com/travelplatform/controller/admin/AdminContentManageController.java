package com.travelplatform.controller.admin;

import com.travelplatform.common.result.Result;
import com.travelplatform.service.admin.AdminContentManageService;
import com.travelplatform.vo.admin.content.AdminReviewListItemVO;
import com.travelplatform.vo.admin.content.AdminShareListItemVO;
import com.travelplatform.vo.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminContentManageController {

    private final AdminContentManageService adminContentManageService;

    public AdminContentManageController(AdminContentManageService adminContentManageService) {
        this.adminContentManageService = adminContentManageService;
    }

    @Operation(summary = "后台分享列表")
    @GetMapping("/shares")
    public Result<PageResult<AdminShareListItemVO>> listShares(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Integer status,
                                                               @RequestParam(required = false) Integer pageNum,
                                                               @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminContentManageService.listShares(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "后台删除分享")
    @DeleteMapping("/shares/{id}")
    public Result<Void> deleteShare(@PathVariable Long id) {
        adminContentManageService.deleteShare(id);
        return Result.success();
    }

    @Operation(summary = "后台评价列表")
    @GetMapping("/reviews")
    public Result<PageResult<AdminReviewListItemVO>> listReviews(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String bizType,
                                                                 @RequestParam(required = false) Integer status,
                                                                 @RequestParam(required = false) Integer pageNum,
                                                                 @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminContentManageService.listReviews(keyword, bizType, status, pageNum, pageSize));
    }

    @Operation(summary = "后台删除评价")
    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        adminContentManageService.deleteReview(id);
        return Result.success();
    }
}
