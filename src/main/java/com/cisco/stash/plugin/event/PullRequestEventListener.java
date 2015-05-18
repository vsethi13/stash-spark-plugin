package com.cisco.stash.plugin.event;

import com.atlassian.event.api.EventListener;
import com.atlassian.stash.event.pull.*;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestAction;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.user.StashUser;
import com.atlassian.stash.util.Operation;
import com.cisco.stash.plugin.hook.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Sagar on 13/05/15.
 */
public class PullRequestEventListener {

    private PullRequestService pullRequestService;
    private RepositoryHookService repositoryHookService;
    private SecurityService securityService;

    private static final Logger log = LoggerFactory.getLogger(PullRequestEventListener.class);

    public PullRequestEventListener(PullRequestService pullRequestService, RepositoryHookService repositoryHookService, SecurityService securityService){
        this.pullRequestService = pullRequestService;
        this.repositoryHookService = repositoryHookService;
        this.securityService = securityService;
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
    //post to the room only if room name exists
    private void handlePullRequestEvent(PullRequestEvent event){

        final Repository repository = event.getPullRequest().getToRef().getRepository();
        Settings settings = null;
        try {
            settings = securityService.withPermission(Permission.REPO_ADMIN, "Access required to get hook settings.").call(new Operation<Settings, Exception>() {
                @Override
                public Settings perform() throws Exception {
                    return repositoryHookService.getSettings(repository, Notifier.REPO_HOOK_KEY);
                }
            });
        } catch (Exception e) {
            log.error("Unexpected exception trying to get the hook settings");
        }

        if(settings == null)
            log.info("Settings not found.");
        else {
            System.out.println("In room: " + settings.getString("roomName"));
            createPrNotification(event);
        }
    }

    //create a notification for a PR related event
    private void createPrNotification(PullRequestEvent event){
        StringBuilder notification = new StringBuilder(128);
        PullRequest pullRequest = event.getPullRequest();
        Repository repository = pullRequest.getToRef().getRepository();
        StashUser stashUser = event.getUser();
        notification.append("Pull Request " + "#" + pullRequest.getId());
        notification.append(event.getAction().toString().toLowerCase() + " by " + stashUser.getDisplayName() + "[" + stashUser.getEmailAddress() + "] ");
        notification.append("in " + repository.getProject().getName() + "/" + repository.getName());
        notification.append("\n");
        notification.append("Title: " + pullRequest.getTitle());
        System.out.println(notification);
    }
}
