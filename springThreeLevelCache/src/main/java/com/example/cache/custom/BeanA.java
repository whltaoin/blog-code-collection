package com.example.cache.custom;

/**
 * 示例BeanA，与BeanB形成循环依赖
 */
public class BeanA {
    
    private BeanB beanB;
    
    public BeanA() {
        System.out.println("BeanA constructor called");
    }
    
    // Setter方法用于依赖注入
    public void setBeanB(BeanB beanB) {
        this.beanB = beanB;
        System.out.println("BeanA injected with BeanB");
    }
    
    public String getMessage() {
        return "Hello from BeanA, using " + (beanB != null ? "injected BeanB" : "null BeanB");
    }
    
    public BeanB getBeanB() {
        return beanB;
    }
}