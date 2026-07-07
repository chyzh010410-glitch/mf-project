package com.mf.fertilizer.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作模块, e.g. "商品管理" */
    String module();
    /** 操作类型, e.g. "新增", "删除", "编辑" */
    String action();
    /** 操作目标描述, 支持 SpEL 表达式, e.g. "#dto.name" */
    String target() default "";
}
