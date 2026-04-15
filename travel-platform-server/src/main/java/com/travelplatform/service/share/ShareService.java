package com.travelplatform.service.share;

import com.travelplatform.dto.share.SharePostCreateRequest;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.share.SharePostDetailVO;
import com.travelplatform.vo.share.SharePostListItemVO;
import org.springframework.web.multipart.MultipartFile;

public interface ShareService {

    String uploadShareImage(MultipartFile file);

    Long createSharePost(SharePostCreateRequest request);

    PageResult<SharePostListItemVO> listPublicShares(Integer pageNum, Integer pageSize);

    PageResult<SharePostListItemVO> listCurrentUserShares(Integer pageNum, Integer pageSize);

    SharePostDetailVO getPublicShareDetail(Long id);
}
