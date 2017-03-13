package com.cisco.bitbucket.plugin.event;

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
import com.atlassian.event.api.EventListener;
import com.cisco.bitbucket.plugin.Notifier;
import com.cisco.bitbucket.plugin.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Sagar on 13/05/15.
 */
public class PullRequestEventListener {

    private SettingsService settingsService;
    private NavBuilder navBuilder;

    private static final String NEWLINE = "\n<br>";
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
    private void handlePullRequestEvent(PullRequestEvent event) {

        final Repository repository = event.getPullRequest().getToRef().getRepository();
        Settings repoSettings = settingsService.getSettings(repository, Notifier.REPO_HOOK_KEY);
        if (repoSettings == null)
            log.info("Settings not found.");
        else {
            new Notifier().pushNotification(repoSettings.getString(Notifier.SPACE_ID, ""), createPrNotification(event));
        }
    }

    /**
     * create a notification for a PR related event
     *
     * @param event
     * @return
     */
    private Map<String, String> createPrNotification(PullRequestEvent event) {
        Map<String, String> notificationMap = new LinkedHashMap<>();
        StringBuilder notification = new StringBuilder(256);
        PullRequest pullRequest = event.getPullRequest();
        Repository repository = pullRequest.getToRef().getRepository();
        ApplicationUser user = event.getUser();

        //If event is related to a PR comment
        if (event.getAction() == PullRequestAction.COMMENTED) {
            PullRequestCommentEvent commentEvent = (PullRequestCommentEvent) event;
            notification.append(user.getDisplayName() + " " + commentEvent.getCommentAction().toString().toLowerCase());
            notification.append(commentEvent.getCommentAction().toString().equals(String.valueOf(CommentAction.REPLIED)) ? " with a comment" : " a comment");
            notification.append(" on pull request " + "#" + pullRequest.getId());
            notification.append(" in \"" + repository.getProject().getName() + "/" + repository.getName() + "\"");
            notification.append(NEWLINE);
            notification.append("Comment: " + commentEvent.getComment().getText());
            notification.append(NEWLINE);

            //for all other events other than "COMMENT" event
        } else {
            StringBuilder message = new StringBuilder(512);
            StringBuilder title = new StringBuilder(128);
            StringBuilder scope = new StringBuilder(128);
            StringBuilder reviewers = new StringBuilder(128);

            message.append("Pull Request ").append(getPrInfoWithMarkdownFmt(repository, pullRequest));
            message.append(" ").append(event.getAction().toString().toLowerCase()).append(" by ").append(user.getDisplayName());
            message.append(" in ").append(getRepoInfoWithMarkdownFmt(repository));

            title.append(pullRequest.getTitle());

            scope.append(StringUtils.removeStart(pullRequest.getFromRef().getId(), GitRefPattern.HEADS.getPath()))
                    .append(" -> ")
                    .append(StringUtils.removeStart(pullRequest.getToRef().getId(), GitRefPattern.HEADS.getPath()));

            notificationMap.put("Message", message.toString());
            notificationMap.put("Title", title.toString());
            notificationMap.put("Scope", scope.toString());

            //Getting PR reviewers info:
            Set<PullRequestParticipant> pullRequestParticipants = pullRequest.getReviewers();
            if (!pullRequestParticipants.isEmpty()) {
                String reviewerStr = pullRequestParticipants.size() > 1 ? "Reviewers: " : "Reviewer: ";
                reviewers.append(pullRequestParticipants.stream()
                        .map(PullRequestParticipant::getUser)
                        .map(ApplicationUser::getDisplayName)
                        .collect(Collectors.joining(", ")));

                notificationMap.put(reviewerStr, reviewers.toString());
            }
        }
        return notificationMap;
    }

    /**
     * gets repo's info with Markdown formatting:
     * syntax: [project/repo](repoUrl)
     * example: (ignore quotes)
     * '[rep_1](localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/browse)'
     *
     * @param repository
     * @return
     */
    private String getRepoInfoWithMarkdownFmt(Repository repository) {

        return "[" + repository.getProject().getName() + "/" + repository.getName() + "]" +
                "(" + navBuilder.repo(repository).buildAbsolute() + ")";
    }

    /**
     * gets PR's id with Markdown formatting:
     * syntax: [#PR Id](PR Url)
     * example: (ignore quotes)
     * '[#42](localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/pull-requests/42)'
     *
     * @param repository
     * @param pullRequest
     * @return
     */
    private String getPrInfoWithMarkdownFmt(Repository repository, PullRequest pullRequest) {

        return "[#" + pullRequest.getId() + "]" +
                "(" + navBuilder.repo(repository).pullRequest(pullRequest.getId()).buildAbsolute() + ")";
    }
}