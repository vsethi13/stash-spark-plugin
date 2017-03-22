package com.cisco.bitbucket.plugin.event;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.CommitsBetweenRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.cisco.bitbucket.plugin.utils.MarkDownInfoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * Created by Sagar on 10/03/17.
 */
@Service
public class RefChangeEvent {

    private CommitService commitService;
    private AuthenticationContext authenticationContext;
    private MarkDownInfoUtil markDownInfoUtil;

    private static final String MERGE_PR_COMMIT_MSG = "Merge pull request #";
    private static final String NEWLINE = "\n<br>";
    private static final int MAX_COMMITS_TO_SHOW = 5;
    private static final int MAX_COMMITS = 15;
    private static final Logger log = LoggerFactory.getLogger(RefChangeEvent.class);

    public RefChangeEvent(AuthenticationContext authenticationContext, CommitService commitService, MarkDownInfoUtil markDownInfoUtil) {
        this.authenticationContext = authenticationContext;
        this.commitService = commitService;
        this.markDownInfoUtil = markDownInfoUtil;
    }

    /**
     * creates notification related to addition, updation (new commits) and deletion of refs on a particular repo
     *
     * @param repositoryHookContext
     * @param refChanges
     * @return
     */
    public Map<String, String> createRefChangeNotification(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges) {
        Map<String, String> notificationMap = new LinkedHashMap<>();
        StringBuilder message = new StringBuilder(512);
        StringBuilder details = new StringBuilder(2048);
        ApplicationUser currentUser = authenticationContext.getCurrentUser();
        Repository repository = repositoryHookContext.getRepository();

        assert currentUser != null;
        message.append(markDownInfoUtil.getUserInfoWithMarkdownFmt(currentUser));
        message.append(" committed to ").append(refChanges.size()).append((refChanges.size() > 1) ? " refs" : " ref");
        message.append(" at ").append(markDownInfoUtil.getRepoInfoWithMarkdownFmt(repository));

        for (RefChange refChange : refChanges) {
            if (refChange.getType() == RefChangeType.ADD) {
                details.append("New ").append(markDownInfoUtil.getRefInfoWithMarkdownFmt(refChange, repository, false))
                        .append(" has been added to the repo");
                details.append(NEWLINE);
            } else if (refChange.getType() == RefChangeType.DELETE) {
                details.append("The ").append(markDownInfoUtil.getRefInfoWithMarkdownFmt(refChange, repository, true))
                        .append(" has been deleted from the repo");
                details.append(NEWLINE);
            } else if (refChange.getType() == RefChangeType.UPDATE) {
                details.append("On ").append(markDownInfoUtil.getRefInfoWithMarkdownFmt(refChange, repository, false));
                details.append(NEWLINE);

                Page<Commit> commits = getLastNCommits(refChange, repository);
                SortedMap<Integer, Commit> commitMap = commits.getOrdinalIndexedValues();

                //Don't publish the notification if it's a PR merge (merge commit is always the first commit in order)
                if (commitMap.get(0).getMessage().startsWith(MERGE_PR_COMMIT_MSG)) {
                    return null;
                }
                int reverseCommitCounter = commits.getSize();
                while (reverseCommitCounter > (commits.getSize() > MAX_COMMITS_TO_SHOW ? commits.getSize() - MAX_COMMITS_TO_SHOW : 0)) {
                    Commit commit = commitMap.get(reverseCommitCounter - 1);
                    details.append(markDownInfoUtil.getCommitInfoWithMarkdownFmt(commit, repository));
                    details.append(NEWLINE);
                    reverseCommitCounter--;
                }
                if (commits.getSize() > MAX_COMMITS_TO_SHOW) {
                    int moreCommits = commits.getSize() - MAX_COMMITS_TO_SHOW;
                    details.append("and ").append(commits.getIsLastPage() ? moreCommits : (moreCommits + "+")).append(" more...");
                    details.append(NEWLINE);
                }
            }
        }
        //delete the last NEWLINE
        details.delete(details.length() - NEWLINE.length(), details.length());
        notificationMap.put("Message", message.toString());
        notificationMap.put("Details", details.toString());
        return notificationMap;
    }

    /**
     * returns last @MAX_COMMITS number of new commits on a ref
     *
     * @param refChange
     * @param repository
     * @return
     */
    private Page<Commit> getLastNCommits(RefChange refChange, Repository repository) {

        PageRequestImpl pageRequest = new PageRequestImpl(0, MAX_COMMITS);
        CommitsBetweenRequest commitsBetweenRequest;
        commitsBetweenRequest = new CommitsBetweenRequest.Builder(repository)
                .include(refChange.getToHash())
                .exclude(refChange.getFromHash())
                .build();
        return commitService.getCommitsBetween(commitsBetweenRequest, pageRequest);
    }
}
