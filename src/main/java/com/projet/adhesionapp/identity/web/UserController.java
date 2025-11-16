package com.projet.adhesionapp.identity.web;


import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API de gestion basique des utilisateurs (patients).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PostMapping("/{id}/deactivate")
    public void deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
    }
}
