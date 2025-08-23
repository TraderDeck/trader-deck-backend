package com.traderdeck.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobAcceptedResponse {
    private String jobId;
}