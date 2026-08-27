package com.travelplatform.user.service;

import com.travelplatform.user.dto.user.UpdateUserProfileRequest;
import com.travelplatform.user.vo.BasicUserVO;
import com.travelplatform.user.vo.CurrentUserVO;
import java.util.List;

public interface UserService {

    CurrentUserVO getCurrentUser();

    CurrentUserVO updateCurrentUser(UpdateUserProfileRequest request);

    CurrentUserVO buildCurrentUserVO(Long userId);

    List<BasicUserVO> listBasicUsers(List<Long> userIds);
}
