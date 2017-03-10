package com.cisco.bitbucket.plugin.publisher;

/**
 * Created by Sagar on 19/05/15.
 */
public interface IPublisher {

    void publish(String destination, String message);
}
