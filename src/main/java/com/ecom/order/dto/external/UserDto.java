package com.ecom.order.dto.external;

public record UserDto(
    Long id,
    String username,
    String firstName,
    String lastName,
    String email,
    String phone,
    boolean isAdmin
) {}
