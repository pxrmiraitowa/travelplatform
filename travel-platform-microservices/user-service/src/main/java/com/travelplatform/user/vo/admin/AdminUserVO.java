package com.travelplatform.user.vo.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserVO(Long id, String username, String nickname, String realName,
                          String phone, String email, Integer gender, String avatar,
                          Integer status, List<String> roleCodes, LocalDateTime lastLoginTime,
                          LocalDateTime createTime) {
}
