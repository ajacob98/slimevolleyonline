package com.alntech.slimevolleyonline.controller;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class EventsWebSocketHandler implements WebSocketHandler {

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        Flux<WebSocketMessage> output = session.receive()
                .doOnNext(message -> {
                    System.out.println("MSTART\n"+message+"\nMEND");
                })
//                .concatMap(message -> {
//                    // ...
//                })
                .map(value -> session.textMessage("Echo " + value));

        return session.send(output);
    }
}