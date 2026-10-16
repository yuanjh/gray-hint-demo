package demo.hint.serviceb;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
@RestController public class DemoController {
 @Value("${demo.lane}") String lane;
 @Value("${server.port}") String port;
 @GetMapping("/internal/demo") public Map<String,String> read() {
  // 展示实际落点。B 是链路末端，不需要 Agent 或选址组件，也不拒绝跨标签回退。
  return Map.of("instance","b-"+lane+":"+port,"instanceHint", "trial".equals(lane) ? "trial" : "");
 }
}
