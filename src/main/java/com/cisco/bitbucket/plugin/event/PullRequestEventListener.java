package com.cisco.bitbucket.plugin.event;

import com.atlassian.event.api.EventListener;
import com.atlassian.bitbucket.comment.CommentAction;
import com.atlassian.bitbucket.event.pull.*;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestAction;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.git.GitRefPattern;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.cisco.bitbucket.plugin.Notifier;
import com.cisco.bitbucket.plugin.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

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
    public void onPullRequestApproved(PullRequestParticipantApprovedEvent event) {
        handlePullRequestEvent(event);
    }

    @EventListener
    public void onPullRequestUnapproved(PullRequestParticipantUnapprovedEvent event) {
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
    public void onPullRequestUpdated(PullRequestUpdatedEvent event) {
        handlePullRequestEvent(event);
    }

//    @EventListener
//    public void onPullRequestRescoped(PullRequestRescopedEvent event) {
//        handlePullRequestEvent(event);
//    }

    @EventListener
    public void onPullRequestMerged(PullRequestMergedEvent event) {
        handlePullRequestEvent(event);
    }

    @EventListener
    public void onPullRequestCommented(PullRequestCommentEvent event) {
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
        StringBuilder notification = new StringBuilder(256);
        PullRequest pullRequest = event.getPullRequest();
        Repository repository = pullRequest.getToRef().getRepository();
        ApplicationUser stashUser = event.getUser();

        //If event is related to a PR comment
        if(event.getAction() == PullRequestAction.COMMENTED) {
            PullRequestCommentEvent commentEvent = (PullRequestCommentEvent) event;
            notification.append(stashUser.getDisplayName() + " " + commentEvent.getCommentAction().toString().toLowerCase());
            notification.append(commentEvent.getCommentAction().toString().equals(String.valueOf(CommentAction.REPLIED)) ? " with a comment" : " a comment");
            notification.append(" on pull request " + "#" + pullRequest.getId());
            notification.append(" in \"" + repository.getProject().getName() + "/" + repository.getName() + "\"");
            notification.append("\n");
            notification.append("Comment: " + commentEvent.getComment().getText());
            notification.append("\n");

        //for all other events other than "COMMENT" event
        } else {
            notification.append("Pull Request " + "#" + pullRequest.getId());
            notification.append(" " + event.getAction().toString().toLowerCase() + " by " + stashUser.getDisplayName() + " ");
            notification.append("in \"" + repository.getProject().getName() + "/" + repository.getName() + "\"");
            notification.append("\n");
            notification.append("Title: " + pullRequest.getTitle());
            notification.append("\n");
            notification.append("Scope: " + StringUtils.removeStart(pullRequest.getFromRef().getId(), GitRefPattern.HEADS.getPath()) + " -> " +
                    StringUtils.removeStart(pullRequest.getToRef().getId(), GitRefPattern.HEADS.getPath()));
            notification.append("\n");

            //Getting PR reviewers info:
            Set<PullRequestParticipant> pullRequestParticipants = pullRequest.getReviewers();
            if(pullRequestParticipants.size() > 0) {
                notification.append(pullRequestParticipants.size() > 1 ? "Reviewers: " : "Reviewer: ");

            /*
            Java 8 still unsupported by atlassian sdk :/
            Use the following commented code when we get the support
             */
//            notification.append(pullRequestParticipants.stream()
//                    .map(PullRequestParticipant::getUser)
//                    .map(StashUser::getDisplayName)
//                    .collect(Collectors.joining(", ")));

                Iterator<PullRequestParticipant> iterator = pullRequestParticipants.iterator();
                String delim = "";
                while (iterator.hasNext()){
                    notification.append(delim).append(iterator.next().getUser().getDisplayName());
                    delim = ", ";
                }
                notification.append("\n");
            }
        }
        notification.append("URL: " + navBuilder.repo(repository).pullRequest(pullRequest.getId()).buildAbsolute());
        return notification;
    }
}