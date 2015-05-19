package com.cisco.stash.plugin.util;

import com.cisco.dft.cd.spark.intg.pojo.Actor;
import com.cisco.dft.cd.spark.intg.pojo.Message;
import com.cisco.dft.cd.spark.intg.service.impl.SparkIntegrationService;
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

    //Spark actor details:
    public static final String USERNAME = "platform-stash";
    public static final String PASSWORD = "W8.5)M1)/17=y6cTirbVL)oVc|0jF$M0";
    public static final String ACTOR_ID = "5a86a12b-c580-4ada-b4c0-9d0dfe2d72cd";

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    public void publishNotification(String roomId, StringBuilder notification){

        Actor actor = new Actor();
        actor.setUsername(USERNAME);
        actor.setPassword(PASSWORD);
        actor.setUid(ACTOR_ID);

        Message message = new Message();
        message.setMessage(notification.toString());
        message.setRoomId(roomId);
        message.setActor(actor);
        new SparkIntegrationService().publishMessage(message);

//        System.out.println(roomId);
//        System.out.println(notification);
    }
}
