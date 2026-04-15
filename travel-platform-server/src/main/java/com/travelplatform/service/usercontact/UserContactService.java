package com.travelplatform.service.usercontact;

import com.travelplatform.dto.usercontact.UserContactCreateRequest;
import com.travelplatform.dto.usercontact.UserContactUpdateRequest;
import com.travelplatform.vo.usercontact.UserContactVO;

import java.util.List;

public interface UserContactService {

    List<UserContactVO> listCurrentUserContacts();

    UserContactVO createContact(UserContactCreateRequest request);

    UserContactVO updateContact(Long id, UserContactUpdateRequest request);

    void deleteContact(Long id);
}
