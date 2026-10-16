package demo.hint.gateway;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

// 只负责演示日志，不参与灰度判断；Header 仍由网关路由配置设置。
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);
    public int getOrder() { return 10001; } // 路由已设置 Header，尚未进行负载均衡选址。
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        var request = exchange.getRequest().mutate()
                .headers(headers -> headers.set("X-Request-Id", id)).build();
        var next = exchange.mutate().request(request).build();
        exchange.getResponse().getHeaders().set("X-Request-Id", id);
        log.info("[{}] ① 网关收到 {} {}，设置 Hint={}，准备调用 A", id,
                request.getMethod(), request.getURI(), request.getHeaders().getFirst("X-SC-LB-Hint"));
        return chain.filter(next).doFinally(signal ->
                log.info("[{}] ⑤ 网关结束，实际 A 地址={}，响应状态={}", id,
                        next.getAttribute(GATEWAY_REQUEST_URL_ATTR), next.getResponse().getStatusCode()));
    }
}
