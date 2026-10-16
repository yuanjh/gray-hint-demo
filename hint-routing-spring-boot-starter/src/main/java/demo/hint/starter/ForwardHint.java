package demo.hint.starter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ForwardHint implements RequestInterceptor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ForwardHint.class);
    @Override
    public void apply(RequestTemplate template) {
        // Feign 发出的是新 HTTP 请求，不会自动继承 A 收到的 Header。
        // 这一个拦截器对 A 的所有同步 Feign 调用生效，业务方法不用改。
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String id = attrs.getRequest().getHeader("X-Request-Id");
            if (id != null) template.header("X-Request-Id", id);
            String hint = attrs.getRequest().getHeader("X-SC-LB-Hint");
            if (hint != null && !hint.isBlank()) {
                template.header("X-SC-LB-Hint", hint);
            }
            log.info("[{}] ③ A 发起 Feign 请求：{} {}，目标服务={}，透传 Hint={}",
                    id, template.method(), template.url(), template.feignTarget().name(), hint);
        }
        // 没有 Header 就什么也不做，不补 main、不用 A 自己的身份替换请求标签。
    }
}
