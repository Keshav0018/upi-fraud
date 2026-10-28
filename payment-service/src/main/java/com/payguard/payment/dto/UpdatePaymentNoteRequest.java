package com.payguard.payment.dto;

import jakarta.validation.constraints.Size;

public class UpdatePaymentNoteRequest {

    @Size(max = 280, message = "note must be at most 280 characters")
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
