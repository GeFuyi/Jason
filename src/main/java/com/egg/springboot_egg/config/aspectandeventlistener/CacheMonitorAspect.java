package com.egg.springboot_egg.config.aspectandeventlistener;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.logging.Logger;

@Aspect
@Order(1) // 调整优先级，确保在 Spring 缓存后执行监控
@Component
public class CacheMonitorAspect {

    private static final Logger logger = Logger.getLogger(CacheMonitorAspect.class.getName());

    @Autowired
    private CacheManager cacheManager;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    @Around("@annotation(cacheable)")
    public Object monitorCache(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        String cacheName = cacheable.value().length > 0 ? cacheable.value()[0] : cacheable.cacheNames().length > 0 ? cacheable.cacheNames()[0] : "default";
        Object key = evaluateKey(joinPoint, cacheable.key());

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                Object cachedValue = wrapper.get();
                logger.info("[CACHE HIT] cacheName: " + cacheName + ", key: " + key + ", content: " + (cachedValue != null ? cachedValue.toString() : "null"));
                // 直接返回缓存值，避免 proceed（但为了不干扰 Spring，实际还是 proceed，让 Spring 处理一致性）
            } else {
                logger.info("[CACHE MISS] cacheName: " + cacheName + ", key: " + key);
            }
        }

        // 执行原方法，让 Spring 缓存正常工作
        Object result = joinPoint.proceed();

        // 执行后打印结果
//        logger.info("[AFTER EXECUTE] cacheName: " + cacheName + ", key: " + key + ", result type: " + (result != null ? result.getClass().getName() : "null") + ", result content: " + (result != null ? result.toString() : "null"));

        return result;
    }

    /**
     * 评估 SpEL key 表达式
     */
    private Object evaluateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (keyExpression == null || keyExpression.isEmpty()) {
            return "defaultKey"; // fallback
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setRootObject(joinPoint.getTarget());
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        return expressionParser.parseExpression(keyExpression).getValue(context);
    }
}