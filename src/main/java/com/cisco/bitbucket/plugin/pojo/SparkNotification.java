package com.cisco.bitbucket.plugin.pojo;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sagar on 22/03/17.
 */
public class SparkNotification {

    private boolean autoBold;
    private Map<String, String> destination;
    private String sender;
    private Map<String, String> data;

    public SparkNotification() {

    }

    public SparkNotification(String spaceId, Map<String, String> data) {
        this.destination = new HashMap<>(2);
        this.destination.put("type", "SPARK");
        this.destination.put("roomId", spaceId);
        this.autoBold = true;
        this.sender = "code-bitbucket";
        this.data = data;
    }

    public boolean isAutoBold() {
        return autoBold;
    }

    public void setAutoBold(boolean autoBold) {
        this.autoBold = autoBold;
    }

    public Map<String, String> getDestination() {
        return destination;
    }

    public void setDestination(Map<String, String> destination) {
        this.destination = destination;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
