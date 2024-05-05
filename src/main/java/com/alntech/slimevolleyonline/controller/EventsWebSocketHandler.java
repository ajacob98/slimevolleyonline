package com.alntech.slimevolleyonline.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventsWebSocketHandler implements WebSocketHandler {

    Map<String, ConnectionMapping> roomMap = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Map<String, List<String>> parameters = new QueryStringDecoder(session.getHandshakeInfo().getUri()).parameters();


        String type = parameters.get("type").get(0);
        String roomId = parameters.get("room").get(0);
        System.out.println("Q\n" + parameters.get("type").get(0) + " " + parameters.get("room").get(0));

        if (type.equals("host")) {
            System.out.println("Host!!!!!!!!!!!!!!!");
            ConnectionMapping connectionMapping = new ConnectionMapping();
            connectionMapping.hostSession = session;

            roomMap.put(roomId, connectionMapping);

            return Flux.never().then();

        } else {
            System.out.println("Join!!!!!!!!!!!!!!!");

            ConnectionMapping connectionMapping = roomMap.get(roomId);
            connectionMapping.joinSession=session;
            WebSocketSession hostSession = connectionMapping.hostSession;

            session.send(hostSession.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .map((jsonString) -> {
                                try {
                                    System.out.println("json String\n"+jsonString);
                                    return objectMapper.readValue(jsonString, MovementMessage.class);
                                } catch (JsonProcessingException e) {
                                    System.out.println("session E1");
                                    System.out.println(e);
                                    throw new RuntimeException(e);
                                }
                            })
                            .map(movementMessage -> {
                                try {
                                    return objectMapper.writeValueAsString(movementMessage);
                                } catch (JsonProcessingException e) {
                                    System.out.println("session E2");
                                    System.out.println(e);
                                    throw new RuntimeException(e);
                                }
                            }).map(session::textMessage))
                    .subscribe();


            hostSession.send(session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(val->{
                                System.out.println("received from joinee "+val);
                            })
                            .map((jsonString) -> {
                                try {
                                    return objectMapper.readValue(jsonString, MovementMessage.class);
                                } catch (JsonProcessingException e) {
                                    System.out.println("E1");
                                    System.out.println(e);
                                    throw new RuntimeException(e);
                                }
                            })
                            .map(movementMessage -> {
                                try {
                                    return objectMapper.writeValueAsString(movementMessage);
                                } catch (JsonProcessingException e) {
                                    System.out.println("E2");
                                    System.out.println(e);
                                    throw new RuntimeException(e);
                                }
                            })
                            .doOnNext(val->{
                                System.out.println("sending to host "+val);
                            })
                            .map(hostSession::textMessage))
                    .subscribe();

            return Flux.never().then();
        }
    }
}