package com.cisco.stash.plugin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.cisco.dft.cd.spark.intg.service.pojo.HttpResponseEntity;
//import com.cisco.dft.cd.spark.intg.service.pojo.Message;
//import com.cisco.dft.cd.spark.intg.service.service.SparkIntegrationService;
//import com.cisco.dft.cd.spark.intg.service.util.Constants.PlatformType;

/**
 * Created by Sagar on 12/05/15.
 */
public class Notifier {

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    public void publishNotification(StringBuilder notification){
//        Message message = new Message();
//        message.setMessage(notification.toString());
//        message.setPlatformType(PlatformType.STASH);
//        message.setRoomId("c60f38e0-ef86-11e4-9df9-270f179cbc2c");
//        SparkIntegrationService.publishMessage(message);
//        HttpResponseEntity response = SparkIntegrationService.publishMessage(message);
//        System.out.println("response: " + response.getStatusCode());
        System.out.println(notification);
    }
}
