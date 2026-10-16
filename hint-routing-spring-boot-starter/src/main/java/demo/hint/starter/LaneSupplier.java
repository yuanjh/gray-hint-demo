package demo.hint.starter;

import java.util.List;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.loadbalancer.core.HintBasedServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

public class LaneSupplier extends HintBasedServiceInstanceListSupplier {
    private final ServiceInstanceListSupplier discovery;

    public LaneSupplier(ServiceInstanceListSupplier discovery, LoadBalancerClientFactory factory) {
        super(discovery, factory);
        this.discovery = discovery;
    }

    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        if (request.getContext() instanceof RequestDataContext context
                && context.getClientRequest() != null
                && StringUtils.hasText(context.getClientRequest().getHeaders().getFirst("X-SC-LB-Hint"))) {
            // 带标请求：沿用 Spring 原生逻辑，优先匹配，匹配不到则回退全部实例。
            return super.get(request);
        }
        // 普通请求：只能选没有标签的实例。没有普通节点就返回空列表，不能转到试点。
        return discovery.get(request).map(this::ordinaryOnly);
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return discovery.get().map(this::ordinaryOnly);
    }

    private List<ServiceInstance> ordinaryOnly(List<ServiceInstance> instances) {
        return instances.stream().filter(instance ->
                !StringUtils.hasText(instance.getMetadata().get("hint"))).toList();
    }
}
