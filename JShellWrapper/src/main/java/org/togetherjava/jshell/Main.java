package org.togetherjava.jshell;

import org.togetherjava.jshell.wrapper.Config;
import org.togetherjava.jshell.wrapper.JShellWrapper;

public class Main {
    public static void main(String[] args) {
        JShellWrapper wrapper = new JShellWrapper();
        wrapper.run(Config.load(), System.in, System.out);
    }
}
