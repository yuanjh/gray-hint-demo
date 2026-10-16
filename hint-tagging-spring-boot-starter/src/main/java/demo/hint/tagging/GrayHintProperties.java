package demo.hint.tagging;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 灰度名单建议放配置中心（Nacos/Apollo）动态维护：给柜员配置灰度角色后无需改代码发版。
@ConfigurationProperties(prefix = "gray.hint")
public class GrayHintProperties {
    // 开关：不配置时为 false，打标过滤器不装配
    private boolean enabled = false;
    // 试点泳道标记值，与试点实例 metadata 的 hint 一致
    private String lane = "trial";
    // 配置了灰度角色的柜员，其所有操作走试点
    private List<String> roles = new ArrayList<>(List.of("GRAY_TRIAL"));
    // 按柜员号直接配置的灰度名单（临时试点/兜底）
    private List<String> tellers = new ArrayList<>(List.of("8888"));

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getLane() { return lane; }
    public void setLane(String lane) { this.lane = lane; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<String> getTellers() { return tellers; }
    public void setTellers(List<String> tellers) { this.tellers = tellers; }
}
