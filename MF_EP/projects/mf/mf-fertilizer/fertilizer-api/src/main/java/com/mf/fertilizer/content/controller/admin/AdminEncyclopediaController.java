package com.mf.fertilizer.content.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.content.dto.admin.EncyclopediaSaveDTO;
import com.mf.fertilizer.content.entity.EncyclopediaEntry;
import com.mf.fertilizer.content.service.ContentAdminApplicationService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/encyclopedia")
@RequiredArgsConstructor
public class AdminEncyclopediaController {

    private final ContentAdminApplicationService contentAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<EncyclopediaEntry>> list(@ModelAttribute PageDTO page,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer isPublished) {
        return ResultVO.success(contentAdminApplicationService.listEntries(page, keyword, isPublished));
    }

    @GetMapping("/{id}")
    public ResultVO<EncyclopediaEntry> detail(@PathVariable Long id) {
        return ResultVO.success(contentAdminApplicationService.getEntry(id));
    }

    @PostMapping
    @OperationLog(module = "百科管理", action = "新增")
    public ResultVO<?> save(@RequestBody EncyclopediaSaveDTO dto) {
        contentAdminApplicationService.createEntry(dto);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "百科管理", action = "编辑")
    public ResultVO<?> update(@PathVariable Long id, @RequestBody EncyclopediaSaveDTO dto) {
        contentAdminApplicationService.updateEntry(id, dto);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "百科管理", action = "删除")
    public ResultVO<?> delete(@PathVariable Long id) {
        contentAdminApplicationService.deleteEntry(id);
        return ResultVO.success();
    }

    @PutMapping("/{id}/publish")
    @OperationLog(module = "百科管理", action = "发布切换")
    public ResultVO<?> togglePublish(@PathVariable Long id) {
        contentAdminApplicationService.toggleEntryPublish(id);
        return ResultVO.success();
    }
}
