package com.nisum.userservice.config;

import java.util.Set;

public final class UserSortConfig {

    private UserSortConfig() {
    }

    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "email",
            "createdAt",
            "status"
    );
}
