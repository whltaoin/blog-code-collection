package com.example.cache.custom;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义单例Bean注册表，实现Spring的三级缓存机制
 * 模拟Spring的DefaultSingletonBeanRegistry
 */
public class CustomSingletonBeanRegistry {
    
    // 一级缓存：存储完全初始化好的单例Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    
    // 二级缓存：存储早期曝光的Bean实例（尚未完成属性注入）
    private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);
    
    // 三级缓存：存储Bean工厂对象，用于生成Bean的早期引用
    private final Map<String, CustomObjectFactory<?>> singletonFactories = new HashMap<>(16);
    
    // 正在创建中的Bean集合
    private final Set<String> singletonsCurrentlyInCreation = 
        Collections.newSetFromMap(new ConcurrentHashMap<>(16));
    
    // 已注册的Bean名称集合
    private final Set<String> registeredSingletons = 
        Collections.newSetFromMap(new ConcurrentHashMap<>(256));
    
    /**
     * 获取单例Bean
     * @param beanName Bean名称
     * @return Bean实例
     */
    @SuppressWarnings("unchecked")
    public Object getSingleton(String beanName) {
        // 1. 先从一级缓存获取
        Object singletonObject = this.singletonObjects.get(beanName);
        
        // 2. 如果一级缓存中不存在，且当前Bean正在创建中
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            synchronized (this.singletonObjects) {
                // 3. 从二级缓存获取早期引用
                singletonObject = this.earlySingletonObjects.get(beanName);
                if (singletonObject == null) {
                    // 4. 从三级缓存获取工厂对象
                    CustomObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                    if (singletonFactory != null) {
                        // 5. 通过工厂获取早期Bean引用
                        singletonObject = singletonFactory.getObject();
                        // 6. 将早期引用放入二级缓存
                        this.earlySingletonObjects.put(beanName, singletonObject);
                        // 7. 从三级缓存移除工厂对象
                        this.singletonFactories.remove(beanName);
                    }
                }
            }
        }
        return singletonObject;
    }
    
    /**
     * 注册单例Bean（完全初始化后的Bean）
     * @param beanName Bean名称
     * @param singletonObject Bean实例
     */
    public void registerSingleton(String beanName, Object singletonObject) {
        synchronized (this.singletonObjects) {
            // 放入一级缓存
            this.singletonObjects.put(beanName, singletonObject);
            // 从二级缓存移除
            this.earlySingletonObjects.remove(beanName);
            // 从三级缓存移除
            this.singletonFactories.remove(beanName);
            // 注册Bean名称
            this.registeredSingletons.add(beanName);
        }
    }
    
    /**
     * 添加单例工厂（用于创建早期引用）
     * @param beanName Bean名称
     * @param singletonFactory Bean工厂
     */
    public void addSingletonFactory(String beanName, CustomObjectFactory<?> singletonFactory) {
        synchronized (this.singletonObjects) {
            // 只有当一级缓存中不存在时，才添加到三级缓存
            if (!this.singletonObjects.containsKey(beanName)) {
                this.singletonFactories.put(beanName, singletonFactory);
                this.earlySingletonObjects.remove(beanName);
            }
        }
    }
    
    /**
     * 标记Bean为正在创建中
     * @param beanName Bean名称
     */
    public void beforeSingletonCreation(String beanName) {
        if (!this.inCreationCheckExclusions.contains(beanName) && 
            !this.singletonsCurrentlyInCreation.add(beanName)) {
            throw new RuntimeException("Bean with name '" + beanName + "' is currently in creation: " +
                "Is there an unresolvable circular reference?");
        }
    }
    
    /**
     * 标记Bean创建完成
     * @param beanName Bean名称
     */
    public void afterSingletonCreation(String beanName) {
        if (!this.inCreationCheckExclusions.contains(beanName) && 
            !this.singletonsCurrentlyInCreation.remove(beanName)) {
            throw new RuntimeException("Singleton '" + beanName + "' isn't currently in creation");
        }
    }
    
    /**
     * 检查Bean是否正在创建中
     * @param beanName Bean名称
     * @return 是否正在创建中
     */
    public boolean isSingletonCurrentlyInCreation(String beanName) {
        return this.singletonsCurrentlyInCreation.contains(beanName);
    }
    
    // 排除检查的Bean名称集合
    private final Set<String> inCreationCheckExclusions = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
}