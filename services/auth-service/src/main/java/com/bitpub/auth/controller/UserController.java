package com.bitpub.auth.controller;

import com.bitpub.auth.dto.UserDto;
import com.bitpub.auth.service.UserService;
import com.bitpub.common.dto.PageResponse;
import com.bitpub.common.specification.SearchCriteria;
import com.bitpub.common.specification.SearchOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestione utenti")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Lista utenti con paginazione e filtri")
    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'LOCAL_ADMIN', 'GAME_ADMIN')")
    public ResponseEntity<PageResponse<UserDto>> getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 20) Pageable pageable) {

        List<SearchCriteria> criteria = new ArrayList<>();
        
        if (username != null && !username.isBlank()) {
            criteria.add(new SearchCriteria("username", SearchOperation.CONTAINS, username));
        }
        if (email != null && !email.isBlank()) {
            criteria.add(new SearchCriteria("email", SearchOperation.CONTAINS, email));
        }

        return ResponseEntity.ok(userService.getUsers(criteria, pageable));
    }
}
