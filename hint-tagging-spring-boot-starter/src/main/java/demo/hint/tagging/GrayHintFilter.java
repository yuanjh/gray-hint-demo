package demo.hint.tagging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// 打标唯一入口：按登录柜员身份（柜员号/角色）决定灰度，外部传入的 Hint 一律覆盖。
// 只服务于网关入口；下游服务靠 hint-routing-spring-boot-starter 的 ForwardHint 透传。
public class GrayHintFilter implements GlobalFilter, Ordered {
    private static final String HINT_HEADER = "X-SC-LB-Hint";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GrayHintProperties properties;

    public GrayHintFilter(GrayHintProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return 10000; // 先打标；日志过滤器(10001) 只记录，负载均衡选址(10150) 在更后面
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        TellerIdentity teller = resolve(exchange);
        String hint = isGray(teller) ? properties.getLane() : null;
        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(HINT_HEADER);
            if (hint != null) headers.set(HINT_HEADER, hint);
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isGray(TellerIdentity teller) {
        return properties.getTellers().contains(teller.tellerNo())
                || teller.roles().stream().anyMatch(properties.getRoles()::contains);
    }

    private TellerIdentity resolve(ServerWebExchange exchange) {
        TellerIdentity fromToken = parseToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (fromToken != null) return fromToken;
        // 演示兜底：无 token 时允许直接传身份头。生产上应由鉴权过滤器校验 token 后写入，
        // 并清除外部伪造的同名 Header，避免身份冒用。
        String tellerNo = exchange.getRequest().getHeaders().getFirst("X-Teller-No");
        Set<String> roles = new LinkedHashSet<>();
        String raw = exchange.getRequest().getHeaders().getFirst("X-Teller-Roles");
        if (raw != null) {
            for (String r : raw.split(",")) {
                if (!r.isBlank()) roles.add(r.trim());
            }
        }
        return new TellerIdentity(tellerNo == null ? "" : tellerNo, roles);
    }

    // 生产环境请在鉴权过滤器/JWT 资源服务器验签后再取 claims，此处只演示解析。
    private TellerIdentity parseToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String[] parts = authorization.substring(7).split("\\.");
        if (parts.length < 2) return null;
        try {
            JsonNode claims = MAPPER.readTree(Base64.getUrlDecoder().decode(parts[1]));
            Set<String> roles = new LinkedHashSet<>();
            claims.path("roles").forEach(node -> roles.add(node.asText()));
            return new TellerIdentity(claims.path("tellerNo").asText(""), roles);
        } catch (Exception e) {
            return null;
        }
    }

    private record TellerIdentity(String tellerNo, Set<String> roles) {}
}
