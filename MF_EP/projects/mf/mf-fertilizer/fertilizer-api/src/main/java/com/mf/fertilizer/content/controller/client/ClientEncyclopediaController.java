package com.mf.fertilizer.content.controller.client;

import com.mf.fertilizer.content.entity.EncyclopediaEntry;
import com.mf.fertilizer.content.service.ContentCatalogApplicationService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/encyclopedia")
@RequiredArgsConstructor
public class ClientEncyclopediaController {

    private final ContentCatalogApplicationService contentCatalogApplicationService;

    @GetMapping
    public ResultVO<PageVO<EncyclopediaEntry>> list(@ModelAttribute PageDTO page,
                                                    @RequestParam(name = "keyword", required = false) String keyword,
                                                    @RequestParam(name = "categoryId", required = false) Long categoryId) {
        return ResultVO.success(contentCatalogApplicationService.listEntries(page, keyword, categoryId));
    }

    @GetMapping("/{id}")
    public ResultVO<EncyclopediaEntry> detail(@PathVariable Long id) {
        return ResultVO.success(contentCatalogApplicationService.getEntryDetail(id));
    }
}
