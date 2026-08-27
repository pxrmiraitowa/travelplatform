package com.travelplatform.contenttrip.service.share;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.vo.share.SharePostDetailVO;
import com.travelplatform.contenttrip.vo.share.SharePostListItemVO;

public interface ShareService {

    PageResult<SharePostListItemVO> listPublicShares(Integer pageNum, Integer pageSize);

    SharePostDetailVO getPublicShareDetail(Long id);
}
