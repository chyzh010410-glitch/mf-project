package com.mf.fertilizer.platform.controller.client;

import com.mf.fertilizer.platform.service.PlatformConfigService;
import com.mf.fertilizer.platform.vo.client.PublicConfigVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client/configs")
@RequiredArgsConstructor
public class ClientConfigController {

    private final PlatformConfigService configService;

    @GetMapping("/public")
    public ResultVO<PublicConfigVO> publicConfig() {
        String navProductLabel = configService.getValue("nav_product_label", "商品商城");
        String navEncyclopediaLabel = configService.getValue("nav_encyclopedia_label", "树木百科");
        boolean paymentEnabled = configService.getBoolean("payment_enabled", false);
        return ResultVO.success(new PublicConfigVO(navProductLabel, navEncyclopediaLabel, paymentEnabled));
    }
}
