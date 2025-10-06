package com.ctg.controller;

import com.ctg.dto.LoginLookupResponse;
import com.ctg.model.User;
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
    public ResponseEntity<LoginLookupResponse> byEmail(@RequestParam String email) {
        User user = service.findByEmailForLogin(email.toLowerCase());
        return ResponseEntity.ok(new LoginLookupResponse(
                user.getId(), user.getEmail(), user.getPassword(), user.getRole(), user.getTokenVersion()
        ));
    }

    @PostMapping("/users/{id}/token-version/increment")
    public ResponseEntity<Void> increment(@PathVariable Long id) {
        service.incrementTokenVersion(id);
        return ResponseEntity.noContent().build();
    }
}
