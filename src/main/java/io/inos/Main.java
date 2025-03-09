package io.inos;

import io.muserver.ContextHandlerBuilder;
import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import io.muserver.handlers.ResourceHandlerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        MuServer server = MuServerBuilder.httpServer()
                .addHandler(ContextHandlerBuilder.context("ui")
                        .addHandler(ResourceHandlerBuilder.classpathHandler("/static"))
                        .build())
                .addResponseCompleteListener(listener -> log.info("url => {}, relative path => {}, status code => {}, duration => {}",
                        listener.request().uri(), listener.request().relativePath(), listener.response().status(), listener.duration()))
                .start();

        log.info("server started at {}", server.uri());
        log.info("UI is running on {}/ui path", server.uri());
        log.info("css file is at {}/static/assets/index-D8b4DHJx.css", server.uri());
    }
}
