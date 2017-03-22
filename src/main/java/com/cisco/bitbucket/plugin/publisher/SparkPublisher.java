package com.cisco.bitbucket.plugin.publisher;

import com.cisco.bitbucket.plugin.pojo.SparkNotification;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Sagar on 19/05/15.
 */
public class SparkPublisher implements IPublisher {

    private static final Logger log = LoggerFactory.getLogger(SparkPublisher.class);

    @Override
    public void publish(String spaceId, Map<String, String> notificationMap) {

        ObjectMapper mapper = new ObjectMapper();
        SparkNotification sparkNotification = new SparkNotification(spaceId, notificationMap);
        try {
            String sparkNotificationJson = mapper.writeValueAsString(sparkNotification);
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, sparkNotificationJson);
            Request request = new Request.Builder()
                    .url("https://dftapi.cisco.com/code/cda/notification/v1/publish")
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .addHeader("cache-control", "no-cache")
                    .build();
            client.newCall(request).execute();
        } catch (IOException e) {
            log.error("Failed to push notification to spark", e);
        }
    }
}
