package com.routineflow.infrastructure.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        // Map the ETag filter only to endpoints where payload might be large and repetitive
        filterRegistrationBean.addUrlPatterns("/api/analytics/heatmap", "/api/analytics/weekly/*");
        filterRegistrationBean.setName("etagFilter");
        return filterRegistrationBean;
    }
}
