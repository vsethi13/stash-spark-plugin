package com.cisco.stash.plugin;

import com.cisco.stash.plugin.publisher.IPublisher;
import com.cisco.stash.plugin.publisher.SparkPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Sagar on 12/05/15.
 */
public class Notifier {

    //key for this repository hook (groupId.artifactId:hookKey)
    public static final String REPO_HOOK_KEY = "com.cisco.stash.plugin.spark-push-notify:spark-notify-hook";

    //field keys from the soy template
    public static final String ROOM_ID = "roomId";
    public static final String BEARER_TOKEN = "bearerToken";

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    public void pushNotification(String roomId, StringBuilder notification){

        IPublisher sparkPublisher = new SparkPublisher();
        sparkPublisher.publish(roomId, notification.toString());
    }
}
