package demo.hint.gateway;

import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import demo.hint.tagging.GrayHintProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// Apollo 修改灰度名单后实时生效：@ConfigurationProperties 默认不会自动刷新，
// 这里监听 gray.hint.* 变更并把新值原地重新绑定到 GrayHintProperties 单例；
// GrayHintFilter 每次请求都从该 Bean 读取，改名单无需重启网关。
@Component
@ConditionalOnProperty(prefix = "apollo.bootstrap", name = "enabled", havingValue = "true")
public class ApolloGrayHintRefresher {
    private static final Logger log = LoggerFactory.getLogger(ApolloGrayHintRefresher.class);

    private final Environment environment;
    private final ObjectProvider<GrayHintProperties> properties;

    public ApolloGrayHintRefresher(Environment environment, ObjectProvider<GrayHintProperties> properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @ApolloConfigChangeListener(interestedKeyPrefixes = "gray.hint.")
    public void onChange(ConfigChangeEvent event) {
        GrayHintProperties target = properties.getIfAvailable();
        if (target == null) return; // gray.hint.enabled 未开启时没有该 Bean
        Binder.get(environment).bind("gray.hint", Bindable.ofInstance(target));
        log.info("Apollo 灰度名单已更新：{}，当前 roles={} tellers={} lane={}",
                event.changedKeys(), target.getRoles(), target.getTellers(), target.getLane());
    }
}
