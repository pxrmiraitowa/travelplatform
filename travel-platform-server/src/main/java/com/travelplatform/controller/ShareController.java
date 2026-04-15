package com.travelplatform.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.dto.share.SharePostCreateRequest;
import com.travelplatform.service.share.ShareService;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.share.SharePostDetailVO;
import com.travelplatform.vo.share.SharePostListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @Operation(summary = "上传分享图片")
    @PostMapping("/api/shares/upload")
    public Result<Map<String, String>> uploadShareImage(@RequestParam("file") MultipartFile file) {
        return Result.success(Map.of("url", shareService.uploadShareImage(file)));
    }

    @Operation(summary = "发布分享")
    @PostMapping("/api/shares")
    public Result<Map<String, Long>> createShare(@Valid @RequestBody SharePostCreateRequest request) {
        return Result.success(Map.of("id", shareService.createSharePost(request)));
    }

    @Operation(summary = "我的分享列表")
    @GetMapping("/api/shares/mine")
    public Result<PageResult<SharePostListItemVO>> listCurrentUserShares(@RequestParam(required = false) Integer pageNum,
                                                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(shareService.listCurrentUserShares(pageNum, pageSize));
    }

    @Operation(summary = "公开分享列表")
    @GetMapping("/api/public/shares")
    public Result<PageResult<SharePostListItemVO>> listPublicShares(@RequestParam(required = false) Integer pageNum,
                                                                    @RequestParam(required = false) Integer pageSize) {
        return Result.success(shareService.listPublicShares(pageNum, pageSize));
    }

    @Operation(summary = "公开分享详情")
    @GetMapping("/api/public/shares/{id}")
    public Result<SharePostDetailVO> getPublicShareDetail(@PathVariable Long id) {
        return Result.success(shareService.getPublicShareDetail(id));
    }
}
