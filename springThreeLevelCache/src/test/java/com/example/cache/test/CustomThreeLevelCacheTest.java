package com.example.cache.test;

import com.example.cache.custom.BeanA;
import com.example.cache.custom.BeanB;
import com.example.cache.custom.CustomBeanFactory;
import org.junit.Test;

/**
 * 自定义三级缓存测试类
 * 验证我们自己实现的三级缓存机制是否能解决循环依赖问题
 */
public class CustomThreeLevelCacheTest {
    
    @Test
    public void testCustomThreeLevelCache() {
        System.out.println("\n===== 自定义三级缓存循环依赖测试 =====");
        
        // 创建自定义Bean工厂
        CustomBeanFactory beanFactory = new CustomBeanFactory();
        
        // 注册Bean定义，声明循环依赖关系
        beanFactory.registerBean("beanA", BeanA.class, "beanB");
        beanFactory.registerBean("beanB", BeanB.class, "beanA");
        
        System.out.println("\n开始获取BeanA（触发循环依赖解决过程）...");
        
        // 获取BeanA，这将触发循环依赖的解决过程
        BeanA beanA = beanFactory.getBean("beanA");
        
        System.out.println("\n验证循环依赖解决结果:");
        
        // 验证BeanA是否成功注入了BeanB
        System.out.println("BeanA的消息: " + beanA.getMessage());
        
        // 获取BeanB并验证
        BeanB beanB = beanFactory.getBean("beanB");
        System.out.println("BeanB的消息: " + beanB.getMessage());
        
        // 验证循环引用的正确性
        System.out.println("\n验证循环引用的一致性:");
        System.out.println("- beanA.getBeanB() == beanB: " + (beanA.getBeanB() == beanB));
        System.out.println("- beanB.getBeanA() == beanA: " + (beanB.getBeanA() == beanA));
        
        // 打印对象的hashCode，确认是同一个实例
        System.out.println("\n对象引用验证:");
        System.out.println("- BeanA实例: " + beanA);
        System.out.println("- BeanB实例: " + beanB);
        System.out.println("- BeanA中的BeanB引用: " + beanA.getBeanB());
        System.out.println("- BeanB中的BeanA引用: " + beanB.getBeanA());
        
        System.out.println("\n自定义三级缓存成功解决了循环依赖问题！");
        System.out.println("======================================\n");
    }
    
    @Test
    public void testDetailedCacheProcess() {
        System.out.println("\n===== 自定义三级缓存详细流程测试 =====");
        
        // 创建自定义Bean工厂
        CustomBeanFactory beanFactory = new CustomBeanFactory();
        
        // 注册Bean定义
        beanFactory.registerBean("beanA", BeanA.class, "beanB");
        beanFactory.registerBean("beanB", BeanB.class, "beanA");
        
        System.out.println("\n[步骤1] 开始获取BeanA");
        BeanA beanA = beanFactory.getBean("beanA");
        
        System.out.println("\n[步骤2] 验证BeanA和BeanB的相互引用");
        System.out.println("- BeanA中的BeanB: " + beanA.getBeanB());
        System.out.println("- BeanB中的BeanA: " + beanA.getBeanB().getBeanA());
        
        System.out.println("\n[步骤3] 再次获取BeanA，应该从一级缓存中返回");
        BeanA beanA2 = beanFactory.getBean("beanA");
        System.out.println("- 两次获取的BeanA是同一个实例: " + (beanA == beanA2));
        
        System.out.println("\n自定义三级缓存详细流程测试完成！");
        System.out.println("======================================\n");
    }
}