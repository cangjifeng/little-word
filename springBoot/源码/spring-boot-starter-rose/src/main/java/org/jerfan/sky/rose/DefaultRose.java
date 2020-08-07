package org.jerfan.sky.rose;

public class DefaultRose extends AbstractRose implements Rose {


    public DefaultRose(){
        this.name="default";
    }

    public DefaultRose(String name,long size){
        this.name=name;
        this.size=size;
    }
    @Override
    public void execute() {
        System.out.println("this is default rose ,"+info());
    }
}
