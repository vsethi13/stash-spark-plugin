package com.cisco.bitbucket.plugin.event;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.CommitsBetweenRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.git.GitRefPattern;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.cisco.bitbucket.plugin.pojo.KeyValue;
import com.cisco.bitbucket.plugin.pojo.RefType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

/**
 * Created by Sagar on 10/03/17.
 */
@Service
public class RefChangeEvent {

    private CommitService commitService;
    private AuthenticationContext authenticationContext;
    private NavBuilder navBuilder;

    private static final String MERGE_PR_COMMIT_MSG = "Merge pull request #";
    private static final int MAX_COMMITS_TO_SHOW = 5;
    private static final int MAX_COMMITS = 15;
    private static final Logger log = LoggerFactory.getLogger(RefChangeEvent.class);

    private static final PageRequestImpl PAGE_REQUEST = new PageRequestImpl(0, MAX_COMMITS);

    public RefChangeEvent(AuthenticationContext authenticationContext, CommitService commitService, NavBuilder navBuilder) {
        this.authenticationContext = authenticationContext;
        this.commitService = commitService;
        this.navBuilder = navBuilder;
    }

    /**
     * creates notification related to addition, updation (new commits) and deletion of refs on a particular repo
     *
     * @param repositoryHookContext
     * @param refChanges
     * @return
     */
    public StringBuilder createRefChangeNotification(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges) {
        StringBuilder notification = new StringBuilder(1024);
        List<KeyValue> commitLinks = new ArrayList<KeyValue>();
        List<KeyValue> addedRefs = new ArrayList<KeyValue>();
        ApplicationUser stashUser = authenticationContext.getCurrentUser();
        Repository repository = repositoryHookContext.getRepository();
        notification.append(stashUser.getDisplayName() + " ");
        notification.append("committed to " + refChanges.size() + ((refChanges.size() > 1) ? " refs " : " ref "));
        notification.append("at " + "\"" + repository.getProject().getName() + "/" + repository.getName() + "\"");
        notification.append("\n");

        String refType;
        String displayRefId;

        for (RefChange refChange : refChanges) {

            refType = RefType.DEFAULT;
            displayRefId = refChange.getRef().getId();

            if (getRefType(refChange).equals(RefType.BRANCH)) {
                refType = RefType.BRANCH;
                displayRefId = StringUtils.removeStart(refChange.getRef().getId(), GitRefPattern.HEADS.getPath());
            } else if (getRefType(refChange).equals(RefType.TAG)) {
                refType = RefType.TAG;
                displayRefId = StringUtils.removeStart(refChange.getRef().getId(), GitRefPattern.TAGS.getPath());
            }

            if (refChange.getType() == RefChangeType.ADD) {
                notification.append("New " + refType + " \"" + displayRefId + "\"" + " has been added to the repo");
                addedRefs.add(new KeyValue(displayRefId, navBuilder.repo(repository).browse().atRevision(refChange.getRef().getId()).buildAbsolute()));
                notification.append("\n");
            } else if (refChange.getType() == RefChangeType.DELETE) {
                notification.append("The " + refType + " \"" + displayRefId + "\"" + " has been deleted from the repo");
                notification.append("\n");
            } else if (refChange.getType() == RefChangeType.UPDATE) {
                notification.append("On " + refType + " \"" + displayRefId + "\"" + ":");
                notification.append("\n");

                CommitsBetweenRequest commitsBetweenRequest;
                commitsBetweenRequest = new CommitsBetweenRequest.Builder(repository)
                        .include(refChange.getToHash())
                        .exclude(refChange.getFromHash())
                        .build();

                Page<Commit> commits = commitService.getCommitsBetween(commitsBetweenRequest, PAGE_REQUEST);
                SortedMap<Integer, Commit> commitMap = commits.getOrdinalIndexedValues();

                //Don't publish the notification if it's a PR merge (merge commit is always the first commit in order)
                if (commitMap.get(Integer.valueOf(0)).getMessage().startsWith(MERGE_PR_COMMIT_MSG)) {
                    return null;
                }
                int reverseCommitCounter = commits.getSize();
                while (reverseCommitCounter > (commits.getSize() > MAX_COMMITS_TO_SHOW ? commits.getSize() - MAX_COMMITS_TO_SHOW : 0)) {
                    Commit commit = commitMap.get(Integer.valueOf(reverseCommitCounter - 1));
                    notification.append("- " + commit.getMessage() + " (" + commit.getDisplayId() + ") ");
//                    notification.append("@ " + commit.getAuthorTimestamp());
                    notification.append("\n");
                    commitLinks.add(new KeyValue(commit.getDisplayId(), navBuilder.repo(repository).commit(commit.getId()).buildConfigured()));
                    reverseCommitCounter--;
                }
                if (commits.getSize() > MAX_COMMITS_TO_SHOW) {
                    int moreCommits = commits.getSize() - MAX_COMMITS_TO_SHOW;
                    notification.append("and " + (commits.getIsLastPage() ? moreCommits : (moreCommits + "+")) + " more...");
                    notification.append("\n");
                }
            }

        }
        notification.append("Repo URL: \n" + navBuilder.repo(repository).buildConfigured());
        notification.append("\n");

        if (commitLinks.size() > 0) {
            notification.append("Commit URL(s): \n");
            for (KeyValue commitEntry : commitLinks) {
                notification.append("- " + commitEntry.getKey() + ": " + commitEntry.getValue());
                notification.append("\n");
            }
        }

        if (addedRefs.size() > 0) {
            notification.append("Ref URL(s): \n");
            for (KeyValue addRefEntry : addedRefs) {
                notification.append("- " + addRefEntry.getKey() + ": " + addRefEntry.getValue());
                notification.append("\n");
            }
        }

        int notificationLength = notification.length();
        if (notification.charAt(notificationLength - 1) == '\n')
            notification.deleteCharAt(notificationLength - 1);

        return notification;
    }

    /**
     * returns the type of the ref in question
     *
     * @param ref
     * @return
     */
    private String getRefType(RefChange ref) {
        if (ref.getRef().getId().startsWith(GitRefPattern.HEADS.getPath()))
            return RefType.BRANCH;
        else if (ref.getRef().getId().startsWith(GitRefPattern.TAGS.getPath()))
            return RefType.TAG;
        return RefType.DEFAULT;
    }
}