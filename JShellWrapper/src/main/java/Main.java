public class Main {
    public static void main(String[] args) {
        JShellWrapper wrapper = new JShellWrapper();
        wrapper.run(Config.load(), System.in, System.out);
    }
}
