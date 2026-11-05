package com.xianhe.config;

import com.xianhe.interceptor.JwtTokenStaffInterceptor;
import com.xianhe.interceptor.JwtTokenUserInterceptor;
import com.xianhe.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

/**
 * Web 层配置：拦截器、消息转换器、静态资源、接口文档
 */
@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenStaffInterceptor jwtTokenStaffInterceptor;

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册两端 JWT 拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 两端各自一个拦截器：令牌放在不同的请求头、用不同的密钥，互不通用
        registry.addInterceptor(jwtTokenStaffInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/staff/login");

        // 用户端白名单：微信登录与门店营业状态（《接口文档-用户端》1）
        // 营业状态未登录也要能查，否则打烊期间小程序连「门店已打烊」都显示不出来
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/member/login", "/user/shop/status");
    }

    /**
     * 把自定义的 JSON 转换器放到转换器列表最前面，使全站时间格式统一
     * @param converters
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 全站时间统一为 yyyy-MM-dd HH:mm:ss（《接口文档》1.7）
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new JacksonObjectMapper());
        converters.add(0, converter);
        log.info("已装配自定义 JSON 消息转换器");
    }

    /**
     * knife4j 的静态资源。用 WebMvcConfigurer 时仍需显式声明，
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 否则 /doc.html 会 404。
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 管理端接口文档分组
     * @return
     */
    @Bean
    public Docket docketAdmin() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("鲜禾前置仓 · 管理端接口")
                .version("1.0")
                .description("门店运营台接口，与《接口文档》第二部分逐条对应")
                .build();
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("管理端接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.xianhe.controller.admin"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 用户端接口文档分组
     * @return
     */
    @Bean
    public Docket docketUser() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("鲜禾前置仓 · 用户端接口")
                .version("1.0")
                .description("小程序接口，与《接口文档-用户端》逐条对应")
                .build();
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("用户端接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.xianhe.controller.user"))
                .paths(PathSelectors.any())
                .build();
    }
}
