package com.travelplatform.contenttrip.service.admin;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.vo.admin.AdminReviewListItemVO;
import com.travelplatform.contenttrip.vo.admin.AdminShareListItemVO;

public interface AdminContentManageService {

    PageResult<AdminShareListItemVO> listShares(String keyword, Integer status, Integer pageNum, Integer pageSize);

    void deleteShare(Long id);

    PageResult<AdminReviewListItemVO> listReviews(String keyword, String bizType, Integer status, Integer pageNum, Integer pageSize);

    void deleteReview(Long id);
}
