package com.cisco.bitbucket.plugin.publisher;

import com.cisco.bitbucket.plugin.pojo.SparkNotification;
import com.squareup.okhttp.*;
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
    public int publish(String spaceId, Map<String, String> notificationMap) {

        ObjectMapper mapper = new ObjectMapper();
        SparkNotification sparkNotification = new SparkNotification(spaceId, notificationMap);
        Response response = null;
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
            response = client.newCall(request).execute();
        } catch (IOException e) {
            log.error("Failed to push notification to spark", e);
        } finally {
            if (response != null) {
                try {
                    response.body().close();
                } catch (IOException e) {
                    log.error("Error while closing response body", e);
                }
            }
        }
        return response == null ? -1 : response.code();
    }
}
