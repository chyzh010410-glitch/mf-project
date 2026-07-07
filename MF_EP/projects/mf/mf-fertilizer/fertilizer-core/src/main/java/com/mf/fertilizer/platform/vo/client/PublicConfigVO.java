package com.mf.fertilizer.platform.vo.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicConfigVO implements Serializable {
    private String navProductLabel;
    private String navEncyclopediaLabel;
    private Boolean paymentEnabled;
}
