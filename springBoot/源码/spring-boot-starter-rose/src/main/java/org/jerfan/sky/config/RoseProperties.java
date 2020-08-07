package org.jerfan.sky.config;


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



    public RoseProperties(){}

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    @Override
    public String toString() {
        return "RoseProperties{" +
                "name='" + name + '\'' +
                ", nickName='" + nickName + '\'' +
                ", version='" + version + '\'' +
                ", size=" + size +
                '}';
    }
}
