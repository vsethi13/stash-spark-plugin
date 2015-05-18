package com.cisco.stash.plugin.event;

import com.atlassian.event.api.EventListener;
import com.atlassian.stash.event.pull.*;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.pull.PullRequestAction;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.SecurityService;
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
    public void onPullRequestDeclined(PullRequestDeclinedEvent event) {
        handlePullRequestEvent(event);
    }

    @EventListener
    public void onPullRequestReopened(PullRequestReopenedEvent event) {
        handlePullRequestEvent(event);
    }

    private void handlePullRequestEvent(PullRequestEvent event){

        final Repository repository = event.getPullRequest().getToRef().getRepository();
        Settings settings = null;
        try {
            settings = securityService.withPermission(Permission.REPO_ADMIN, "").call(new Operation<Settings, Exception>() {
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
            System.out.println("Room name: " + settings.getString("roomName"));
            if(event.getAction() == PullRequestAction.OPENED)
                PrOpenedEvent(event);
            else if(event.getAction() == PullRequestAction.APPROVED)
                PrApprovedEvent(event);
            else if(event.getAction() == PullRequestAction.DECLINED)
                PrDeclinedEvent(event);
            else if(event.getAction() == PullRequestAction.REOPENED){
                PrReOpenedEvent(event);
            }
        }
    }

    private void PrOpenedEvent(PullRequestEvent event) {
        System.out.println("Pull Request " + "#" + event.getPullRequest().getId() + " (" + event.getPullRequest().getTitle() + ") " + event.getAction().toString().toLowerCase());
        System.out.println(event.getUser().getDisplayName());
    }

    private void PrApprovedEvent(PullRequestEvent event) {
        System.out.println("Pull Request " + "#" + event.getPullRequest().getId() + " (" + event.getPullRequest().getTitle() + ") " + event.getAction().toString().toLowerCase());
        System.out.println(event.getUser().getDisplayName());
    }

    private void PrDeclinedEvent(PullRequestEvent event) {
        System.out.println("Pull Request " + "#" + event.getPullRequest().getId() + " (" + event.getPullRequest().getTitle() + ") " + event.getAction().toString().toLowerCase());
        System.out.println(event.getUser().getDisplayName());
    }

    private void PrReOpenedEvent(PullRequestEvent event) {
        System.out.println("Pull Request " + "#" + event.getPullRequest().getId() + " (" + event.getPullRequest().getTitle() + ") " + event.getAction().toString().toLowerCase());
        System.out.println(event.getUser().getDisplayName());
    }
}
