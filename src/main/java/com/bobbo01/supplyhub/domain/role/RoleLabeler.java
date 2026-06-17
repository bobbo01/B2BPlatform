package com.bobbo01.supplyhub.domain.role;

import com.bobbo01.supplyhub.domain.role.entity.RoleNames;

public final class RoleLabeler {

    private RoleLabeler() {
    }

    public static String toRoleLabel(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "알 수 없음";
        }
        return switch (roleName) {
            case RoleNames.PLATFORM_ADMIN -> "플랫폼 관리자";
            case RoleNames.COMPANY_ADMIN -> "회사 관리자";
            case RoleNames.CART_USER -> "일반 사용자";
            case RoleNames.PURCHASER -> "구매 담당자";
            case RoleNames.APPROVER -> "승인 담당자";
            case "COMPANY_USER" -> "회사 사용자";
            default -> roleName;
        };
    }
}
