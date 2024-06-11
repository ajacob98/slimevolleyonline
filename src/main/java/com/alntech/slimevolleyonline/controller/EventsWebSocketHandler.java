package com.alntech.slimevolleyonline.controller;

import com.alntech.slimevolleyonline.controller.pojo.BallData;
import com.alntech.slimevolleyonline.controller.pojo.ClientMessage;
import com.alntech.slimevolleyonline.controller.pojo.InitialData;
import com.alntech.slimevolleyonline.controller.pojo.SlimeData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventsWebSocketHandler implements WebSocketHandler {

    Map<String, GameData> roomMap = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Map<String, List<String>> parameters = new QueryStringDecoder(session.getHandshakeInfo().getUri()).parameters();


        String type = parameters.get("type").get(0);
        String roomId = parameters.get("room").get(0);
        System.out.println("Q\n" + parameters.get("type").get(0) + " " + parameters.get("room").get(0));

        if (type.equals("host")) {
            System.out.println("Host!!!!!!!!!!!!!!!");
            GameData gameData;
            if (!roomMap.containsKey(roomId)) {
                gameData = new GameData();
                roomMap.put(roomId, gameData);
            } else {
                gameData = roomMap.get(roomId);
            }

            gameData.hostSession = session;

            //Flux to send score every 2 seconds
            Flux.interval(Duration.ofSeconds(2)).subscribe(new Subscriber<Long>() {
                Subscription s;
                @Override
                public void onSubscribe(Subscription s) {
                    this.s = s;
                    s.request(1);
                }
                @Override
                public void onNext(Long aLong) {
                    s.request(1);
                    if (gameData.hostSession != null)
                        if (gameData.hostSession.isOpen()) {
                            System.out.println("SENDING SCORE");
                            gameData.hostSession.send(Mono.just(gameData.hostSession.textMessage("{\"type\": \"score\", \"score1\":" + gameData.hostScore + ", \"score2\": " + gameData.joinScore + "}"))).subscribe();
                        } else
                            s.cancel();
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }
            });

            //process messages received from host slime
            return session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .map((jsonString) -> {
                        try {
                            return objectMapper.readValue(jsonString, ClientMessage.class);
                        } catch (JsonProcessingException e) {
                            System.out.println(e);
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(clientMessage -> {
                        if (clientMessage.type.equals("initialize")) {
                            System.out.println("initialize");

                            InitialData initialData = null;
                            try {
                                initialData = objectMapper.treeToValue(clientMessage.getData(), InitialData.class);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                            gameData.ballRadius = initialData.getBallRadius();
                            gameData.slimeRadius = initialData.getSlimeRadius();
                            gameData.floorWidth = initialData.getFloorWidth();
                            System.out.println(initialData.getBallRadius());
                            return false;
                        }

                        return true;
                    })
                    .filter((clientMessage) -> gameData.joinSession != null && gameData.joinSession.isOpen())
                    .filter((clientMessage) -> {
                        if (clientMessage.type.equals(("lostPoint"))) {
                            System.out.println("LOST POINT/n/n/n/n/n/n/n/n");
                            gameData.joinScore++;
                            Mono.delay(Duration.ofSeconds(3)).doOnNext((val) -> {
                                System.out.println("sending reset host");
                                gameData.hostSession.send(Flux.just(gameData.hostSession.textMessage("{\"type\": \"resetTo2\"}"))).subscribe();
                                gameData.joinSession.send(Flux.just(gameData.joinSession.textMessage("{\"type\": \"resetTo2\"}"))).subscribe();
                            }).subscribe();
                            return false;
                        }
                        return true;
                    })
                    .map(clientMessage -> {
                        try {
                            if (clientMessage.type.equals("slime")) {
                                SlimeData slimeData = objectMapper.treeToValue(clientMessage.getData(), SlimeData.class);
                                return objectMapper.writeValueAsString(clientMessage);
                            } else { //if(clientMessage.type.equals("ball"))
                                BallData ballData = objectMapper.treeToValue(clientMessage.getData(), BallData.class);
                                return objectMapper.writeValueAsString(clientMessage);
                            }
                        } catch (JsonProcessingException e) {
                            System.out.println("session E2");
                            System.out.println(e);
                            throw new RuntimeException(e);
                        }
                    })
                    .map((val) -> gameData.joinSession.textMessage(val))
                    .doOnNext((val) -> {
                        gameData.joinSession.send(Flux.just(val)).subscribe();
                    })
                    .onErrorContinue((error, value) -> {
                        System.out.println("HOST Error occurred: " + error.getMessage() + ", Skipping value: " + value);
                    })
                    .then();
//            return Flux.never().then();

        }
        else {
            System.out.println("Join!!!!!!!!!!!!!!!");
            GameData gameData = roomMap.get(roomId);

            gameData.joinSession = session;


            //Send score every two seconds to other slime
            Flux.interval(Duration.ofSeconds(2)).subscribe(new Subscriber<Long>() {
                Subscription s;

                @Override
                public void onSubscribe(Subscription s) {
                    this.s = s;
                    s.request(1);
                }
                @Override
                public void onNext(Long aLong) {
                    s.request(1);
                    if (gameData.joinSession != null)
                        if (gameData.joinSession.isOpen())
                            gameData.joinSession.send(Mono.just(gameData.joinSession.textMessage("{\"type\": \"score\", \"score1\":" + gameData.hostScore + ", \"score2\": " + gameData.joinScore + "}"))).subscribe();
                        else
                            s.cancel();
                }
                @Override
                public void onError(Throwable throwable) {

                }
                @Override
                public void onComplete() {

                }
            });

            //process messages received from other slime
            return session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .filter((jsonString) -> gameData.hostSession != null && gameData.hostSession.isOpen())
                    .map((jsonString) -> {
                        try {
                            return objectMapper.readValue(jsonString, ClientMessage.class);
                        } catch (JsonProcessingException e) {
                            System.out.println(e);
                            throw new RuntimeException(e);
                        }
                    })
                    .filter((clientMessage) -> {
                        if (clientMessage.type.equals(("lostPoint"))) {
                            System.out.println("LOST POINT/n/n/n/n/n/n/n/n");
                            gameData.hostScore++;
                            Mono.delay(Duration.ofSeconds(3)).doOnNext((val) -> {
                                System.out.println("sending reset join");
                                gameData.hostSession.send(Flux.just(gameData.hostSession.textMessage("{\"type\": \"resetTo1\"}"))).subscribe();
                                gameData.joinSession.send(Flux.just(gameData.joinSession.textMessage("{\"type\": \"resetTo1\"}"))).subscribe();
                            }).subscribe();
                            return false;
                        }
                        return true;
                    })
                    .map(clientMessage -> {
                        try {
                            if (clientMessage.type.equals("slime")) {
                                SlimeData slimeData = objectMapper.treeToValue(clientMessage.getData(), SlimeData.class);
                                return objectMapper.writeValueAsString(clientMessage);
                            } else { //if(clientMessage.type.equals("ball"))
                                BallData ballData = objectMapper.treeToValue(clientMessage.getData(), BallData.class);
                                return objectMapper.writeValueAsString(clientMessage);
                            }
                        } catch (JsonProcessingException e) {
                            System.out.println("session E2");
                            System.out.println(e);
                            throw new RuntimeException(e);
                        }
                    })
                    .map((val) -> gameData.hostSession.textMessage(val))
                    .doOnNext((val) -> {
                        gameData.hostSession.send(Flux.just(val)).subscribe();
                    })
                    .onErrorContinue((error, value) -> {
                        System.out.println("JOIN Error occurred: " + error.getMessage() + ", Skipping value: " + value);
                    })
                    .then();

//            return Flux.never().then();
        }
    }
}