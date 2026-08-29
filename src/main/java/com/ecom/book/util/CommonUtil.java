package com.ecom.book.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CommonUtil {

	public static String getCurrentUsername() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}
}
