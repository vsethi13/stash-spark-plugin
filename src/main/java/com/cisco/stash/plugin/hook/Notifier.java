package com.cisco.stash.plugin.hook;

import com.atlassian.stash.commit.Commit;
import com.atlassian.stash.commit.CommitService;
import com.atlassian.stash.commit.CommitsBetweenRequest;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.atlassian.stash.util.PageRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.stash.scm.git.GitRefPattern;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sagar on 12/05/15.
 */
public class Notifier {

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);
    private final StashAuthenticationContext stashAuthenticationContext;
    private final CommitService commitService;
    private final NavBuilder navBuilder;

    public static final String REPO_HOOK_KEY = "com.cisco.stash.plugin.spark-push-notify:spark-notify-hook";

    public Notifier(StashAuthenticationContext stashAuthenticationContext, CommitService commitService, NavBuilder navBuilder){
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.commitService = commitService;
        this.navBuilder = navBuilder;
    }

    public StringBuilder createNotification(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges){
        StringBuilder notification = new StringBuilder(1000);
        Map<String, String> commitLinks = new HashMap<String, String>();
        StashUser stashUser = stashAuthenticationContext.getCurrentUser();
        Repository repository = repositoryHookContext.getRepository();
        notification.append(stashUser.getDisplayName() + "[" + stashUser.getEmailAddress() + "] ");
        notification.append("committed to " + refChanges.size() + " branch(es) ");
        notification.append("at " + repository.getProject().getName() + "/" + repository.getName());
        notification.append("\n");

        for(RefChange refChange : refChanges){
            if(refChange.getType() == RefChangeType.ADD){
                notification.append("New ref " + StringUtils.removeStart(refChange.getRefId(), GitRefPattern.HEADS.getPath()) + " has been added to the repo");
                notification.append("\n");
            }
            else if(refChange.getType() == RefChangeType.DELETE){
                notification.append("The ref " + StringUtils.removeStart(refChange.getRefId(), GitRefPattern.HEADS.getPath()) + " has been deleted from the repo");
                notification.append("\n");
            }
            else if(refChange.getType() == RefChangeType.UPDATE){
                notification.append("On ref " + StringUtils.removeStart(refChange.getRefId(), GitRefPattern.HEADS.getPath()) + ":");
                notification.append("\n");

                CommitsBetweenRequest commitsBetweenRequest;
                commitsBetweenRequest = new CommitsBetweenRequest.Builder(repository)
                        .include(refChange.getToHash())
                        .exclude(refChange.getFromHash())
                        .build();

                //TODO: do something about the hardcoded literals
                //TODO: limit amount of commit info to be displayed
                for (Commit commit : commitService.getCommitsBetween(commitsBetweenRequest, new PageRequestImpl(0, 100)).getValues()) {
                    notification.append("- " + commit.getMessage() + "(" + commit.getDisplayId() + ") ");
                    notification.append("@ " + commit.getAuthorTimestamp());
                    notification.append("\n");
                    commitLinks.put(commit.getDisplayId(), navBuilder.repo(repository).commit(commit.getId()).buildConfigured());
                }
            }
        }
        notification.append("Repo URL: " + navBuilder.repo(repository).buildConfigured());
        notification.append("\n");
        if(commitLinks.size() > 0) {
            notification.append("Commit URL(s): \n");
            for (Map.Entry<String, String> commitEntry : commitLinks.entrySet()) {
                notification.append("- " + commitEntry.getKey() + ": " + commitEntry.getValue());
                notification.append("\n");
            }
        }
        return notification;
    }

    public void publishNotification(StringBuilder notification){
        int notificationLength = notification.length();
        if(notification.charAt(notificationLength-1) == '\n')
            notification.deleteCharAt(notificationLength-1);
        System.out.println(notification);
    }
}
