# Spring三级缓存原理详解

## 目录
- [1. 什么是Spring三级缓存](#1-什么是spring三级缓存)
- [2. 三级缓存的具体实现](#2-三级缓存的具体实现)
- [3. 循环依赖问题分析](#3-循环依赖问题分析)
- [4. 三级缓存解决循环依赖的流程](#4-三级缓存解决循环依赖的流程)
- [5. 为什么需要三级缓存而不是两级](#5-为什么需要三级缓存而不是两级)
- [6. Spring源码中的关键实现](#6-spring源码中的关键实现)
- [7. 自定义实现分析](#7-自定义实现分析)
- [8. 构造器注入与setter注入的区别](#8-构造器注入与setter注入的区别)
- [9. 面试常见问题及答案](#9-面试常见问题及答案)
- [10. 总结](#10-总结)

## 1. 什么是Spring三级缓存

Spring的三级缓存是Spring框架用来解决Bean之间循环依赖问题的一种机制。在Spring中，当两个或多个Bean互相依赖时（例如A依赖B，B又依赖A），如果没有特殊处理，会导致无限循环创建Bean的问题。

Spring通过三个不同级别的缓存来管理Bean的创建过程，从而优雅地解决了循环依赖问题：

| 缓存级别 | 名称 | 用途 | 存储内容 |
|---------|------|------|----------|
| 一级缓存 | singletonObjects | 存储完全初始化好的单例Bean | 已完成所有属性注入和初始化的最终Bean实例 |
| 二级缓存 | earlySingletonObjects | 存储早期暴露的Bean引用 | 已实例化但尚未完成属性注入的Bean实例 |
| 三级缓存 | singletonFactories | 存储Bean工厂对象 | 用于创建Bean早期引用的ObjectFactory |

## 2. 三级缓存的具体实现

在Spring源码中，三级缓存的实现在`DefaultSingletonBeanRegistry`类中，核心数据结构如下：

```java
// 一级缓存：存储完全初始化好的Bean
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

// 二级缓存：存储早期暴露的Bean引用（未完成属性注入）
private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

// 三级缓存：存储Bean工厂，用于生成Bean的早期引用
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

// 正在创建中的Bean集合
private final Set<String> singletonsCurrentlyInCreation = 
    Collections.newSetFromMap(new ConcurrentHashMap<>(16));
```

## 3. 循环依赖问题分析

循环依赖是指多个Bean之间相互依赖，形成闭环。例如：
- A依赖B
- B依赖A

或者更复杂的循环：
- A依赖B
- B依赖C
- C依赖A

在传统的对象创建过程中，这种情况会导致无限递归，最终导致栈溢出错误。

### 3.1 循环依赖的三种情况

1. **构造器循环依赖**：通过构造函数注入形成的循环依赖，Spring无法解决
2. **setter方法循环依赖**：通过setter方法注入形成的循环依赖，Spring可以通过三级缓存解决
3. **字段注入循环依赖**：通过@Autowired注解直接注入字段形成的循环依赖，本质上与setter注入类似

## 4. 三级缓存解决循环依赖的流程

下面以ServiceA和ServiceB互相依赖为例，详细说明三级缓存解决循环依赖的完整流程：

### 4.1 初始化ServiceA的过程

1. **检查缓存**：尝试从一级缓存singletonObjects中获取ServiceA，此时缓存为空
2. **标记创建中**：将ServiceA添加到singletonsCurrentlyInCreation集合中
3. **实例化**：调用构造函数创建ServiceA实例，但尚未设置属性
4. **暴露工厂**：将ServiceA的ObjectFactory添加到三级缓存singletonFactories
5. **注入依赖**：ServiceA需要注入ServiceB，开始创建ServiceB

### 4.2 初始化ServiceB的过程

1. **检查缓存**：尝试从一级缓存singletonObjects中获取ServiceB，此时缓存为空
2. **标记创建中**：将ServiceB添加到singletonsCurrentlyInCreation集合中
3. **实例化**：调用构造函数创建ServiceB实例，但尚未设置属性
4. **暴露工厂**：将ServiceB的ObjectFactory添加到三级缓存singletonFactories
5. **注入依赖**：ServiceB需要注入ServiceA，此时触发从缓存获取ServiceA

### 4.3 解决循环依赖的关键步骤

1. **查找ServiceA**：ServiceB尝试注入ServiceA时，首先检查一级缓存，没有找到
2. **检查早期引用**：检查二级缓存earlySingletonObjects，也没有找到
3. **使用工厂获取早期引用**：从三级缓存singletonFactories中获取ServiceA的ObjectFactory
4. **创建早期引用**：通过ObjectFactory获取ServiceA的早期引用（未完成属性注入的实例）
5. **升级缓存**：将ServiceA的早期引用从三级缓存移动到二级缓存earlySingletonObjects
6. **注入早期引用**：将ServiceA的早期引用注入到ServiceB中
7. **完成ServiceB初始化**：ServiceB完成属性注入和初始化，放入一级缓存singletonObjects
8. **返回ServiceA的初始化**：回到ServiceA的初始化过程，注入已经完全初始化的ServiceB
9. **完成ServiceA初始化**：ServiceA完成所有初始化，放入一级缓存singletonObjects

## 5. 为什么需要三级缓存而不是两级

这是面试中的高频问题。很多人会问：既然二级缓存已经可以存储早期引用，为什么还需要三级缓存？

### 5.1 AOP代理的考虑

三级缓存的关键作用是**支持AOP代理**。具体来说：

1. **延迟代理创建**：ObjectFactory允许在真正需要时才创建代理对象，而不是在Bean实例化后立即创建
2. **条件代理**：只有当Bean被其他Bean引用且需要代理时，才会通过工厂创建代理对象
3. **避免不必要的代理**：如果一个Bean没有被其他Bean循环依赖引用，就不会触发从三级缓存获取，从而避免了不必要的代理对象创建

### 5.2 二级缓存的局限性

如果只使用两级缓存：
- 在Bean实例化后，就需要决定是否创建代理对象
- 无论该Bean是否被其他Bean引用，都要创建代理
- 这会导致不必要的性能开销

### 5.3 三级缓存的优势

通过ObjectFactory，Spring实现了：
- **延迟加载**：只有在真正需要时才创建代理
- **灵活处理**：可以根据实际情况决定返回原始对象还是代理对象
- **性能优化**：避免了不必要的代理创建

## 6. Spring源码中的关键实现

### 6.1 getSingleton方法

`getSingleton`方法是Spring解决循环依赖的核心，它实现了从三级缓存中获取Bean的逻辑：

```java
@Nullable
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // 1. 从一级缓存获取
    Object singletonObject = this.singletonObjects.get(beanName);
    
    // 2. 如果一级缓存中没有且Bean正在创建中
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        synchronized (this.singletonObjects) {
            // 3. 从二级缓存获取
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                // 4. 从三级缓存获取工厂
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    // 5. 通过工厂获取早期引用
                    singletonObject = singletonFactory.getObject();
                    // 6. 升级到二级缓存
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    // 7. 从三级缓存移除
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}
```

### 6.2 addSingletonFactory方法

这个方法用于将Bean工厂添加到三级缓存：

```java
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(singletonFactory, "Singleton factory must not be null");
    synchronized (this.singletonObjects) {
        if (!this.singletonObjects.containsKey(beanName)) {
            this.singletonFactories.put(beanName, singletonFactory);
            this.earlySingletonObjects.remove(beanName);
            this.registeredSingletons.add(beanName);
        }
    }
}
```

### 6.3 doCreateBean方法中的关键步骤

在`AbstractAutowireCapableBeanFactory`的`doCreateBean`方法中，Spring完成Bean的创建、属性注入和初始化：

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    // 1. 实例化Bean
    BeanWrapper instanceWrapper = null;
    // ...
    instanceWrapper = createBeanInstance(beanName, mbd, args);
    Object bean = instanceWrapper.getWrappedInstance();
    
    // 2. 决定是否需要提前暴露对象引用
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
            isSingletonCurrentlyInCreation(beanName));
    if (earlySingletonExposure) {
        // 3. 暴露早期引用到三级缓存
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }
    
    // 4. 初始化Bean实例
    Object exposedObject = bean;
    try {
        // 5. 注入依赖
        populateBean(beanName, mbd, instanceWrapper);
        // 6. 调用初始化方法
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    } catch (Throwable ex) {
        // ...异常处理
    }
    
    // 7. 处理循环引用和代理
    // ...
    
    return exposedObject;
}
```

## 7. 自定义实现分析

在我们的自定义实现中，我们模拟了Spring的三级缓存机制，主要包含以下几个核心组件：

### 7.1 CustomObjectFactory接口

模拟Spring的ObjectFactory，用于创建Bean的早期引用：

```java
public interface CustomObjectFactory<T> {
    T getObject();
}
```

### 7.2 CustomSingletonBeanRegistry类

实现了三级缓存的核心逻辑：
- 三个不同级别的缓存Map
- 管理正在创建中的Bean集合
- 提供getSingleton、registerSingleton和addSingletonFactory等核心方法

### 7.3 CustomBeanFactory类

模拟Spring的BeanFactory，负责：
- 管理Bean定义
- 创建和初始化Bean
- 处理依赖注入
- 与三级缓存协作解决循环依赖

## 8. 构造器注入与setter注入的区别

### 8.1 构造器注入

- **优势**：
  - 保证依赖不可变（final关键字）
  - 保证依赖不为null
  - 构造完成后对象即完全初始化
  - 更符合面向对象的设计原则

- **劣势**：
  - 无法解决循环依赖问题
  - 构造函数参数过多时代码可读性差

### 8.2 setter注入

- **优势**：
  - 可以解决循环依赖问题
  - 更灵活，可以在对象创建后再设置依赖
  - 适合可选依赖

- **劣势**：
  - 无法保证依赖在使用时已被设置
  - 可能导致部分初始化的对象

### 8.3 Spring对两种注入方式的处理

- **构造器注入的循环依赖**：Spring无法解决，会抛出异常
- **setter注入的循环依赖**：Spring可以通过三级缓存机制解决

## 9. 面试常见问题及答案

### 9.1 Spring如何解决循环依赖？

**答案**：Spring通过三级缓存机制解决循环依赖问题：

1. **一级缓存(singletonObjects)**：存储完全初始化好的Bean
2. **二级缓存(earlySingletonObjects)**：存储早期暴露的Bean引用
3. **三级缓存(singletonFactories)**：存储Bean工厂对象

当出现循环依赖时，Spring会在Bean实例化后但未完成属性注入前，将Bean的工厂对象放入三级缓存。当另一个Bean需要依赖该Bean时，会通过工厂获取早期引用，并将其升级到二级缓存，从而解决循环依赖问题。

### 9.2 为什么需要三级缓存而不是两级？

**答案**：三级缓存的核心作用是支持AOP代理。ObjectFactory允许在真正需要时才创建代理对象，而不是在Bean实例化后立即创建。这样可以避免不必要的代理创建，提高性能。

如果只有两级缓存，Spring需要在Bean实例化后立即决定是否创建代理，无论该Bean是否被其他Bean循环依赖引用。通过三级缓存，Spring实现了延迟代理创建的策略。

### 9.3 构造器注入的循环依赖为什么无法解决？

**答案**：构造器注入的循环依赖无法解决的原因是：在构造器注入的情况下，Bean在实例化阶段就需要依赖对象完全创建好。当A的构造器需要B，而B的构造器需要A时，就会陷入死循环。

Spring的三级缓存机制只能解决setter注入的循环依赖，因为setter注入允许先创建实例，再注入依赖。

### 9.4 能否详细描述一下三级缓存解决循环依赖的完整流程？

**答案**：以A依赖B，B依赖A为例：

1. 创建A时，先将A标记为正在创建中
2. 实例化A（调用构造函数）
3. 将A的工厂对象放入三级缓存
4. A需要注入B，开始创建B
5. 创建B时，同样先标记为正在创建中
6. 实例化B（调用构造函数）
7. 将B的工厂对象放入三级缓存
8. B需要注入A，从三级缓存获取A的工厂对象
9. 通过工厂获取A的早期引用，放入二级缓存
10. 从三级缓存移除A的工厂对象
11. B完成属性注入和初始化，放入一级缓存
12. 回到A，完成B的注入和A的初始化
13. A放入一级缓存

### 9.5 Spring中singletonObjects、earlySingletonObjects和singletonFactories的区别？

**答案**：
- **singletonObjects**：一级缓存，存储完全初始化好的单例Bean，这些Bean已经完成了所有属性注入和初始化过程
- **earlySingletonObjects**：二级缓存，存储早期暴露的Bean引用，这些Bean已经实例化但尚未完成属性注入
- **singletonFactories**：三级缓存，存储Bean工厂对象，用于在需要时创建Bean的早期引用或代理对象

这三个缓存的优先级依次是：singletonObjects > earlySingletonObjects > singletonFactories

### 9.6 如果Spring没有三级缓存机制，会有什么问题？

**答案**：
1. **无法解决循环依赖**：特别是setter注入的循环依赖问题
2. **AOP代理处理复杂**：难以在Bean创建过程中正确处理代理对象
3. **性能问题**：可能导致不必要的代理对象创建
4. **初始化顺序混乱**：Bean的初始化过程可能变得复杂且难以控制

### 9.7 Spring中的循环依赖有哪些类型，哪些可以解决，哪些不能？

**答案**：

1. **构造器注入循环依赖**：无法解决，Spring会抛出异常
2. **setter注入循环依赖**：可以通过三级缓存解决
3. **字段注入循环依赖**：本质上与setter注入类似，可以解决

### 9.8 Spring是如何处理代理对象的循环依赖的？

**答案**：Spring通过ObjectFactory和三级缓存机制处理代理对象的循环依赖：

1. 当Bean需要被代理时，ObjectFactory会在被调用时创建代理对象
2. 这个过程发生在从三级缓存获取Bean引用时
3. 代理对象会被放入二级缓存，确保后续获取到的是同一个代理实例
4. 这样可以保证无论是直接获取还是循环依赖获取，得到的都是相同的代理对象

## 10. 总结

Spring的三级缓存机制是一个精巧的设计，它有效地解决了Bean之间的循环依赖问题，同时也支持了AOP代理等高级特性。理解三级缓存的工作原理，对于深入理解Spring框架的核心机制和应对面试都非常重要。

三级缓存的核心思想是：**在Bean完全初始化之前，提前暴露其引用，从而允许其他Bean引用它，即使它还没有完全初始化完成**。通过ObjectFactory的延迟创建机制，Spring还实现了对AOP代理的灵活支持。

在实际开发中，我们应该尽量避免循环依赖的设计，因为它通常表示系统设计存在问题。但理解Spring如何解决循环依赖，有助于我们更好地理解Spring的工作原理，以及在必要时正确使用Spring的依赖注入功能。