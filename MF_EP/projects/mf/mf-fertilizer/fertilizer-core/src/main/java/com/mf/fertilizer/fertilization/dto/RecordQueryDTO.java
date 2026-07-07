package com.mf.fertilizer.fertilization.dto;

import com.mf.fertilizer.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class RecordQueryDTO extends PageDTO {

    private Long treeId;

    private Long fertilizerId;

    private LocalDate startDate;

    private LocalDate endDate;
}
