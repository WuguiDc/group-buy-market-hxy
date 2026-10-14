package com.hxy.types.design.framework.tree;
/**
 * @Description: 策略处理器
 * @Author: hxy
 * @Date: 2026/10/11
 */


public interface StrategyHandler<T,D,R> {

    StrategyHandler DEFAULT= (T,D)->null;

    R apply(T requestParameter,D dynamicContext) throws Exception;


}
