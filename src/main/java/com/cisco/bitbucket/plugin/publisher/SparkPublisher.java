package com.cisco.bitbucket.plugin.publisher;

import com.ciscospark.Spark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

/**
 * Created by Sagar on 19/05/15.
 */
public class SparkPublisher implements IPublisher {

    private Spark spark;
    private static final String BOT_BEARER_TOKEN = "NjhkNTc3MzUtZDJjMS00MDJjLWExYjAtYzkyYjVmODQ2MjVkNjIwZDcyYjEtZjgy";
    private static final String NEWLINE = "\n<br>";

    private static final Logger log = LoggerFactory.getLogger(SparkPublisher.class);

    public SparkPublisher() {
        spark = Spark.builder()
                .baseUrl(URI.create("https://api.ciscospark.com/v1"))
                .accessToken(BOT_BEARER_TOKEN)
                .build();
    }

    @Override
    public void publish(String spaceId, Map<String, String> notificationMap) {
        com.ciscospark.Message message = new com.ciscospark.Message();
        message.setRoomId(spaceId);
        message.setMarkdown(prepareNotification(notificationMap));
        spark.messages().post(message);
    }

    private String prepareNotification(Map<String, String> notificationMap) {
        StringBuilder notification = new StringBuilder(4096);
        for (Map.Entry<String, String> entry : notificationMap.entrySet()) {
            notification.append("**").append(entry.getKey()).append(":** ");
            notification.append(entry.getValue());
            notification.append(NEWLINE);
        }
        return notification.toString();
    }
}
