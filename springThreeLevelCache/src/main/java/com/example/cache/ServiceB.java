package com.example.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceB {
    
    private ServiceA serviceA;
    
    public ServiceB() {
        System.out.println("ServiceB构造函数被调用");
    }
    
    @Autowired
    public void setServiceA(ServiceA serviceA) {
        System.out.println("ServiceB的setServiceA方法被调用");
        this.serviceA = serviceA;
    }
    
    public String getMessage() {
        return "Hello from ServiceB";
    }
}