package com.travelplatform.contenttrip.controller.admin;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.service.admin.AdminContentManageService;
import com.travelplatform.contenttrip.vo.admin.AdminReviewListItemVO;
import com.travelplatform.contenttrip.vo.admin.AdminShareListItemVO;
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

    @GetMapping("/shares")
    public Result<PageResult<AdminShareListItemVO>> listShares(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Integer status,
                                                               @RequestParam(required = false) Integer pageNum,
                                                               @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminContentManageService.listShares(keyword, status, pageNum, pageSize));
    }

    @DeleteMapping("/shares/{id}")
    public Result<Void> deleteShare(@PathVariable Long id) {
        adminContentManageService.deleteShare(id);
        return Result.success();
    }

    @GetMapping("/reviews")
    public Result<PageResult<AdminReviewListItemVO>> listReviews(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String bizType,
                                                                 @RequestParam(required = false) Integer status,
                                                                 @RequestParam(required = false) Integer pageNum,
                                                                 @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminContentManageService.listReviews(keyword, bizType, status, pageNum, pageSize));
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        adminContentManageService.deleteReview(id);
        return Result.success();
    }
}
