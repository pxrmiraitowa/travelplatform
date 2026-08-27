package com.travelplatform.contenttrip.service.order;

import com.travelplatform.common.vo.PageResult;

public interface OrderReviewClient {

    OrderReviewContext getReviewContext(Long orderId, Long userId);

    PageResult<OrderReviewContext> listReviewableOrders(Long userId, int pageNum, int pageSize);
}
