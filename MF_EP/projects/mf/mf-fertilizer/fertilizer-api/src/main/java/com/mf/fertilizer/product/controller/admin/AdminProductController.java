package com.mf.fertilizer.product.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.constant.CacheNames;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.product.dto.admin.ProductSaveDTO;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.service.ProductAdminApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductAdminApplicationService productAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<Product>> list(@ModelAttribute PageDTO page,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) String productType,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) Integer status) {
        return ResultVO.success(productAdminApplicationService.listProducts(page, name, productType, categoryId, status));
    }

    @GetMapping("/{id}")
    public ResultVO<?> detail(@PathVariable Long id) {
        return ResultVO.success(productAdminApplicationService.getProductDetail(id));
    }

    @PostMapping
    @CacheEvict(value = CacheNames.PRODUCTS, allEntries = true)
    @OperationLog(module = "商品管理", action = "新增")
    public ResultVO<?> save(@RequestBody ProductSaveDTO dto) {
        productAdminApplicationService.createProduct(dto);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @CacheEvict(value = CacheNames.PRODUCTS, allEntries = true)
    @OperationLog(module = "商品管理", action = "编辑")
    public ResultVO<?> update(@PathVariable Long id, @RequestBody ProductSaveDTO dto) {
        productAdminApplicationService.updateProduct(id, dto);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = CacheNames.PRODUCTS, allEntries = true)
    @OperationLog(module = "商品管理", action = "删除")
    public ResultVO<?> delete(@PathVariable Long id) {
        productAdminApplicationService.deleteProduct(id);
        return ResultVO.success();
    }

    @PutMapping("/{id}/status")
    @CacheEvict(value = CacheNames.PRODUCTS, allEntries = true)
    @OperationLog(module = "商品管理", action = "上下架")
    public ResultVO<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        productAdminApplicationService.updateProductStatus(id, body.get("status"));
        return ResultVO.success();
    }

    @PutMapping("/{id}/recommend")
    @CacheEvict(value = CacheNames.PRODUCTS, allEntries = true)
    public ResultVO<?> toggleRecommend(@PathVariable Long id) {
        productAdminApplicationService.toggleProductRecommend(id);
        return ResultVO.success();
    }

    @PutMapping("/{id}/new")
    @CacheEvict(value = CacheNames.PRODUCTS, allEntries = true)
    public ResultVO<?> toggleNew(@PathVariable Long id) {
        productAdminApplicationService.toggleProductNew(id);
        return ResultVO.success();
    }
}
