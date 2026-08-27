package com.travelplatform.contenttrip.service.share;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.contenttrip.dto.share.SharePostCreateRequest;
import com.travelplatform.contenttrip.vo.share.SharePostDetailVO;
import com.travelplatform.contenttrip.vo.share.SharePostListItemVO;
import org.springframework.web.multipart.MultipartFile;

public interface ShareService {

    String uploadShareImage(MultipartFile file);

    Long createSharePost(SharePostCreateRequest request);

    PageResult<SharePostListItemVO> listPublicShares(Integer pageNum, Integer pageSize);

    PageResult<SharePostListItemVO> listCurrentUserShares(Integer pageNum, Integer pageSize);

    SharePostDetailVO getPublicShareDetail(Long id);
}
