package com.travelplatform.product.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.product.service.attraction.AttractionCatalogService;
import com.travelplatform.product.vo.attraction.AttractionSnapshotVO;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/attractions")
public class InternalAttractionController {
    private final AttractionCatalogService attractionCatalogService;

    public InternalAttractionController(AttractionCatalogService attractionCatalogService) {
        this.attractionCatalogService = attractionCatalogService;
    }

    @GetMapping
    public Result<List<AttractionSnapshotVO>> list(@RequestParam(required = false) String city,
                                                   @RequestParam(required = false) String ids) {
        List<Long> parsedIds = StringUtils.hasText(ids)
                ? Arrays.stream(ids.split(",")).map(String::trim).filter(StringUtils::hasText).map(Long::valueOf).toList()
                : List.of();
        return Result.success(attractionCatalogService.list(city, parsedIds));
    }
}
