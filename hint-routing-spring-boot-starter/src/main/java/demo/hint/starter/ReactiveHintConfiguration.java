package demo.hint.starter;

import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

// 仅由 LoadBalancer 子容器加载。业务工程不需要复制或手动导入此配置。
public class ReactiveHintConfiguration {
    @Bean
    ServiceInstanceListSupplier hintSupplier(ConfigurableApplicationContext context,
                                            LoadBalancerClientFactory factory) {
        var discovery = ServiceInstanceListSupplier.builder().withDiscoveryClient().build(context);
        return new LaneSupplier(discovery, factory);
    }
}
