package it.com.cisco.bitbucket.plugin.hook.publisher;

import com.cisco.bitbucket.plugin.publisher.SparkPublisher;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by Sagar on 17/04/17.
 */
public class SparkPublisherTest {

    private SparkPublisher sparkPublisher;

    @Before
    public void setUp() {
        sparkPublisher = new SparkPublisher();
    }

    @Test
    public void publishTest() {
        String spaceId = "1520b570-f9a6-11e4-9a90-4949eae98d53";
        Map<String, String> notificationMap = new HashMap<>();
        String testNotification = "On branch [test1](http://localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/browse?at=refs/heads/test1)\n<br>" +
                "- Merge pull request #1 in PROJECT_1/rep_1 from test1 to master\n\n\\#USsomething\n\n\\* commit '28d4956f492384fe9693e270099291cd5702b102':\n  \\#US2346 adding abc2\n  \\# US123 adding new file ([27cb2110d97](http://localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/commits/27cb2110d97f85fb2a3c4e972dd0412a5ed94d4d))\n<br>" +
                "- adding xyz ([5681dae3940](http://localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/commits/5681dae39402a77307d66c4d5f650e4d52c0d823))\n<br>" +
                "- adding xyz2 ([f6c36ba12d1](http://localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/commits/f6c36ba12d1dd502336072786438017a64acebe7))\n<br>" +
                "- testing xyz ([f386af55706](http://localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/commits/f386af55706aecd65e258bb7dbc9864172142330))\n<br>" +
                "- adding new ([0b63d32e200](http://localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/commits/0b63d32e200abd0543e49888cfab3efbd75dce8c))";
        notificationMap.put("Details", testNotification);
        assertEquals(200, sparkPublisher.publish(spaceId, notificationMap));
    }
}
