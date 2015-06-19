package com.cisco.stash.plugin.publisher;

import com.cisco.dft.cd.spark.intg.pojo.Actor;
import com.cisco.dft.cd.spark.intg.pojo.Message;
import com.cisco.dft.cd.spark.intg.pojo.OAuthCredentials;
import com.cisco.dft.cd.spark.intg.service.impl.SparkIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Sagar on 19/05/15.
 */
public class SparkPublisher implements IPublisher {

    //Spark actor details:
    public static final String USERNAME = "stash-platform";
    public static final String PASSWORD = "W8.5)M1)/17=y6cTirbVL)oVc|0jF$M0";
    public static final String STASH_ACTOR_ID = "119845d5-144a-4e16-baff-5a0c9d530957";
    public static final String CLIENT_ID = "C118c5ea0db6cb8ee42428d62b2949f84f181081313f2f9eea08eff1599c16c39";
    public static final String CLIENT_SECRET = "9931beebba9fa019130b048a508fa1222263ef2eafc87fc98bc4a09308acfc01";

    private static final Logger log = LoggerFactory.getLogger(SparkPublisher.class);

    public static SparkIntegrationService sparkIntegrationService = new SparkIntegrationService();

    public void publish(String roomId, String notification){

        OAuthCredentials creds = new OAuthCredentials();
        creds.setClientID(CLIENT_ID);
        creds.setClientSecret(CLIENT_SECRET);

        Actor actor = new Actor();
        actor.setUsername(USERNAME);
        actor.setPassword(PASSWORD);
        actor.setUid(STASH_ACTOR_ID);

        Message message = new Message();
        message.setMessage(notification);
        message.setRoomId(roomId);
        message.setActor(actor);
        message.setOauthCredentials(creds);
        sparkIntegrationService.publishMessage(message);
    }

    public static void invite(String roomId, String bearerToken){
        if(roomId.isEmpty() || bearerToken.isEmpty()){
            log.error("roomId and/or bearerToken not found.");
        }
        sparkIntegrationService.inviteParticipants(roomId, STASH_ACTOR_ID, bearerToken);
    }
}
