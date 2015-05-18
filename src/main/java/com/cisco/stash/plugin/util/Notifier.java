package com.cisco.stash.plugin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.dft.cd.spark.intg.service.pojo.HttpResponseEntity;
import com.cisco.dft.cd.spark.intg.service.pojo.Message;
import com.cisco.dft.cd.spark.intg.service.service.SparkIntegrationService;
import com.cisco.dft.cd.spark.intg.service.util.Constants.PlatformType;

/**
 * Created by Sagar on 12/05/15.
 */
public class Notifier {

    //key for this repository hook
    public static final String REPO_HOOK_KEY = "com.cisco.stash.plugin.spark-push-notify:spark-notify-hook";

    //field keys from the soy template
    public static final String ROOM_ID = "roomId";
    public static final String BEARER_TOKEN = "bearerToken";

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    public void publishNotification(String roomId, StringBuilder notification){
        Message message = new Message();
        message.setMessage(notification.toString());
        message.setPlatformType(PlatformType.STASH);
        message.setRoomId(roomId);
        SparkIntegrationService.publishMessage(message);
//        HttpResponseEntity response = SparkIntegrationService.publishMessage(message);
//        System.out.println("response: " + response.getStatusCode());
//        System.out.println(roomId);
//        System.out.println(notification);
    }
}
