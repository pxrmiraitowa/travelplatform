package com.travelplatform.order.vo;

import java.time.LocalDate;

public record ReviewContextVO(Long orderId, String orderNo, Long userId, String bizType,
                              Long bizId, LocalDate travelDate, String summaryTitle,
                              String summarySubtitle, boolean completed) {
}
