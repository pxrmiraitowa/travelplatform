package com.travelplatform.product.service.snapshot;

import com.travelplatform.product.vo.snapshot.ProductSnapshotVO;

public interface ProductSnapshotService {

    ProductSnapshotVO getSnapshot(String productType, Long productId, Long variantId, String variantName);
}
