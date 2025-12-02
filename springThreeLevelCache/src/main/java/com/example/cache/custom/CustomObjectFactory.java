package com.example.cache.custom;

/**
 * 自定义对象工厂接口
 * 用于创建Bean的早期引用，类似于Spring中的ObjectFactory
 */
public interface CustomObjectFactory<T> {
    
    /**
     * 获取Bean实例
     * @return Bean实例
     */
    T getObject();
}