package com.travelplatform.contenttrip.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.service.share.ShareService;
import com.travelplatform.contenttrip.vo.share.SharePostDetailVO;
import com.travelplatform.contenttrip.vo.share.SharePostListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/shares")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @Operation(summary = "公开分享列表")
    @GetMapping
    public Result<PageResult<SharePostListItemVO>> listPublicShares(@RequestParam(required = false) Integer pageNum,
                                                                    @RequestParam(required = false) Integer pageSize) {
        return Result.success(shareService.listPublicShares(pageNum, pageSize));
    }

    @Operation(summary = "公开分享详情")
    @GetMapping("/{id}")
    public Result<SharePostDetailVO> getPublicShareDetail(@PathVariable Long id) {
        return Result.success(shareService.getPublicShareDetail(id));
    }
}
