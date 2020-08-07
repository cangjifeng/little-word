

手撕一个spring-boot-starter-XXX

原理 ：

```
@ConfigurationProperties

@Configuration 

@EnableConfigurationProperties

@SpringBootApplication 

# 熟悉如上几个注解的作用，以及spring boot 工程启动流程 ，自行悟出其原理 
# spring boot 自动配置 ，可以看 我绘制的 springboot自动配置生效图解.png ,丑了点，但能看。
# 熟悉spring boot 加载启动的条件限制注解 如 @ConditionalOnMissingBean 还有好几个哦
# 源码
```

实现流程

1 构建一个spring-boot-starter-xxx 工程 ，示例使用的是 spring-boot-starter-rose .

​        spring-boot-starter-rose 完成的内容，动态的实例化 Rose的试下，Rose 有2个实现，DefaultRose 和 SpecialRose 。使用方配置 default 则实例化出 DefaultRose ，配置 special 则实例化出 SpecialRose 。说明：Rose工程相关的可以是独立jar，spring-boot-starter-rose 依赖它。为了简单示例是写在了一起。

2 需要依赖 spring boot 的2个 jar 包 

```
# pom.xml 
       <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <version>2.2.3.RELEASE</version>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <version>2.2.3.RELEASE</version>
        </dependency>
```

3 RoseProperties 类 ，这是一个配置类，spring boot 工程启动会把其全局配置信息会映射到这个类中【按需要进行映射】。

```
# RoseProperties.java 定义
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "rose")
public class RoseProperties {
    /**
     * 实例名称匹配,用于确定实例化 DefaultRose 还是 SpecialRose
     */
    private String name="default";
    /**
     * 给 Rose 实例化中的属性赋值 ，演示的是application.yml 属性传递
     */
    private String nickName="defaultRose";
    /**
     * 版本控制， 暂无意义
     */
    private String version="1.0.0";
    /**
     * 和 nickName 类似，
     */
    private long size=10;
    //省略 get和set , 推荐使用  @Data 注解
}
```

4 RoseAutoConfiguration 自动配置选择，

```
#RoseAutoConfiguration.java

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RoseProperties.class)
public class RoseAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DefaultRose.class)
    @ConditionalOnProperty(prefix = "rose",
            name = "name",
            havingValue = "default")
    DefaultRose getDefaultRose(RoseProperties pro){
        System.out.println(pro);
        return new DefaultRose(pro.getNickName(),pro.getSize());
    }

    @Bean
    @ConditionalOnMissingBean(SpecialRose.class)
    @ConditionalOnProperty(prefix = "rose",
            name = "name",
            havingValue = "special")
    SpecialRose getSpecialRose(RoseProperties pro){
        System.out.println(pro);
        return new SpecialRose(pro.getNickName(),pro.getSize());
    }

}
```

5 spring.factories , 让 RoseAutoConfiguration 生效，则需要遵循spring 和 spring boot 自动配置的实现原则。

spring boot 启动时候会加载所有jar中的 resource/META-INF/spring.factories 对其完成自动装载。

```
# org.jerfan.sky.config.RoseAutoConfiguration 是 示例中的全路径，注意更改
org.springframework.boot.autoconfigure.EnableAutoConfiguration=org.jerfan.sky.config.RoseAutoConfiguration
```

6 如有有maven私服的甩到maven私服上，没有的 本地 install 也可以。到此，完成了期望的spring-boot-starter-rose 自动配置。

7 使用 spring-boot-starter-rose ，创建一个spring boot 工程 ，依赖  spring-boot-starter-rose 

application.yml 中配置 , 可以配置哪些属性，RoseProperties 对其进行了规定。

```
application.yml 中配置
# 使用specialRose ,name 一定是special ，在getSpecialRose地方制定，约定优于配置的落地
rose:
  name: special
  version: 1.0.0
  nickName: useSpecialRose
  size: 100
  
```

完成上面配置之后 ，便可以正常使用了，

```
public Class Test{
    @Resource
    private Rose rose;
    
    public void test(){
    rose.execute();
    }

}
```

