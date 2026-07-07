package com.mf.fertilizer.fertilization.dto;

import com.mf.fertilizer.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FertilizerQueryDTO extends PageDTO {

    private String name;

    private String type;
}
