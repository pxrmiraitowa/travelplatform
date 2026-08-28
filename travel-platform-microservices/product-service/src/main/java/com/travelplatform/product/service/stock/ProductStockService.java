package com.travelplatform.product.service.stock;

import com.travelplatform.product.dto.internal.StockChangeRequest;

public interface ProductStockService {
    void deduct(StockChangeRequest request);
    void restore(StockChangeRequest request);
}
