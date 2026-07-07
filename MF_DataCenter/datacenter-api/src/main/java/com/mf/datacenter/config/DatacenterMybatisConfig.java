package com.mf.datacenter.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "datacenter.mysql.enabled", havingValue = "true")
@MapperScan("com.mf.datacenter.**.mapper")
public class DatacenterMybatisConfig {
}
