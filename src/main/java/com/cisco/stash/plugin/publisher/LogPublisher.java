package com.cisco.stash.plugin.publisher;

import com.cisco.stash.plugin.publisher.IPublisher;

/**
 * Created by Sagar on 19/05/15.
 */
public class LogPublisher implements IPublisher {
    @Override
    public void publish(String destination, String message) {
        System.out.println(destination);
        System.out.println(message);
    }
}
