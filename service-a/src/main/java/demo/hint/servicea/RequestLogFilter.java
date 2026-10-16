package demo.hint.servicea;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 演示日志统一写在 Filter，Controller 不需要读取 Header。
@Component
public class RequestLogFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);
    @Value("${demo.lane}") String lane;
    @Value("${server.port}") String port;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!request.getRequestURI().equals("/api/demo")) {
            chain.doFilter(request, response);
            return;
        }
        String id = request.getHeader("X-Request-Id");
        log.info("[{}] ② A 收到请求，实际实例=a-{}:{}，{} {}，query={}，Hint={}", id,
                lane, port, request.getMethod(), request.getRequestURI(), request.getQueryString(),
                request.getHeader("X-SC-LB-Hint"));
        try { chain.doFilter(request, response); }
        finally { log.info("[{}] A 返回，实例=a-{}:{}，状态={}", id, lane, port, response.getStatus()); }
    }
}
