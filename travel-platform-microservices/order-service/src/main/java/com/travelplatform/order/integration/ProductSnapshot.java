package com.travelplatform.order.integration;

import java.math.BigDecimal;

public record ProductSnapshot(Long productId, String productName, String summary,
                              BigDecimal price, boolean available, Integer stock) {
}
