package com.cisco.bitbucket.plugin.utils;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.git.GitRefPattern;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.cisco.bitbucket.plugin.pojo.RefType;
import org.apache.commons.lang.StringUtils;

/**
 * Created by Sagar on 21/03/17.
 */
public class MarkDownInfoUtil {

    private NavBuilder navBuilder;

    public MarkDownInfoUtil(NavBuilder navBuilder) {
        this.navBuilder = navBuilder;
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
    public String getUserInfoWithMarkdownFmt(ApplicationUser user) {
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
    public String getCommitInfoWithMarkdownFmt(Commit commit, Repository repository) {
        return "- " + commit.getMessage() +
                " ([" + commit.getDisplayId() + "]" +
                "(" + navBuilder.repo(repository).commit(commit.getId()).buildConfigured() + "))";
    }

    /**
     * gets repo's info with Markdown formatting:
     * syntax: [repo](repoUrl)
     * example: (ignore quotes)
     * '[rep_1](localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/browse)'
     *
     * @param repository
     * @return
     */
    public String getRepoInfoWithMarkdownFmt(Repository repository) {

        return "[" + repository.getName() + "]" +
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
    public String getPrInfoWithMarkdownFmt(Repository repository, PullRequest pullRequest) {

        return "[#" + pullRequest.getId() + "]" +
                "(" + navBuilder.repo(repository).pullRequest(pullRequest.getId()).buildAbsolute() + ")";
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
    public String getRefInfoWithMarkdownFmt(RefChange refChange, Repository repository, boolean deleteFlag) {

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
}
