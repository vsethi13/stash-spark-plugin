package com.cisco.stash.plugin.publisher;

/**
 * Created by Sagar on 19/05/15.
 */
public interface IPublisher {

    public void publish(String destination, String message);
}
