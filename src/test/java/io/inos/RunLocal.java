package io.inos;

import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import io.muserver.murp.Murp;
import io.muserver.murp.ReverseProxyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

public class RunLocal {
    private static final Logger log = LoggerFactory.getLogger(RunLocal.class);

    private static CommandRunner runner;

    public static void startNpmDevServer() {
        runner = new AsyncCommandRunner();
        runner.runCommandAsync("cd ./web && npm run dev", res -> {
            if (res.exitCode() == 0) {
                log.info("startNpmDevServer :: Dev server started successfully {}", res);
            } else {
                log.error("startNpmDevServer :: Something went wrong {}", res);
            }
        }).exceptionally(e -> {
            log.error("startNpmDevServer :: Exception occurred", e);
            return null;
        });
    }

    public static void main(String[] args) {
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        startNpmDevServer();
        MuServer server = MuServerBuilder.httpServer()
                .withHttpPort(5454)
                .addHandler(
                        ReverseProxyBuilder.reverseProxy()
                                .withUriMapper(request -> {
                                    String pathAndQuery = Murp.pathAndQuery(request.uri());
                                    log.debug("pathAndQuery => {}", pathAndQuery);
                                    return URI.create("http://localhost:5173").resolve(pathAndQuery);
                                })
                                .sendLegacyForwardedHeaders(true)
                                .proxyHostHeader(true)
                                .withTotalTimeout(Duration.ofMinutes(1).toMillis())
                                .addProxyCompleteListener((clientRequest, clientResponse, targetUri, durationMillis) ->
                                        log.debug("Proxied {} to {} and returned {} in {} ms", clientRequest, targetUri, clientResponse.status(), durationMillis))
                                .withHttpClient(
                                        ReverseProxyBuilder.createHttpClientBuilder(true)
                                                .connectTimeout(Duration.ofMinutes(1))
                                                .version(HttpClient.Version.HTTP_1_1)
                                                .followRedirects(HttpClient.Redirect.NORMAL)
                                                .build()
                                ).build()
                )
                .addResponseCompleteListener(listener -> log.info("url => {}, response code => {}, duration => {}", listener.request().relativePath(), listener.response().status(), listener.duration()))
                .addShutdownHook(true)
                .start();

        log.info("reverse proxy server started at {}", server.uri());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            runner.stopGracefully();
            server.stop();
        }, "shutdown-hook-thread"));
    }
}
