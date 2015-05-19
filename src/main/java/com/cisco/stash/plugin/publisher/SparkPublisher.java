package com.cisco.stash.plugin.publisher;

import com.cisco.dft.cd.spark.intg.pojo.Actor;
import com.cisco.dft.cd.spark.intg.pojo.Message;
import com.cisco.dft.cd.spark.intg.service.impl.SparkIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Sagar on 19/05/15.
 */
public class SparkPublisher implements IPublisher {

    //Spark actor details:
    public static final String USERNAME = "platform-stash";
    public static final String PASSWORD = "W8.5)M1)/17=y6cTirbVL)oVc|0jF$M0";
    public static final String STASH_ACTOR_ID = "5a86a12b-c580-4ada-b4c0-9d0dfe2d72cd";

    private static final Logger log = LoggerFactory.getLogger(SparkPublisher.class);

    public static SparkIntegrationService sparkIntegrationService = new SparkIntegrationService();

    public void publish(String roomId, String notification){
        Actor actor = new Actor();
        actor.setUsername(USERNAME);
        actor.setPassword(PASSWORD);
        actor.setUid(STASH_ACTOR_ID);

        Message message = new Message();
        message.setMessage(notification);
        message.setRoomId(roomId);
        message.setActor(actor);
        sparkIntegrationService.publishMessage(message);
    }

    public static void invite(String roomId, String bearerToken){
        if(roomId.isEmpty() || bearerToken.isEmpty()){
            log.error("roomId and/or bearerToken not found.");
        }
        sparkIntegrationService.inviteParticipants(roomId, STASH_ACTOR_ID, bearerToken);
    }
}
