package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryDto {
    private String status;
    private String note;
    private String createdAt;
}
