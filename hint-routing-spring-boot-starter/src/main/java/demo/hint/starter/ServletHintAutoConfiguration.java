package demo.hint.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;

// 引入依赖即自动启用；根据应用类型区分网关和 Servlet 微服务。
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@LoadBalancerClients(defaultConfiguration = ServletHintConfiguration.class)
public class ServletHintAutoConfiguration {}
