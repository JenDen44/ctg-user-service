package com.ctg.controller;

import com.ctg.dto.LoginUserResponse;
import com.ctg.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserService service;

    @GetMapping("/users/by-email")
    public ResponseEntity<LoginUserResponse> byEmail(@RequestParam String email) {
        return ResponseEntity.ok(service.findByEmailForLogin(email.toLowerCase()));
    }

    @PostMapping("/users/{id}/token-version/increment")
    public ResponseEntity<Void> increment(@PathVariable Long id) {
        service.incrementTokenVersion(id);
        return ResponseEntity.noContent().build();
    }
}
