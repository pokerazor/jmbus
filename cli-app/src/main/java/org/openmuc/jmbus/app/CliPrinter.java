package org.openmuc.jmbus.app;

import static javax.xml.bind.DatatypeConverter.printHexBinary;

import org.openmuc.jmbus.internal.cli.FlagCliParameter;

public class CliPrinter {

    private final String usage;
    private final FlagCliParameter printVerboseMsg;

    public CliPrinter(String usage, FlagCliParameter verbose) {
        this.usage = usage;
        this.printVerboseMsg = verbose;
    }

    public void printError(String errMsg, boolean printUsage) {
        System.err.println("Error: " + errMsg + '\n');
        if (printUsage) {
            System.err.flush();

            System.out.println(usage);
        }
        System.exit(1);
    }

    public void printlnDebug(Object... msg) {
        if (!printVerboseMsg.isSelected()) {
            return;
        }

        println(msg);
    }

    private void println(Object[] msg) {
        String string = msgToString(msg);
        System.out.println(string);
    }

    private String msgToString(Object[] msg) {
        StringBuilder sb = new StringBuilder();
        for (Object message : msg) {
            if (message instanceof byte[]) {
                sb.append(printHexBinary((byte[]) message));
            }
            else {
                sb.append(message);
            }
        }
        return sb.toString();
    }

    public void printInfo(Object... msg) {
        print(msg);
    }

    private void print(Object[] msg) {
        String string = msgToString(msg);
        System.out.print(string);
    }

    public void printlnInfo(Object... msg) {
        println(msg);
    }
}
