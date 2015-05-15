package com.cisco.stash.plugin.event;

import com.atlassian.event.api.EventListener;
import com.atlassian.stash.event.pull.*;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.pull.PullRequestAction;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.atlassian.stash.setting.Settings;

/**
 * Created by Sagar on 13/05/15.
 */
public class PullRequestEventListener {

    private final RepositoryService repositoryService;
    private final PullRequestService pullRequestService;
    private final RepositoryHookService repositoryHookService;

    public PullRequestEventListener(RepositoryService repositoryService, PullRequestService pullRequestService, RepositoryHookService repositoryHookService){
        this.repositoryService=repositoryService;
        this.pullRequestService = pullRequestService;
        this.repositoryHookService = repositoryHookService;
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
        Repository repository = event.getPullRequest().getToRef().getRepository();
        Settings settings = repositoryHookService.getSettings(repository, "com.cisco.stash.plugin.spark-push-notify:spark-notify-hook");
        if(settings == null)
            return;
        else {
            System.out.println("Room name: " + settings.getString("roomName"));
            if(event.getAction() == PullRequestAction.OPENED)
                PrOpenedEvent(event);
            else if(event.getAction() == PullRequestAction.APPROVED)
                PrApprovedEvent(event);
            else if(event.getAction() == PullRequestAction.DECLINED)
                PrDeclinedEvent(event);
            else if(event.getAction() == PullRequestAction.REOPENED){

            }
        }
    }

    private void PrOpenedEvent(PullRequestEvent event) {
        System.out.println("Pull Request " + "#" + event.getPullRequest().getId() + " (" + event.getPullRequest().getTitle() + ") " + "opened");
        System.out.println(event.getUser().getDisplayName());
    }

    private void PrApprovedEvent(PullRequestEvent event) {
        System.out.println("Pull Request " + "#" + event.getPullRequest().getId() + " (" + event.getPullRequest().getTitle() + ") " + "approved");
        System.out.println(event.getUser().getDisplayName());
    }

    private void PrDeclinedEvent(PullRequestEvent event) {
        System.out.println("Pull Request " + "#" + event.getPullRequest().getId() + " (" + event.getPullRequest().getTitle() + ") " + "declined");
        System.out.println(event.getUser().getDisplayName());
    }

    private void PrReOpenedEvent(PullRequestEvent event) {
        System.out.println("Pull Request " + "#" + event.getPullRequest().getId() + " (" + event.getPullRequest().getTitle() + ") " + "reopened");
        System.out.println(event.getUser().getDisplayName());
    }
}
