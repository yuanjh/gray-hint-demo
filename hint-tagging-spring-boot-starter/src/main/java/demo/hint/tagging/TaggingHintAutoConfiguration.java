package demo.hint.tagging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

// 只在网关（classpath 上有 Spring Cloud Gateway）且 gray.hint.enabled=true 时装配打标过滤器。
// @ConditionalOnMissingBean 保留覆盖能力：想按机构号/渠道打标时自定义 GrayHintFilter Bean 即可替换。
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@ConditionalOnProperty(prefix = "gray.hint", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GrayHintProperties.class)
public class TaggingHintAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(GrayHintFilter.class)
    GrayHintFilter grayHintFilter(GrayHintProperties properties) {
        return new GrayHintFilter(properties);
    }
}
