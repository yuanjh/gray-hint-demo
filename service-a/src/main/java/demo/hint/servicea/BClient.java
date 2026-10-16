package demo.hint.servicea;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
// 固定逻辑服务名；具体 B 地址由 Eureka + Hint + LoadBalancer 共同确定。
@FeignClient(name="hint-service-b")
public interface BClient { @GetMapping("/internal/demo") Map<String,String> read(); }
