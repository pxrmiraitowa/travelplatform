package com.travelplatform.user.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.user.dto.contact.UserContactRequest;
import com.travelplatform.user.service.UserContactService;
import com.travelplatform.user.vo.UserContactVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-contacts")
public class UserContactController {
    private final UserContactService service;
    public UserContactController(UserContactService service) { this.service = service; }
    @GetMapping public Result<List<UserContactVO>> list() { return Result.success(service.list()); }
    @PostMapping public Result<UserContactVO> create(@Valid @RequestBody UserContactRequest request) {
        return Result.success(service.create(request));
    }
    @PutMapping("/{id}") public Result<UserContactVO> update(@PathVariable Long id,
                                                             @Valid @RequestBody UserContactRequest request) {
        return Result.success(service.update(id, request));
    }
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) {
        service.delete(id); return Result.success();
    }
}
