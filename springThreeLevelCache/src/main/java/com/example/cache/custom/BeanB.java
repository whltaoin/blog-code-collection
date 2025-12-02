package com.example.cache.custom;

/**
 * 示例BeanB，与BeanA形成循环依赖
 */
public class BeanB {
    
    private BeanA beanA;
    
    public BeanB() {
        System.out.println("BeanB constructor called");
    }
    
    // Setter方法用于依赖注入
    public void setBeanA(BeanA beanA) {
        this.beanA = beanA;
        System.out.println("BeanB injected with BeanA");
    }
    
    public String getMessage() {
        return "Hello from BeanB, using " + (beanA != null ? "injected BeanA" : "null BeanA");
    }
    
    public BeanA getBeanA() {
        return beanA;
    }
}