package io.inos;

import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import io.muserver.murp.Murp;
import io.muserver.murp.ReverseProxyBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class RunLocal {
    private static final Logger log = LoggerFactory.getLogger(RunLocal.class);

    private static CommandRunner runner;

    public static void startNpmDevServer() {
        runner = new AsyncCommandRunner();
        runner.runCommandAsync("cd ./web && npm run dev", res -> log.info("res => {}", res));
    }

    public static void main(String[] args) {
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        CompletableFuture.runAsync(RunLocal::startNpmDevServer);
        MuServer server = MuServerBuilder.httpServer()
                .withHttpPort(5454)
                .addHandler(
                        ReverseProxyBuilder.reverseProxy()
                                .withUriMapper(request -> {
                                    String pathAndQuery = Murp.pathAndQuery(request.uri());
                                    log.info("pathAndQuery => {}", pathAndQuery);
                                    return URI.create("http://localhost:5173").resolve(pathAndQuery);
                                })
                                .sendLegacyForwardedHeaders(true)
                                .proxyHostHeader(true)
                                .withTotalTimeout(Duration.ofMinutes(1).toMillis())
                                .addProxyCompleteListener((clientRequest, clientResponse, targetUri, durationMillis) -> {
                                    log.info("Proxied {} to {} and returned {} in {} ms", clientRequest, targetUri, clientResponse.status(), durationMillis);
                                })
                                .withHttpClient(
                                        ReverseProxyBuilder.createHttpClientBuilder(true)
                                                .connectTimeout(Duration.ofMinutes(1))
                                                .version(HttpClient.Version.HTTP_1_1)
                                                .followRedirects(HttpClient.Redirect.NORMAL)
                                                .build()
                                ).build()
                )
                .addShutdownHook(true)
                .start();

        log.info("reverse proxy server started at {}", server.uri());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            runner.stopGracefully();
            server.stop();
        }, "process-stopper-thread"));
    }

    @Path("ui")
    static class ReverseProxy {
        @GET
        public Response uiHandler() {
            return Response.temporaryRedirect(URI.create("http://localhost:5173/ui")).build();
        }
    }
}
