package com.travelplatform.service.review;

import com.travelplatform.dto.review.ReviewCreateRequest;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.review.ReviewVO;
import com.travelplatform.vo.review.ReviewableOrderVO;

public interface ReviewService {

    ReviewVO createReview(ReviewCreateRequest request);

    PageResult<ReviewableOrderVO> listReviewableOrders(Integer pageNum, Integer pageSize);

    ReviewVO getCurrentUserOrderReview(Long orderId);

    ReviewVO getVisibleOrderReview(Long orderId);
}
