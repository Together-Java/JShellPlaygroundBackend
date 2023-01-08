import jdk.jshell.JShell;

import java.util.Scanner;

public class JShellWrapper {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Hello world " + scanner.nextLine());
    }
}
