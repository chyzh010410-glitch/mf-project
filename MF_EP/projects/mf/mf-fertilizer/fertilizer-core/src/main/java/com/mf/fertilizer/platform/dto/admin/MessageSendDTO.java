package com.mf.fertilizer.platform.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class MessageSendDTO {
    private List<Long> userIds;
    @NotBlank private String title;
    @NotBlank private String content;
    @NotBlank private String type;
    private String pushChannel;
}
