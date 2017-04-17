package com.cisco.bitbucket.plugin.publisher;

import java.util.Map;

/**
 * Created by Sagar on 19/05/15.
 */
public interface IPublisher {

    int publish(String destination, Map<String, String> notificationMessage);
}
