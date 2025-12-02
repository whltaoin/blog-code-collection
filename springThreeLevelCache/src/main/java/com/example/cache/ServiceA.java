package com.example.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceA {
    
    private ServiceB serviceB;
    
    public ServiceA() {
        System.out.println("ServiceA构造函数被调用");
    }
    
    @Autowired
    public void setServiceB(ServiceB serviceB) {
        System.out.println("ServiceA的setServiceB方法被调用");
        this.serviceB = serviceB;
    }
    
    public String getInfo() {
        return "ServiceA调用ServiceB: " + serviceB.getMessage();
    }
}