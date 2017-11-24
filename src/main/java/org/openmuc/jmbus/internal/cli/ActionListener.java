package org.openmuc.jmbus.internal.cli;

public interface ActionListener {

    public void actionCalled(String actionKey) throws ActionException;

    public void quit();

}
