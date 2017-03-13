package com.cisco.bitbucket.plugin;

import com.cisco.bitbucket.plugin.publisher.IPublisher;
import com.cisco.bitbucket.plugin.publisher.SparkPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Sagar on 12/05/15.
 */
public class Notifier {

    //key for this repository hook (groupId.artifactId:hookKey)
    public static final String REPO_HOOK_KEY = "com.cisco.bitbucket.plugin.spark-push-notify:spark-notify-hook";

    //field keys from the soy template
    public static final String SPACE_ID = "spaceId";

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    public void pushNotification(String spaceId, Map<String, String> notificationMap){

        IPublisher sparkPublisher = new SparkPublisher();
        sparkPublisher.publish(spaceId, notificationMap);
    }
}
