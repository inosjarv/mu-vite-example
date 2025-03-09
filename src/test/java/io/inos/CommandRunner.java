package io.inos;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface CommandRunner {
    CompletableFuture<Void> runCommandAsync(String command, Consumer<CommandResultV2> listener);
    void readStream(InputStream is, Consumer<String> logConsumer);
    void stopGracefully();

    public record CommandResultV2(String command, int exitCode, List<String> output){}
}
