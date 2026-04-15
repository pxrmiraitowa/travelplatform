package com.travelplatform.service.user;

import com.travelplatform.dto.user.UpdateUserProfileRequest;
import com.travelplatform.vo.user.CurrentUserVO;

public interface UserService {

    CurrentUserVO getCurrentUser();

    CurrentUserVO updateCurrentUser(UpdateUserProfileRequest request);

    CurrentUserVO buildCurrentUserVO(Long userId);
}
