package com.travelplatform.user.service;

import com.travelplatform.user.dto.contact.UserContactRequest;
import com.travelplatform.user.vo.UserContactVO;
import java.util.List;

public interface UserContactService {
    List<UserContactVO> list();
    UserContactVO create(UserContactRequest request);
    UserContactVO update(Long id, UserContactRequest request);
    void delete(Long id);
}
