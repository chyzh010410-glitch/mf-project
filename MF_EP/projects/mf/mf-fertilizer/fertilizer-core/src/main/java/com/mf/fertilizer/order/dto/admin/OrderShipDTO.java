package com.mf.fertilizer.order.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderShipDTO {
    @NotBlank private String logisticsCompany;
    @NotBlank private String logisticsNo;
}
