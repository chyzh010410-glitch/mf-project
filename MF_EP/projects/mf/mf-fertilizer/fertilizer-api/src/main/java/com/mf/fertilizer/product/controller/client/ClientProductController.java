package com.mf.fertilizer.product.controller.client;

import com.mf.fertilizer.constant.CacheNames;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.product.entity.ProductCategory;
import com.mf.fertilizer.product.service.ProductCatalogApplicationService;
import com.mf.fertilizer.product.vo.client.ProductVO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/client/products")
@RequiredArgsConstructor
public class ClientProductController {

    private final ProductCatalogApplicationService productCatalogApplicationService;

    @GetMapping
    @Cacheable(value = CacheNames.PRODUCTS, key = "'p' + #page.page + ':s' + #page.size + ':pt' + (#productType != null ? #productType : 'all') + ':cid' + (#categoryId != null ? #categoryId : 'all') + ':kw' + (#keyword != null ? #keyword : '') + ':sort' + (#sort != null ? #sort : 'default') + ':min' + (#minPrice != null ? #minPrice : '') + ':max' + (#maxPrice != null ? #maxPrice : '')")
    public ResultVO<PageVO<ProductVO>> list(@ModelAttribute PageDTO page,
                                            @RequestParam(name = "categoryId", required = false) Long categoryId,
                                            @RequestParam(name = "keyword", required = false) String keyword,
                                            @RequestParam(name = "sort", required = false) String sort,
                                            @RequestParam(name = "productType", required = false) String productType,
                                            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
                                            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice) {
        return ResultVO.success(productCatalogApplicationService.listProducts(page, categoryId, keyword, sort, productType, minPrice, maxPrice));
    }

    @GetMapping("/{id}")
    public ResultVO<ProductVO> detail(@PathVariable Long id) {
        return ResultVO.success(productCatalogApplicationService.getProductDetail(id));
    }

    @GetMapping("/categories")
    public ResultVO<List<ProductCategory>> categories(@RequestParam(required = false) String type) {
        return ResultVO.success(productCatalogApplicationService.listCategories(type));
    }
}
