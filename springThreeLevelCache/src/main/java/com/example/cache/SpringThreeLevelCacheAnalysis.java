package com.example.cache;

/**
 * Spring三级缓存机制分析
 * 
 * Spring通过三级缓存解决循环依赖问题：
 * 1. 一级缓存（singletonObjects）：存储完全初始化好的单例Bean
 * 2. 二级缓存（earlySingletonObjects）：存储早期曝光的Bean实例，尚未完成属性注入
 * 3. 三级缓存（singletonFactories）：存储Bean工厂对象，用于生成Bean的早期引用
 */
public class SpringThreeLevelCacheAnalysis {
    
    /**
     * Spring三级缓存的核心实现位于DefaultSingletonBeanRegistry类中，主要包含：
     * 
     * // 一级缓存：存储完全初始化好的Bean
     * private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
     * 
     * // 二级缓存：存储早期暴露的Bean引用（未完成属性注入）
     * private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);
     * 
     * // 三级缓存：存储Bean工厂，用于生成Bean的早期引用
     * private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
     * 
     * // 正在创建中的Bean集合
     * private final Set<String> singletonsCurrentlyInCreation = 
     *     Collections.newSetFromMap(new ConcurrentHashMap<>(16));
     */
    
    /**
     * 循环依赖解决流程：
     * 1. 创建ServiceA时，先将ServiceA加入singletonsCurrentlyInCreation集合
     * 2. 实例化ServiceA（调用构造函数）
     * 3. 将ServiceA的工厂对象放入三级缓存singletonFactories
     * 4. ServiceA需要注入ServiceB，开始创建ServiceB
     * 5. 创建ServiceB时，同样先加入singletonsCurrentlyInCreation集合
     * 6. 实例化ServiceB（调用构造函数）
     * 7. 将ServiceB的工厂对象放入三级缓存singletonFactories
     * 8. ServiceB需要注入ServiceA，从三级缓存获取ServiceA的工厂对象
     * 9. 通过工厂获取ServiceA的早期引用，放入二级缓存earlySingletonObjects
     * 10. 从三级缓存移除ServiceA的工厂对象
     * 11. ServiceB完成属性注入和初始化，放入一级缓存singletonObjects
     * 12. 回到ServiceA，完成ServiceB的注入和ServiceA的初始化
     * 13. ServiceA放入一级缓存singletonObjects
     */
    
    /**
     * 三级缓存存在的必要性：
     * 1. 三级缓存允许在Bean初始化完成前暴露其引用
     * 2. 解决了构造器循环依赖问题（基于setter注入）
     * 3. 支持AOP代理，通过ObjectFactory可以在需要时创建代理对象
     * 4. 避免了不必要的代理对象创建
     */
    
    public static void main(String[] args) {
        // 在测试类中演示完整流程
    }
}