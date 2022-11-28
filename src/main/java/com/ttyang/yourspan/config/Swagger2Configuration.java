package com.ttyang.yourspan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-16
 */
@Configuration
@EnableSwagger2
public class Swagger2Configuration {
    @Bean
    public Docket webApiConfig() {
        List<Parameter> pars = new ArrayList<>();
        ParameterBuilder pb = new ParameterBuilder();
        pb.name("case")
                .description("接口用例")
                .defaultValue("0")
                .modelRef(new ModelRef("string"))
                .parameterType("header")
                .required(false)
                .build();
        pars.add(pb.build());
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("webApi")
                .apiInfo(webApiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.ttyang.yourspan.controller"))
                .build()
                .globalOperationParameters(pars);
    }

    public ApiInfo webApiInfo() {
        return new ApiInfoBuilder()
                .title("YoursPan-API文档")
                .description("Controller层各接口定义")
                .version("1.0")
                .contact(new Contact("ttyang", "https://github.com/ttyang361/yoursname.git", "3615796004@qq.com"))
                .build();
    }
}
