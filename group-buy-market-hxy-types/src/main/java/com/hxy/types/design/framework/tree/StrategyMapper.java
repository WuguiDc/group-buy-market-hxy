package com.hxy.types.design.framework.tree;
/**
* @Description: 策略映射器
* @Author: hxy
* @Date: 2026/10/11
*/
public interface StrategyMapper<T,D,R> {

    StrategyHandler<T,D,R> get(T requestParameter,D dynamicContext) throws Exception;

}
