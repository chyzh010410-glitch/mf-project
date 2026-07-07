package com.mf.fertilizer.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.dto.PageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageVOTest {

    @Test
    void ofPageDtoAndTotalKeepsPageMetadata() {
        PageDTO page = new PageDTO();
        page.setPage(2);
        page.setSize(5);
        List<String> records = List.of("a", "b");

        PageVO<String> result = PageVO.of(page, 12L, records);

        assertEquals(12L, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(records, result.getRecords());
    }

    @Test
    void ofPageDtoAndIPageKeepsMybatisPageResult() {
        PageDTO page = new PageDTO();
        page.setPage(3);
        page.setSize(10);
        Page<String> mybatisPage = new Page<>(3, 10);
        mybatisPage.setTotal(21);
        mybatisPage.setRecords(List.of("x", "y"));

        PageVO<String> result = PageVO.of(page, mybatisPage);

        assertEquals(21L, result.getTotal());
        assertEquals(3, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(List.of("x", "y"), result.getRecords());
    }
}
