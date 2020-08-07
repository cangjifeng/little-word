

spring boot 自动配置

spring boot 出现的背景 ： 

spring 的一个重要思想 约定优于配置 ，以及spring 4.x 提供的按条件配置bean的能力，促成了spring boot的出现。

spring有一个全局的配置文件 application.properties 或者 application.yml 。

spring的自动配置封装在 org.springframework.boot.spring-boot-autoconfigure.jar 中。

spring启动类上有@SpringBootApplication注解，它是一个复合注解，其中有 @EnableAutoConfiguration 注解，@EnableAutoConfiguration 的作用是开启自动配置，@EnableAutoConfiguration 也是一个复合注解，关键功能是 @Import 注解提供的，其导入的AutoConfigurationImportSelector的selectImports()方法通过SpringFactoriesLoader.loadFactoryNames()扫描所有具有META-INF/spring.factories的jar包。spring-boot-autoconfigure-x.x.x.x.jar里就有一个这样的spring.factories文件。spring.factories 的存储格式是key-value

```
spring boot 自动启动的简单回答：
Spring Boot启动的时候会通过@EnableAutoConfiguration注解找到META-INF/spring.factories配置文件中的所有自动配置类，并对其进行加载，而这些自动配置类都是以AutoConfiguration结尾来命名的，它实际上就是一个JavaConfig形式的Spring容器配置类，它能通过以Properties结尾命名的类中取得在全局配置文件中配置的属性如：server.port，而XxxxProperties类是通过@ConfigurationProperties注解与全局配置文件中对应的属性进行绑定的。
XxxxProperties类的含义是：封装配置文件中相关属性；XxxxAutoConfiguration类的含义是：自动配置类，目的是给容器中添加组件。
```

