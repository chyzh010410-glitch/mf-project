package com.mf.fertilizer.platform.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.dto.admin.FaqSaveDTO;
import com.mf.fertilizer.platform.entity.Faq;
import com.mf.fertilizer.platform.service.PlatformAdminApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/faqs")
@RequiredArgsConstructor
public class AdminFaqController {

    private final PlatformAdminApplicationService platformAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<Faq>> list(@ModelAttribute PageDTO page,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String category) {
        return ResultVO.success(platformAdminApplicationService.listFaqs(page, keyword, category));
    }

    @GetMapping("/{id}")
    public ResultVO<Faq> detail(@PathVariable Long id) {
        return ResultVO.success(platformAdminApplicationService.getFaq(id));
    }

    @PostMapping
    @OperationLog(module = "FAQ管理", action = "新增")
    public ResultVO<?> save(@RequestBody FaqSaveDTO dto) {
        platformAdminApplicationService.createFaq(dto);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "FAQ管理", action = "编辑")
    public ResultVO<?> update(@PathVariable Long id, @RequestBody FaqSaveDTO dto) {
        platformAdminApplicationService.updateFaq(id, dto);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "FAQ管理", action = "删除")
    public ResultVO<?> delete(@PathVariable Long id) {
        platformAdminApplicationService.deleteFaq(id);
        return ResultVO.success();
    }
}
