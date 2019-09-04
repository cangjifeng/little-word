​                                                                             **dozer详情**

# dozer说明

​        A对象和B对象的属性完全相同，比如MemberVo和MemberBean，vo是对外暴露和bean是内部操作，入参地方需要将vo转换成bean，出参地方需要将bean转换成vo。创建的几种做法，使用对象的get和set方法进行转换，使用Json进行操作，使用反射获取对象的属性并赋值给另一个对象中的对应属性（JSON内部实现是基于此的），还有一种方法，使用dozer。dozer比使用get、set方法方便，比使用json方式性能好些（性能最好的是使用get、set方法，但是遇到复杂对象或者对象属性多时候很繁琐）。

使用方式

# maven依赖

5.5.1版本之前是在 net.sf.dozer

```java
<!-- https://mvnrepository.com/artifact/net.sf.dozer/dozer -->
<dependency>
    <groupId>net.sf.dozer</groupId>
    <artifactId>dozer</artifactId>
    <version>5.5.1</version>
</dependency>

```

6.x.x 版本之后是在 com.github.dozermapper 上

```java
<!-- https://mvnrepository.com/artifact/com.github.dozermapper/dozer-core -->
<dependency>
    <groupId>com.github.dozermapper</groupId>
    <artifactId>dozer-core</artifactId>
    <version>6.0.0</version>
</dependency>
```

# Mapper对象初始化

6.x版本前的 DozerBeanMapper 类有一个无参的构造函数，6.x 版本中取消了无参的构造函数。

```java
Mapper mapper = new DozerBeanMapper(); // 5.5.1以及之前的版本初始化方式
Mapper mapper = DozerBeanMapperBuilder.create().build(); // 6.x版本开始初始化方法
```



```java
package org.jerfan.sky.jvm.gc.dozer;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jerfan.cang
 * @date 2019/9/3  15:17
 */
@Configuration
public class DozerConfig {

    @Bean(value = "dozerMapper")
    public Mapper initMapper(){
        return  DozerBeanMapperBuilder.create().build(); //6.0.0 版本
        //return new DozerBeanMapper(); // 5.5.1 版本
    }
}

```



# 对象转换

​        调用 Mapper 的map() 方法，它有4个重载的方法，支持对象2Class、对象2对象的转换方式。

​        支持的数据类型如下：

- [ ] ​        基本数据类型
- [ ] ​        引用类型（Class）类型
- [ ] ​        List<T> 集合类型
- [ ] ​        Map<T>集合类型
- [ ] ​        Set<T>集合类型
- [ ] ​        Enum 类型
- [ ] ​        String 2 Date

​        转换的案例，更多的使用参考dozer-user-guide.pdf(git目录同级下)

```java
MemberBean bean = mapper.map(vo,MemberBean.class);
// 如果memberBeanList 为null 会报 MappingException
List<MemberBean> memberBeanList = new ArrayList<>();       
mapper.map(memberVoList,memberBeanList);
```



# dozer 转换核心源码

## Mapper 接口定义

```java
/*
 * Copyright 2005-2019 Dozer Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dozermapper.core;

import com.github.dozermapper.core.metadata.MappingMetadata;

/**
 * Public root interface for performing Dozer mappings from application code.
 */
public interface Mapper {

    /**
     * Constructs new instance of destinationClass and performs mapping between from source
     *
     * @param source           object to convert from
     * @param destinationClass type to convert to
     * @param <T>              type to convert to
     * @return mapped object
     * @throws MappingException mapping failure
     */
    <T> T map(Object source, Class<T> destinationClass) throws MappingException;

    /**
     * Performs mapping between source and destination objects
     *
     * @param source      object to convert from
     * @param destination object to convert to
     * @throws MappingException mapping failure
     */
    void map(Object source, Object destination) throws MappingException;

    /**
     * Constructs new instance of destinationClass and performs mapping between from source
     *
     * @param source           object to convert from
     * @param destinationClass type to convert to
     * @param mapId            id in configuration for mapping
     * @param <T>              type to convert to
     * @return mapped object
     * @throws MappingException mapping failure
     */
    <T> T map(Object source, Class<T> destinationClass, String mapId) throws MappingException;

    /**
     * Performs mapping between source and destination objects
     *
     * @param source      object to convert from
     * @param destination object to convert to
     * @param mapId       id in configuration for mapping
     * @throws MappingException mapping failure
     */
    void map(Object source, Object destination, String mapId) throws MappingException;

    /**
     * The {@link com.github.dozermapper.core.metadata.MappingMetadata} interface can be used to query information about the current
     * mapping definitions. It provides read only access to all important classes and field
     * mapping properties. When first called, initializes all mappings if map() has not yet been called.
     *
     * @return An instance of {@link com.github.dozermapper.core.metadata.MappingMetadata} which serves starting point
     * for querying mapping information.
     */
    default MappingMetadata getMappingMetadata() {
        return MappingMetadata.EMPTY;
    }

    /**
     * Returns {@link MapperModelContext} which allows readonly access to the Mapper model
     *
     * @return an instance of {@link MapperModelContext}
     */
    MapperModelContext getMapperModelContext();
}

```



## Mapper 接口的实现类

​        Mapper接口有两个实现类 DozerBeanMapper 和 MappingProcessor ，在DozerBeanMapper 类中有一个方法是获取MappingProcessor 实例的方法 getMappingProcessor() 。

```
protected Mapper getMappingProcessor() {
        Mapper processor = new MappingProcessor(customMappings, globalConfiguration, cacheManager, customConverters,
                                                eventManager, customFieldMapper, customConvertersWithId, beanContainer, destBeanCreator, destBeanBuilderCreator,
                                                beanMappingGenerator, propertyDescriptorFactory);

        return processor;
    }
```

## map() 方法的核心代码

​        map() 方法的内部实现核心代码是调用 mapGeneral() 方法。

​        mapGeneral() 方法的实现如下：

```java
/**
     * Single point of entry for atomic mapping operations
     *
     * @param srcObj    source object
     * @param destClass destination class
     * @param destObj   destination object
     * @param mapId     mapping identifier
     * @param <T>       destination object type
     * @return new or updated destination object
     */
    private <T> T mapGeneral(Object srcObj, final Class<T> destClass, final T destObj, final String mapId) {
        srcObj = MappingUtils.deProxy(srcObj, beanContainer);

        Class<T> destType;
        T result;
        if (destClass == null) {
            destType = (Class<T>)destObj.getClass();
            result = destObj;
        } else {
            destType = destClass;
            result = null;
        }

        ClassMap classMap = null;
        try {
            classMap = getClassMap(srcObj.getClass(), destType, mapId);


            eventManager.on(new DefaultEvent(EventTypes.MAPPING_STARTED, classMap, null, srcObj, result, null));

            // TODO Check if any proxy issues are here
            // Check to see if custom converter has been specified for this mapping
            // combination. If so, just use it.
            Class<?> converterClass = MappingUtils.findCustomConverter(converterByDestTypeCache, classMap.getCustomConverters(), srcObj
                    .getClass(), destType);

            if (destObj == null) {
                // If this is a nested MapperAware conversion this mapping can be already processed
                // but we can do this optimization only in case of no destObject, instead we must copy to the dest object
                Object alreadyMappedValue = mappedFields.getMappedValue(srcObj, destType, mapId);
                if (alreadyMappedValue != null) {
                    return (T)alreadyMappedValue;
                }
            }

            if (converterClass != null) {
                return (T)mapUsingCustomConverter(converterClass, srcObj.getClass(), srcObj, destType, result, null, true);
            }

            BeanCreationDirective creationDirective =
                    new BeanCreationDirective(srcObj, classMap.getSrcClassToMap(), classMap.getDestClassToMap(), destType,
                                              classMap.getDestClassBeanFactory(), classMap.getDestClassBeanFactoryId(), classMap.getDestClassCreateMethod(),
                                              classMap.getDestClass().isSkipConstructor());

            result = createByCreationDirectiveAndMap(creationDirective, classMap, srcObj, result, false, null);
        } catch (Throwable e) {
            MappingUtils.throwMappingException(e);
        }
        eventManager.on(new DefaultEvent(EventTypes.MAPPING_FINISHED, classMap, null, srcObj, result, null));

        return result;
    }
```







