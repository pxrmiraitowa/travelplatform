package com.travelplatform.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.dto.usercontact.UserContactCreateRequest;
import com.travelplatform.dto.usercontact.UserContactUpdateRequest;
import com.travelplatform.service.usercontact.UserContactService;
import com.travelplatform.vo.usercontact.UserContactVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user-contacts")
public class UserContactController {

    private final UserContactService userContactService;

    public UserContactController(UserContactService userContactService) {
        this.userContactService = userContactService;
    }

    @Operation(summary = "查询当前用户常用出行人")
    @GetMapping
    public Result<List<UserContactVO>> listContacts() {
        return Result.success(userContactService.listCurrentUserContacts());
    }

    @Operation(summary = "新增常用出行人")
    @PostMapping
    public Result<UserContactVO> createContact(@Valid @RequestBody UserContactCreateRequest request) {
        return Result.success(userContactService.createContact(request));
    }

    @Operation(summary = "编辑常用出行人")
    @PutMapping("/{id}")
    public Result<UserContactVO> updateContact(@PathVariable Long id,
                                               @Valid @RequestBody UserContactUpdateRequest request) {
        return Result.success(userContactService.updateContact(id, request));
    }

    @Operation(summary = "删除常用出行人")
    @DeleteMapping("/{id}")
    public Result<Void> deleteContact(@PathVariable Long id) {
        userContactService.deleteContact(id);
        return Result.success();
    }
}
