package org.jerfan.sky.config;

import org.jerfan.sky.rose.DefaultRose;
import org.jerfan.sky.rose.SpecialRose;
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
