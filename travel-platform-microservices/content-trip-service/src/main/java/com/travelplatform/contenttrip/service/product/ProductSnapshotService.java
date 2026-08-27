package com.travelplatform.contenttrip.service.product;

public interface ProductSnapshotService {

    ProductSnapshot getProductSnapshot(String productType, Long productId);

    String normalizeProductType(String productType);
}
