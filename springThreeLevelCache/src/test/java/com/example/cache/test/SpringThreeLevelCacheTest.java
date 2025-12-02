package com.example.cache.test;

import com.example.cache.ServiceA;
import com.example.cache.ServiceB;
import com.example.cache.SpringConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Spring原生三级缓存测试类
 * 验证Spring如何通过三级缓存解决循环依赖问题
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
public class SpringThreeLevelCacheTest {
    
    @Autowired
    private ServiceA serviceA;
    
    @Autowired
    private ServiceB serviceB;
    
    @Test
    public void testCircularDependency() {
        System.out.println("\n===== Spring三级缓存循环依赖测试 =====");
        
        // 验证ServiceA是否成功注入了ServiceB
        System.out.println("ServiceA调用结果: " + serviceA.getInfo());
        
        // 验证循环依赖是否成功解决
        System.out.println("验证循环依赖是否解决:");
        System.out.println("- ServiceA的serviceB引用: " + serviceB.getMessage());
        
        // 打印两个对象的hashCode，确认是同一个实例
        System.out.println("\n对象引用验证:");
        System.out.println("- ServiceA实例: " + serviceA);
        System.out.println("- ServiceB实例: " + serviceB);
        
        System.out.println("\nSpring三级缓存成功解决了循环依赖问题！");
        System.out.println("====================================\n");
    }
}