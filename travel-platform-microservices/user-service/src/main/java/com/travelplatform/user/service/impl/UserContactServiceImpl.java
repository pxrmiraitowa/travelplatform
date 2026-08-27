package com.travelplatform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.user.dto.contact.UserContactRequest;
import com.travelplatform.user.entity.UserContact;
import com.travelplatform.user.mapper.UserContactMapper;
import com.travelplatform.user.security.SecurityUtils;
import com.travelplatform.user.service.UserContactService;
import com.travelplatform.user.vo.UserContactVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserContactServiceImpl implements UserContactService {
    private final UserContactMapper mapper;
    public UserContactServiceImpl(UserContactMapper mapper) { this.mapper = mapper; }

    @Override
    public List<UserContactVO> list() {
        return mapper.selectList(new LambdaQueryWrapper<UserContact>()
                        .eq(UserContact::getUserId, SecurityUtils.getCurrentUserId())
                        .orderByDesc(UserContact::getIsDefault).orderByDesc(UserContact::getId))
                .stream().map(this::toVO).toList();
    }
    @Override
    @Transactional
    public UserContactVO create(UserContactRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (request.getIsDefault() != null && request.getIsDefault() == 1) clearDefault(userId);
        UserContact contact = new UserContact(); contact.setUserId(userId); fill(contact, request);
        mapper.insert(contact); return toVO(contact);
    }
    @Override
    @Transactional
    public UserContactVO update(Long id, UserContactRequest request) {
        Long userId = SecurityUtils.getCurrentUserId(); UserContact contact = owned(id, userId);
        if (request.getIsDefault() != null && request.getIsDefault() == 1) clearDefault(userId);
        fill(contact, request); mapper.updateById(contact); return toVO(contact);
    }
    @Override public void delete(Long id) { mapper.deleteById(owned(id, SecurityUtils.getCurrentUserId())); }

    private UserContact owned(Long id, Long userId) {
        UserContact contact = mapper.selectById(id);
        if (contact == null || !userId.equals(contact.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "常用出行人不存在");
        }
        return contact;
    }
    private void clearDefault(Long userId) {
        mapper.selectList(new LambdaQueryWrapper<UserContact>().eq(UserContact::getUserId, userId)
                .eq(UserContact::getIsDefault, 1)).forEach(contact -> {
                    contact.setIsDefault(0); mapper.updateById(contact);
                });
    }
    private void fill(UserContact contact, UserContactRequest request) {
        contact.setName(request.getName().trim()); contact.setPhone(request.getPhone().trim());
        contact.setIdCard(request.getIdCard().trim());
        contact.setContactType(request.getContactType() == null ? 1 : request.getContactType());
        contact.setIsDefault(request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
        contact.setRemark(request.getRemark());
    }
    private UserContactVO toVO(UserContact value) {
        return new UserContactVO(value.getId(), value.getName(), value.getPhone(), value.getIdCard(),
                value.getContactType(), value.getIsDefault(), value.getRemark());
    }
}
