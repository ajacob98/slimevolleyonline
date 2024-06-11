package com.alntech.slimevolleyonline.controller;

import org.springframework.web.reactive.socket.WebSocketSession;

public class GameData {
    public int ballRadius;
    public int slimeRadius;
    public int floorWidth;
    public WebSocketSession hostSession;
    public WebSocketSession joinSession;

    public int hostScore;
    public int joinScore;
}
