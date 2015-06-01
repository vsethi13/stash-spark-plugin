package com.cisco.stash.plugin.event;

import com.atlassian.event.api.EventListener;
import com.atlassian.stash.event.pull.*;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.StashUser;
import com.cisco.stash.plugin.Notifier;
import com.cisco.stash.plugin.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Sagar on 13/05/15.
 */
public class PullRequestEventListener {

    private SettingsService settingsService;
    private NavBuilder navBuilder;

    private static final Logger log = LoggerFactory.getLogger(PullRequestEventListener.class);


    public PullRequestEventListener(SettingsService settingsService, NavBuilder navBuilder) {
        this.settingsService = settingsService;
        this.navBuilder = navBuilder;
    }

    @EventListener
    public void onPullRequestOpened(PullRequestOpenedEvent event) {
        handlePullRequestEvent(event);
    }

    @EventListener
    public void onPullRequestApproved(PullRequestApprovedEvent event) {
        handlePullRequestEvent(event);
    }

    @EventListener
    public void onPullRequestUnapproved(PullRequestUnapprovedEvent event) {
        handlePullRequestEvent(event);
    }

    @EventListener
    public void onPullRequestDeclined(PullRequestDeclinedEvent event) {
        handlePullRequestEvent(event);
    }

    @EventListener
    public void onPullRequestReopened(PullRequestReopenedEvent event) {
        handlePullRequestEvent(event);
    }

    @EventListener
    public void onPullRequestMerged(PullRequestMergedEvent event) {
        handlePullRequestEvent(event);
    }

    //get settings to check if the room name is present (check if the hook is enabled or not)
    //post to the room only if hook is enabled and room name exists
    private void handlePullRequestEvent(PullRequestEvent event){

        final Repository repository = event.getPullRequest().getToRef().getRepository();
        Settings repoSettings = null;

        repoSettings = settingsService.getSettings(repository, Notifier.REPO_HOOK_KEY);

        if(repoSettings == null)
            log.info("Settings not found.");
        else {
            new Notifier().pushNotification(repoSettings.getString(Notifier.ROOM_ID, ""), createPrNotification(event));
        }
    }

    /**
     * create a notification for a PR related event
     * @param event
     * @return
     */
    private StringBuilder createPrNotification(PullRequestEvent event){
        StringBuilder notification = new StringBuilder(128);
        PullRequest pullRequest = event.getPullRequest();
        Repository repository = pullRequest.getToRef().getRepository();
        StashUser stashUser = event.getUser();
        notification.append("Pull Request " + "#" + pullRequest.getId());
        notification.append(" " + event.getAction().toString().toLowerCase() + " by " + stashUser.getDisplayName() + " ");
        notification.append("in " + repository.getProject().getName() + "/" + repository.getName());
        notification.append("\n");
        notification.append("Title: " + pullRequest.getTitle());
        notification.append("\n");
        notification.append("URL: " + navBuilder.repo(repository).pullRequest(pullRequest.getId()).buildAbsolute());
        return notification;
    }
}
