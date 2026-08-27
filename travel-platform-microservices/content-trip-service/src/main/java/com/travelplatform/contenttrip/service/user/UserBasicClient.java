package com.travelplatform.contenttrip.service.user;

import java.util.Collection;
import java.util.Map;

public interface UserBasicClient {

    Map<Long, UserBasicInfo> listBasicUsers(Collection<Long> userIds);
}
