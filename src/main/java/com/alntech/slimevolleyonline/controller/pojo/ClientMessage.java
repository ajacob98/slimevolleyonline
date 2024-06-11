package com.alntech.slimevolleyonline.controller.pojo;

import com.fasterxml.jackson.databind.JsonNode;

public class ClientMessage {
    public String type;
    public JsonNode data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public ClientMessage() {
        super();
    }

}
