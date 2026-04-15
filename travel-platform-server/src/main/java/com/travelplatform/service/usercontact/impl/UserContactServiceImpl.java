package com.travelplatform.service.usercontact.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.dto.usercontact.UserContactCreateRequest;
import com.travelplatform.dto.usercontact.UserContactUpdateRequest;
import com.travelplatform.entity.UserContact;
import com.travelplatform.mapper.UserContactMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.service.usercontact.UserContactService;
import com.travelplatform.vo.usercontact.UserContactVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserContactServiceImpl implements UserContactService {

    private final UserContactMapper userContactMapper;

    public UserContactServiceImpl(UserContactMapper userContactMapper) {
        this.userContactMapper = userContactMapper;
    }

    @Override
    public List<UserContactVO> listCurrentUserContacts() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userContactMapper.selectList(new LambdaQueryWrapper<UserContact>()
                        .eq(UserContact::getUserId, userId)
                        .orderByDesc(UserContact::getIsDefault)
                        .orderByDesc(UserContact::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public UserContactVO createContact(UserContactCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (Integer.valueOf(1).equals(request.getIsDefault())) {
            clearDefault(userId);
        }
        UserContact contact = new UserContact();
        contact.setUserId(userId);
        fillContact(contact, request.getName(), request.getPhone(), request.getIdCard(), request.getContactType(), request.getIsDefault(), request.getRemark());
        userContactMapper.insert(contact);
        return toVO(contact);
    }

    @Override
    @Transactional
    public UserContactVO updateContact(Long id, UserContactUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserContact contact = getOwnedContact(id, userId);
        if (Integer.valueOf(1).equals(request.getIsDefault())) {
            clearDefault(userId);
        }
        fillContact(contact, request.getName(), request.getPhone(), request.getIdCard(), request.getContactType(), request.getIsDefault(), request.getRemark());
        userContactMapper.updateById(contact);
        return toVO(contact);
    }

    @Override
    public void deleteContact(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserContact contact = getOwnedContact(id, userId);
        userContactMapper.deleteById(contact.getId());
    }

    private void clearDefault(Long userId) {
        List<UserContact> contacts = userContactMapper.selectList(new LambdaQueryWrapper<UserContact>()
                .eq(UserContact::getUserId, userId)
                .eq(UserContact::getIsDefault, 1));
        for (UserContact item : contacts) {
            item.setIsDefault(0);
            userContactMapper.updateById(item);
        }
    }

    private UserContact getOwnedContact(Long id, Long userId) {
        UserContact contact = userContactMapper.selectById(id);
        if (contact == null || !userId.equals(contact.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "常用出行人不存在");
        }
        return contact;
    }

    private void fillContact(UserContact contact,
                             String name,
                             String phone,
                             String idCard,
                             Integer contactType,
                             Integer isDefault,
                             String remark) {
        contact.setName(name);
        contact.setPhone(phone);
        contact.setIdCard(idCard);
        contact.setContactType(contactType == null ? 1 : contactType);
        contact.setIsDefault(Integer.valueOf(1).equals(isDefault) ? 1 : 0);
        contact.setRemark(remark);
    }

    private UserContactVO toVO(UserContact contact) {
        UserContactVO vo = new UserContactVO();
        vo.setId(contact.getId());
        vo.setName(contact.getName());
        vo.setPhone(contact.getPhone());
        vo.setIdCard(contact.getIdCard());
        vo.setContactType(contact.getContactType());
        vo.setIsDefault(contact.getIsDefault());
        vo.setRemark(contact.getRemark());
        return vo;
    }
}
