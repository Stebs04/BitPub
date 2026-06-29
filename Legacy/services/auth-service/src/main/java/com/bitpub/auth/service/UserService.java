package com.bitpub.auth.service;

import com.bitpub.auth.dto.UserDto;
import com.bitpub.auth.model.User;
import com.bitpub.auth.repository.UserRepository;
import com.bitpub.auth.specification.UserSpecification;
import com.bitpub.common.dto.PageResponse;
import com.bitpub.common.specification.SearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public PageResponse<UserDto> getUsers(List<SearchCriteria> criteria, Pageable pageable) {
        Specification<User> spec = UserSpecification.createSpecification(criteria);
        Page<User> page = userRepository.findAll(spec, pageable);
        
        PageResponse<UserDto> response = new PageResponse<>();
        response.setContent(page.getContent().stream().map(this::toDto).collect(Collectors.toList()));
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        
        return response;
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
    }
}
