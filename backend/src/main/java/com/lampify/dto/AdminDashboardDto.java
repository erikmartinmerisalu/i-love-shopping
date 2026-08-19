package com.lampify.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardDto {
    private long totalProducts;
    private long activeProducts;
    private long lowStockProducts;
    private long totalOrders;
    private long pendingOrders;
    private long totalUsers;
    private long adminUsers;
}
