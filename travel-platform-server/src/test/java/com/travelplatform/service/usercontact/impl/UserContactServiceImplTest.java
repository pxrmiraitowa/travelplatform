package com.travelplatform.service.usercontact.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.dto.usercontact.UserContactCreateRequest;
import com.travelplatform.dto.usercontact.UserContactUpdateRequest;
import com.travelplatform.entity.UserContact;
import com.travelplatform.mapper.UserContactMapper;
import com.travelplatform.security.SecurityUtils;
import com.travelplatform.vo.usercontact.UserContactVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserContactServiceImplTest {
    @Mock UserContactMapper mapper;
    @InjectMocks UserContactServiceImpl service;

    @Test
    void listCurrentUserContactsShouldConvertRecords() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            UserContact contact = contact(1L, 1L, "Tom", 1);
            when(mapper.selectList(any())).thenReturn(List.of(contact));

            List<UserContactVO> result = service.listCurrentUserContacts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Tom");
        }
    }

    @Test
    void createContactShouldClearExistingDefaultWhenNeeded() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(mapper.selectList(any())).thenReturn(List.of(contact(9L, 1L, "Old", 1)));
            UserContactCreateRequest request = new UserContactCreateRequest();
            request.setName("New");
            request.setPhone("13900000000");
            request.setIdCard("110101199001011234");
            request.setIsDefault(1);

            UserContactVO result = service.createContact(request);

            verify(mapper).updateById(any(UserContact.class));
            verify(mapper).insert(any(UserContact.class));
            assertThat(result.getIsDefault()).isEqualTo(1);
        }
    }

    @Test
    void deleteContactShouldRejectUnownedRecord() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(mapper.selectById(3L)).thenReturn(contact(3L, 2L, "Other", 0));

            assertThatThrownBy(() -> service.deleteContact(3L)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void updateContactShouldSaveCurrentRecord() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            UserContact stored = contact(3L, 1L, "Old", 0);
            when(mapper.selectById(3L)).thenReturn(stored);
            UserContactUpdateRequest request = new UserContactUpdateRequest();
            request.setName("Updated");
            request.setPhone("13900000001");
            request.setIdCard("110101199001011234");
            request.setIsDefault(0);

            UserContactVO result = service.updateContact(3L, request);

            verify(mapper).updateById(stored);
            assertThat(result.getName()).isEqualTo("Updated");
        }
    }

    private UserContact contact(Long id, Long userId, String name, Integer isDefault) {
        UserContact contact = new UserContact();
        contact.setId(id);
        contact.setUserId(userId);
        contact.setName(name);
        contact.setPhone("13900000000");
        contact.setIdCard("110101199001011234");
        contact.setContactType(1);
        contact.setIsDefault(isDefault);
        return contact;
    }
}
