package com.travelplatform.user.vo;

public record UserContactVO(Long id, String name, String phone, String idCard,
                            Integer contactType, Integer isDefault, String remark) {
}
