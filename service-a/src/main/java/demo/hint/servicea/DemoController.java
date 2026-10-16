package demo.hint.servicea;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
@RestController public class DemoController {
 private final BClient b;
 public DemoController(BClient b){this.b=b;}
 @Value("${demo.lane}") String lane;
 @Value("${server.port}") String port;
 @GetMapping("/api/demo") public Map<String,Object> demo(){
  return Map.of("serviceA",Map.of("instance","a-"+lane+":"+port,"instanceHint", "trial".equals(lane) ? "trial" : ""),"serviceB",b.read());
 }
}
