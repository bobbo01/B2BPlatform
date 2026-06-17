package com.bobbo01.supplyhub.domain.purchase.dto;

import java.time.LocalDateTime;

public record PurchaseOrderStatusHistoryView(
        String fromStatusCode,
        String fromStatusLabel,
        String toStatusCode,
        String toStatusLabel,
        String changedByName,
        String changeNote,
        LocalDateTime changedAt,
        String transitionLabel,
        String changedByDisplayName,
        String changeNoteDisplay
) {
}
