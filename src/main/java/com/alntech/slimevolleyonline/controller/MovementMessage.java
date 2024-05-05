package com.alntech.slimevolleyonline.controller;

public class MovementMessage {
    String type;
    int x;
    int y;
    int vX;
    int vY;
    public void setType(String type) {
        this.type = type;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setvX(int vX) {
        this.vX = vX;
    }

    public void setvY(int vY) {
        this.vY = vY;
    }

    public String getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getvX() {
        return vX;
    }

    public int getvY() {
        return vY;
    }


    public MovementMessage() {
        super();
    }

    public MovementMessage(String type, int x, int y, int vX, int vY) {
        super();
        this.type = type;
        this.x = x;
        this.y = y;
        this.vX = vX;
        this.vY = vY;
    }
}
