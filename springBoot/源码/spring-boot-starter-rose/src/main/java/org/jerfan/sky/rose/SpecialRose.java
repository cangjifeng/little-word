package org.jerfan.sky.rose;

public class SpecialRose extends AbstractRose implements Rose {


    public SpecialRose(){
        this.name="special";
    }

    public SpecialRose(String name,long size){
        this.name=name;
        this.size=size;
    }

    @Override
    public void execute() {
        System.out.println("this is special rose , "+info());
    }
}
