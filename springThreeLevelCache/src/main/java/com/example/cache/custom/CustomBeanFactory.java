package com.example.cache.custom;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义Bean工厂，用于创建和管理Bean
 * 模拟Spring的BeanFactory，使用三级缓存解决循环依赖
 */
public class CustomBeanFactory {
    
    // 使用我们的自定义单例Bean注册表
    private final CustomSingletonBeanRegistry singletonRegistry = new CustomSingletonBeanRegistry();
    
    // 存储Bean定义的映射
    private final Map<String, BeanDefinition<?>> beanDefinitions = new HashMap<>();
    
    /**
     * 注册Bean定义
     * @param beanName Bean名称
     * @param beanClass Bean类
     * @param dependencies 依赖的Bean名称数组
     */
    public <T> void registerBean(String beanName, Class<T> beanClass, String... dependencies) {
        BeanDefinition<T> definition = new BeanDefinition<>(beanClass, dependencies);
        beanDefinitions.put(beanName, definition);
    }
    
    /**
     * 获取Bean实例
     * @param beanName Bean名称
     * @return Bean实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName) {
        // 1. 先尝试从单例缓存中获取
        Object bean = singletonRegistry.getSingleton(beanName);
        if (bean != null) {
            return (T) bean;
        }
        
        // 2. 如果缓存中没有，开始创建Bean
        BeanDefinition<T> definition = (BeanDefinition<T>) beanDefinitions.get(beanName);
        if (definition == null) {
            throw new RuntimeException("No bean named '" + beanName + "' available");
        }
        
        try {
            // 3. 标记Bean为正在创建中
            singletonRegistry.beforeSingletonCreation(beanName);
            
            // 4. 创建Bean实例
            T instance = createBean(definition);
            
            // 5. 添加到三级缓存，用于解决循环依赖
            singletonRegistry.addSingletonFactory(beanName, () -> instance);
            
            // 6. 注入依赖
            injectDependencies(instance, definition);
            
            // 7. 初始化Bean
            initializeBean(instance);
            
            // 8. 注册到单例缓存（一级缓存）
            singletonRegistry.registerSingleton(beanName, instance);
            
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error creating bean with name '" + beanName + "': " + e.getMessage(), e);
        } finally {
            // 9. 标记Bean创建完成
            singletonRegistry.afterSingletonCreation(beanName);
        }
    }
    
    /**
     * 创建Bean实例
     */
    private <T> T createBean(BeanDefinition<T> definition) throws Exception {
        System.out.println("Creating bean instance for: " + definition.getBeanClass().getSimpleName());
        return definition.getBeanClass().getDeclaredConstructor().newInstance();
    }
    
    /**
     * 注入依赖
     */
    private <T> void injectDependencies(T instance, BeanDefinition<T> definition) throws Exception {
        String[] dependencies = definition.getDependencies();
        if (dependencies != null) {
            for (String dependencyName : dependencies) {
                System.out.println("Injecting dependency: " + dependencyName + " into " + 
                                  instance.getClass().getSimpleName());
                // 获取依赖的Bean实例
                Object dependency = getBean(dependencyName);
                
                // 简化的依赖注入实现
                // 实际Spring中会使用反射查找setter方法或字段
                // 这里我们假设Bean类中包含对应名称的setter方法
                String setterName = "set" + dependencyName.substring(0, 1).toUpperCase() + 
                                   dependencyName.substring(1);
                
                try {
                    // 查找setter方法并调用
                    instance.getClass().getMethod(setterName, dependency.getClass()).invoke(instance, dependency);
                } catch (NoSuchMethodException e) {
                    // 如果找不到setter方法，尝试直接设置字段
                    try {
                        java.lang.reflect.Field field = instance.getClass().getDeclaredField(dependencyName);
                        field.setAccessible(true);
                        field.set(instance, dependency);
                    } catch (NoSuchFieldException ignored) {
                        // 如果字段也不存在，忽略该依赖
                        System.out.println("Cannot inject dependency: " + dependencyName + 
                                          " - no setter or field found");
                    }
                }
            }
        }
    }
    
    /**
     * 初始化Bean
     */
    private <T> void initializeBean(T instance) {
        System.out.println("Initializing bean: " + instance.getClass().getSimpleName());
        // 实际Spring中会调用InitializingBean接口或init-method
    }
    
    /**
     * Bean定义内部类
     */
    private static class BeanDefinition<T> {
        private final Class<T> beanClass;
        private final String[] dependencies;
        
        public BeanDefinition(Class<T> beanClass, String[] dependencies) {
            this.beanClass = beanClass;
            this.dependencies = dependencies;
        }
        
        public Class<T> getBeanClass() {
            return beanClass;
        }
        
        public String[] getDependencies() {
            return dependencies;
        }
    }
}