
public record Config(int evalTimeoutSeconds) {
    public static Config load() {
        return new Config(
                Integer.parseInt(System.getenv("evalTimeoutSeconds"))
        );
    }
}
