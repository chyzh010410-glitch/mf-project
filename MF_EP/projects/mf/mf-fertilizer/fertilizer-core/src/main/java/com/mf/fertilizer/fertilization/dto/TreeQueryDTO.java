package com.mf.fertilizer.fertilization.dto;

import com.mf.fertilizer.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TreeQueryDTO extends PageDTO {

    private String species;

    private String status;

    private Integer ageMin;

    private Integer ageMax;
}
