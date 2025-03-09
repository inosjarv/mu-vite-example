package io.inos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AsyncCommandRunner implements CommandRunner {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final AtomicReference<List<Process>> processes = new AtomicReference<>(new ArrayList<>());
    private static final Logger log = LoggerFactory.getLogger(AsyncCommandRunner.class);

    @Override
    public CompletableFuture<Void> runCommandAsync(String command, Consumer<CommandResultV2> listener) {
        return CompletableFuture.runAsync(() -> {
            log.info("runCommandAsync :: Running Command => {}", command);
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            processBuilder.redirectErrorStream(true);

            try {
                Process process = processBuilder.start();
                updateProcessList(process, true);

                List<String> output = new ArrayList<>();
                readStream(process.getInputStream(), output::add);

                int exitCode = process.waitFor();
                listener.accept(new CommandResultV2(command, exitCode, output));
            } catch (Exception e) {
                log.error("runCommandAsync :: Exception occurred", e);
                listener.accept(new CommandResultV2(command, -1, List.of("Error: " + e.getMessage())));
            }
        });
    }

    private void updateProcessList(Process process, boolean add) {
        processes.updateAndGet(list -> {
            List<Process> updated = new ArrayList<>(list);
            if (add) updated.add(process);
            return updated;
        });
    }

    @Override
    public void readStream(InputStream is, Consumer<String> logConsumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while (Objects.nonNull(line = reader.readLine())) {
                logConsumer.accept(line);
            }
        } catch (IOException e) {
            logConsumer.accept("Error reading output: " + e.getMessage());
        }
    }

    @Override
    public void stopGracefully() {
        log.info("Shutting down all processes");
        processes.getAndUpdate(list -> {
            list.forEach(process -> {
                log.info("stopGraceFully :: Stopping Process => {}", process.info().commandLine());
                process.destroy();
            });
            return list;
        });
        executor.shutdown();
        try {
            executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("stopGracefully :: Exception occurred", e);
            Thread.currentThread().interrupt();
        }
    }
}
