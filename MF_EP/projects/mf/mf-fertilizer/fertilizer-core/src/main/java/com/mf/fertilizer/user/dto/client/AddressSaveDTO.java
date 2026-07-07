package com.mf.fertilizer.user.dto.client;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressSaveDTO {
    @NotBlank private String receiverName;
    @NotBlank private String receiverPhone;
    @NotBlank private String province;
    @NotBlank private String city;
    @NotBlank private String district;
    @NotBlank private String detail;
    private String postalCode;
    private Integer isDefault;
    private String tag;
}
