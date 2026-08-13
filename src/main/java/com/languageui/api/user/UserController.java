package com.languageui.api.user;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    List<UserAccount> findAll() { return userService.findAll(); }

    @GetMapping("/{id}")
    UserAccount findById(@PathVariable UUID id) { return userService.findById(id); }

    @PostMapping
    ResponseEntity<UserAccount> create(@Valid @RequestBody CreateUserRequest request) {
        UserAccount user = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
    }

    @PostMapping("/{id}/languages/{languageCode}")
    UserAccount addLanguage(@PathVariable UUID id, @PathVariable String languageCode) {
        return userService.addLanguage(id, languageCode);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
