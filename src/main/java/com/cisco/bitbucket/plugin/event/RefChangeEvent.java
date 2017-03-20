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
import com.cisco.bitbucket.plugin.pojo.RefType;
import org.apache.commons.lang.StringUtils;
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
    private NavBuilder navBuilder;

    private static final String MERGE_PR_COMMIT_MSG = "Merge pull request #";
    private static final String NEWLINE = "\n<br>";
    private static final int MAX_COMMITS_TO_SHOW = 5;
    private static final int MAX_COMMITS = 15;
    private static final Logger log = LoggerFactory.getLogger(RefChangeEvent.class);

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
    public Map<String, String> createRefChangeNotification(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges) {
        Map<String, String> notificationMap = new LinkedHashMap<>();
        StringBuilder message = new StringBuilder(512);
        StringBuilder details = new StringBuilder(2048);
        ApplicationUser currentUser = authenticationContext.getCurrentUser();
        Repository repository = repositoryHookContext.getRepository();

        assert currentUser != null;
        message.append(getUserInfoWithMarkdownFmt(currentUser));
        message.append(" committed to ").append(refChanges.size()).append((refChanges.size() > 1) ? " refs" : " ref");
        message.append(" at [").append(repository.getName()).append("]");
        message.append("(").append(navBuilder.repo(repository).buildConfigured()).append(")");

        for (RefChange refChange : refChanges) {
            if (refChange.getType() == RefChangeType.ADD) {
                details.append("New ").append(getRefInfoWithMarkdownFmt(refChange, repository, false))
                        .append(" has been added to the repo");
                details.append(NEWLINE);
            } else if (refChange.getType() == RefChangeType.DELETE) {
                details.append("The ").append(getRefInfoWithMarkdownFmt(refChange, repository, true))
                        .append(" has been deleted from the repo");
                details.append(NEWLINE);
            } else if (refChange.getType() == RefChangeType.UPDATE) {
                details.append("On ").append(getRefInfoWithMarkdownFmt(refChange, repository, false));
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
                    details.append(getCommitInfoWithMarkdownFmt(commit, repository));
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
     * gets commit's info in Markdown formatting:
     * syntax: [userDisplayName] (CDA Developer Url)
     * example: (ignore quotes)
     * '[Vivek Sethi] (https://cdanalytics.cisco.com/developer/developer/vivekse/summary)'
     *
     * @param user
     * @return
     */
    private String getUserInfoWithMarkdownFmt(ApplicationUser user) {
        return "[" + user.getDisplayName() + "]" +
                "(" + "https://cdanalytics.cisco.com/developer/developer/" + user.getSlug() + "/summary" + ")";
    }

    /**
     * gets commit's info in Markdown formatting:
     * syntax: - commitMessage ([commitDisplayId](commitURL))
     * example: (ignore quotes)
     * '- test commit message (([ef5ef861edc](http://localhost:7990/.../commits/ef5ef861edcc6a8d0b6e83c1963be0db78de02ea))'
     *
     * @param commit
     * @param repository
     * @return
     */
    private String getCommitInfoWithMarkdownFmt(Commit commit, Repository repository) {
        return "- " + commit.getMessage() +
                " ([" + commit.getDisplayId() + "]" +
                "(" + navBuilder.repo(repository).commit(commit.getId()).buildConfigured() + "))";
    }

    /**
     * gets ref's info in Markdown formatting:
     * syntax: refType [refName](refURL)
     * example: (ignore quotes)
     * 'branch [master](localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/browse?at=refs/heads/master)'
     *
     * @param refChange
     * @param repository
     * @return
     */
    private String getRefInfoWithMarkdownFmt(RefChange refChange, Repository repository, boolean deleteFlag) {

        String refType;
        String displayRefId;
        if (getRefType(refChange).equals(RefType.BRANCH)) {
            refType = RefType.BRANCH;
            displayRefId = StringUtils.removeStart(refChange.getRef().getId(), GitRefPattern.HEADS.getPath());
        } else if (getRefType(refChange).equals(RefType.TAG)) {
            refType = RefType.TAG;
            displayRefId = StringUtils.removeStart(refChange.getRef().getId(), GitRefPattern.TAGS.getPath());
        } else {
            refType = RefType.DEFAULT;
            displayRefId = refChange.getRef().getId();
        }
        if (!deleteFlag) {
            return refType + " [" + displayRefId + "]" +
                    "(" + navBuilder.repo(repository).browse().atRevision(refChange.getRef().getId()).buildAbsolute() + ")";
        } else {
            return refType + " \"" + displayRefId + "\"";
        }
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
