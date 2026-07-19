package com.lucky.seckill.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * 网关 Sentinel 限流：全入口 QPS 上限 10000
 */
@Configuration
public class GatewaySentinelConfig {

    /** 自定义 API 分组名，匹配全部网关流量 */
    public static final String GATEWAY_API_NAME = "lucky_seckill_gateway";

    /** 每秒最大请求数 */
    public static final double GATEWAY_QPS_LIMIT = 10000D;

    @PostConstruct
    public void initGatewayRules() {
        initApiDefinitions();
        initFlowRules();
    }

    private void initApiDefinitions() {
        Set<ApiPredicateItem> predicateItems = new HashSet<>();
        predicateItems.add(new ApiPathPredicateItem()
                .setPattern("/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
        Set<ApiDefinition> definitions = new HashSet<>();
        definitions.add(new ApiDefinition(GATEWAY_API_NAME).setPredicateItems(predicateItems));
        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    private void initFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule(GATEWAY_API_NAME)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(GATEWAY_QPS_LIMIT)
                .setIntervalSec(1));
        GatewayRuleManager.loadRules(rules);
    }
}
