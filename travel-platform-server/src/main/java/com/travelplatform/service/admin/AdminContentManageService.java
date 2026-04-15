package com.travelplatform.service.admin;

import com.travelplatform.vo.admin.content.AdminReviewListItemVO;
import com.travelplatform.vo.admin.content.AdminShareListItemVO;
import com.travelplatform.vo.common.PageResult;

public interface AdminContentManageService {

    PageResult<AdminShareListItemVO> listShares(String keyword, Integer status, Integer pageNum, Integer pageSize);

    void deleteShare(Long id);

    PageResult<AdminReviewListItemVO> listReviews(String keyword, String bizType, Integer status, Integer pageNum, Integer pageSize);

    void deleteReview(Long id);
}
