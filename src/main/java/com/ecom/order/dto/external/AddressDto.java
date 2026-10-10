package com.ecom.order.dto.external;

public record AddressDto(
    Long addressId,
    String building,
    String area,
    String city,
    String state,
    String country,
    String pincode
) {}
