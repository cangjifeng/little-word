package org.jerfan.sky.rose;

public abstract class AbstractRose implements Rose{

    protected String name;

    protected long size;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void execute();


    protected String info() {
        return "AbstractRose{" +
                "name='" + name + '\'' +
                ", size=" + size +
                '}';
    }
}
