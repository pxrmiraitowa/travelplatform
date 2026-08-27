package com.travelplatform.contenttrip.service.review;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.dto.review.ReviewCreateRequest;
import com.travelplatform.contenttrip.vo.review.ReviewVO;
import com.travelplatform.contenttrip.vo.review.ReviewableOrderVO;

public interface ReviewService {

    ReviewVO createReview(ReviewCreateRequest request);

    PageResult<ReviewableOrderVO> listReviewableOrders(Integer pageNum, Integer pageSize);

    ReviewVO getCurrentUserOrderReview(Long orderId);

    ReviewVO getVisibleOrderReview(Long orderId);
}
