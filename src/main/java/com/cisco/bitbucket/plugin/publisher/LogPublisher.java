package com.cisco.bitbucket.plugin.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Sagar on 19/05/15.
 */
public class LogPublisher implements IPublisher {

    private static final Logger log = LoggerFactory.getLogger(SparkPublisher.class);

    public void publish(String destination, String message) {
        System.out.println(destination);
        System.out.println(message);
    }

    @Override
    public void publish(String destination, Map<String, String> notificationMessage) {
        log.info("Destination: {}", destination);
        log.info("Notification: \n");
        for (Map.Entry<String, String> entry : notificationMessage.entrySet()) {
            log.info("key: {}", entry.getKey());
            log.info("value: {}", entry.getValue());
        }
    }
}
