package com.mf.fertilizer.product.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.product.entity.ProductCategory;
import com.mf.fertilizer.product.service.ProductAdminApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final ProductAdminApplicationService productAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<ProductCategory>> list(@ModelAttribute PageDTO page,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(required = false) String type) {
        return ResultVO.success(productAdminApplicationService.listCategories(page, name, type));
    }

    @GetMapping("/{id}")
    public ResultVO<ProductCategory> detail(@PathVariable Long id) {
        return ResultVO.success(productAdminApplicationService.getCategory(id));
    }

    @PostMapping
    @OperationLog(module = "分类管理", action = "新增")
    public ResultVO<?> save(@RequestBody ProductCategory dto) {
        productAdminApplicationService.createCategory(dto);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "分类管理", action = "编辑")
    public ResultVO<?> update(@PathVariable Long id, @RequestBody ProductCategory dto) {
        productAdminApplicationService.updateCategory(id, dto);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "分类管理", action = "删除")
    public ResultVO<?> delete(@PathVariable Long id) {
        productAdminApplicationService.deleteCategory(id);
        return ResultVO.success();
    }
}
