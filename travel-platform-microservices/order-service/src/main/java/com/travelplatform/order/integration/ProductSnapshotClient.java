package com.travelplatform.order.integration;

import com.travelplatform.order.dto.OrderCreateRequest;

public interface ProductSnapshotClient {
    ProductSnapshot getSnapshot(OrderCreateRequest request);
}
