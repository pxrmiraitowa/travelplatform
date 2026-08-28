package com.travelplatform.product.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.product.dto.internal.StockChangeRequest;
import com.travelplatform.product.service.snapshot.ProductSnapshotService;
import com.travelplatform.product.service.stock.ProductStockService;
import com.travelplatform.product.vo.snapshot.ProductSnapshotVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/products")
public class InternalProductController {

    private final ProductSnapshotService productSnapshotService;
    private final ProductStockService productStockService;

    public InternalProductController(ProductSnapshotService productSnapshotService,
                                     ProductStockService productStockService) {
        this.productSnapshotService = productSnapshotService;
        this.productStockService = productStockService;
    }

    @Operation(summary = "商品下单快照")
    @GetMapping("/snapshot")
    public Result<ProductSnapshotVO> snapshot(@RequestParam String productType,
                                              @RequestParam Long productId,
                                              @RequestParam(required = false) Long variantId,
                                              @RequestParam(required = false) String variantName) {
        return Result.success(productSnapshotService.getSnapshot(productType, productId, variantId, variantName));
    }

    @PostMapping("/stock/deduct")
    public Result<Void> deduct(@jakarta.validation.Valid @RequestBody StockChangeRequest request) {
        productStockService.deduct(request);
        return Result.success();
    }

    @PostMapping("/stock/restore")
    public Result<Void> restore(@jakarta.validation.Valid @RequestBody StockChangeRequest request) {
        productStockService.restore(request);
        return Result.success();
    }
}
