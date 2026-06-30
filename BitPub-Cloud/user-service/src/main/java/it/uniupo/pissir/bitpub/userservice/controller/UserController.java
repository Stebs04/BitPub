package it.uniupo.pissir.bitpub.userservice.controller;

import it.uniupo.pissir.bitpub.userservice.dto.CreateUserRequest;
import it.uniupo.pissir.bitpub.userservice.dto.UserDto;
import it.uniupo.pissir.bitpub.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable("id") String id) {
        return userService.getUserById(id);
    }
    
    @GetMapping("/by-username/{username}")
    public UserDto getUserByUsername(@PathVariable("username") String username) {
        return userService.getUserByUsername(username);
    }

    @GetMapping
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/count")
    public long countTotalUsers() {
        return userService.countTotalUsers();
    }
}
