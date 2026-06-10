package com.gurjeet.pm.adapter.in.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final BoardWebSocketHandler boardWebSocketHandler;
    private final WsHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(BoardWebSocketHandler boardWebSocketHandler,
                           WsHandshakeInterceptor handshakeInterceptor) {
        this.boardWebSocketHandler = boardWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(boardWebSocketHandler, "/ws/board")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
