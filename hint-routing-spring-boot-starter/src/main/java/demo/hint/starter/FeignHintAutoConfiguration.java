package demo.hint.starter;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

// 只有 Servlet 应用存在 Feign 时才装配。网关不会加载 Servlet/Feign 组件。
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = {"feign.RequestInterceptor", "jakarta.servlet.http.HttpServletRequest"})
public class FeignHintAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ForwardHint.class)
    ForwardHint forwardHint() {
        return new ForwardHint();
    }
}
