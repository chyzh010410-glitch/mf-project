package com.mf.fertilizer.merchant.controller;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.product.dto.admin.ProductSaveDTO;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.service.ProductMerchantApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchant/products")
@RequiredArgsConstructor
public class MerchantProductController {

    private final ProductMerchantApplicationService productMerchantApplicationService;

    @GetMapping
    public ResultVO<PageVO<Product>> list(@ModelAttribute PageDTO page,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) String productType,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) Integer status) {
        return ResultVO.success(productMerchantApplicationService.listProducts(
                UserContext.requireUserId(), page, name, productType, categoryId, status
        ));
    }

    @PostMapping
    public ResultVO<?> create(@Valid @RequestBody ProductSaveDTO dto) {
        productMerchantApplicationService.createProduct(UserContext.requireUserId(), dto);
        return ResultVO.success();
    }

    @GetMapping("/{id}")
    public ResultVO<?> detail(@PathVariable Long id) {
        return ResultVO.success(productMerchantApplicationService.getProductDetail(UserContext.requireUserId(), id));
    }

    @PutMapping("/{id}")
    public ResultVO<?> update(@PathVariable Long id, @Valid @RequestBody ProductSaveDTO dto) {
        productMerchantApplicationService.updateProduct(UserContext.requireUserId(), id, dto);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    public ResultVO<?> delete(@PathVariable Long id) {
        productMerchantApplicationService.deleteProduct(UserContext.requireUserId(), id);
        return ResultVO.success();
    }
}
