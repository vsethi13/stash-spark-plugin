package com.cisco.stash.plugin.hook;

import com.atlassian.stash.commit.Commit;
import com.atlassian.stash.commit.CommitService;
import com.atlassian.stash.commit.CommitsBetweenRequest;
import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.scm.git.GitRefPattern;
import com.atlassian.stash.setting.*;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.atlassian.stash.util.Operation;
import com.atlassian.stash.util.Page;
import com.atlassian.stash.util.PageRequestImpl;
import com.cisco.stash.plugin.Notifier;
import com.cisco.stash.plugin.pojo.KeyValue;
import com.cisco.stash.plugin.pojo.RefType;
import com.cisco.stash.plugin.publisher.SparkPublisher;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SparkNotifyHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private CommitService commitService;
    private StashAuthenticationContext stashAuthenticationContext;
    private RepositoryHookService repositoryHookService;
    private SecurityService securityService;
    private NavBuilder navBuilder;

    private static final int MAX_COMMITS_TO_SHOW = 5;
    private static final int MAX_COMMITS = 15;
    private static final Logger log = LoggerFactory.getLogger(SparkNotifyHook.class);

    private static final PageRequestImpl PAGE_REQUEST = new PageRequestImpl(0, MAX_COMMITS);

    public SparkNotifyHook(StashAuthenticationContext stashAuthenticationContext, CommitService commitService, RepositoryHookService repositoryHookService, SecurityService securityService, NavBuilder navBuilder) {
        this.commitService = commitService;
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.repositoryHookService = repositoryHookService;
        this.securityService = securityService;
        this.navBuilder = navBuilder;
    }

    /**
     * Connects to a configured URL to notify of all changes.
     * @param repositoryHookContext
     * @param refChanges
     */
    @Override
    public void postReceive(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges) {

        Settings repoSettings = getSettings(repositoryHookContext.getRepository(), Notifier.REPO_HOOK_KEY);
        if(repoSettings != null) {
            new Notifier().pushNotification(repoSettings.getString(Notifier.ROOM_ID, ""), createRefChangeNotification(repositoryHookContext, refChanges));
        }
    }

    /**
     * validates configuration settings of a hook
     * @param settings
     * @param errors
     * @param repository
     */
    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        if (settings.getString(Notifier.ROOM_ID, "").isEmpty()) {
            errors.addFieldError(Notifier.ROOM_ID, "'Room Id' field is blank, please supply one");
        }

        if(settings.getString(Notifier.BEARER_TOKEN, "").isEmpty()) {
            errors.addFieldError(Notifier.BEARER_TOKEN, "'Bearer Token' field is blank, please supply one");
        }

        SparkPublisher.invite(settings.getString(Notifier.ROOM_ID, ""), settings.getString(Notifier.BEARER_TOKEN, ""));
    }

    /**
     * creates notification related to addition, updatation (new commits) and deletion of refs on a particular repo
     * @param repositoryHookContext
     * @param refChanges
     * @return
     */
    private StringBuilder createRefChangeNotification(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges){
        StringBuilder notification = new StringBuilder(1024);
        List<KeyValue> commitLinks = new ArrayList<KeyValue>();
        List<KeyValue> addedRefs = new ArrayList<KeyValue>();
        StashUser stashUser = stashAuthenticationContext.getCurrentUser();
        Repository repository = repositoryHookContext.getRepository();
        notification.append(stashUser.getDisplayName() + " ");
        notification.append("committed to " + refChanges.size() +  ((refChanges.size() > 1) ? " refs " : " ref "));
        notification.append("at " + "\"" + repository.getProject().getName() + "/" + repository.getName() + "\"");
        notification.append("\n");

        String refType;
        String displayRefId;

        for(RefChange refChange : refChanges){

            refType = RefType.DEFAULT;
            displayRefId = refChange.getRefId();

            if(getRefType(refChange).equals(RefType.BRANCH)){
                refType = RefType.BRANCH;
                displayRefId = StringUtils.removeStart(refChange.getRefId(), GitRefPattern.HEADS.getPath());
            }

            else if(getRefType(refChange).equals(RefType.TAG)){
                refType = RefType.TAG;
                displayRefId = StringUtils.removeStart(refChange.getRefId(), GitRefPattern.TAGS.getPath());
            }

            if (refChange.getType() == RefChangeType.ADD) {
                notification.append("New " + refType + " \"" + displayRefId + "\"" + " has been added to the repo");
                addedRefs.add(new KeyValue(displayRefId, navBuilder.repo(repository).browse().atRevision(refChange.getRefId()).buildAbsolute()));
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
                int reverseCommitCounter = commits.getSize();
                while(reverseCommitCounter > (commits.getSize() > MAX_COMMITS_TO_SHOW ? commits.getSize() - MAX_COMMITS_TO_SHOW : 0)){
                    Commit commit = commitMap.get(Integer.valueOf(reverseCommitCounter-1));
                    notification.append("- " + commit.getMessage() + " (" + commit.getDisplayId() + ") ");
//                    notification.append("@ " + commit.getAuthorTimestamp());
                    notification.append("\n");
                    commitLinks.add(new KeyValue(commit.getDisplayId(), navBuilder.repo(repository).commit(commit.getId()).buildConfigured()));
                    reverseCommitCounter--;
                }
                if(commits.getSize() > MAX_COMMITS_TO_SHOW) {
                    int moreCommits = commits.getSize() - MAX_COMMITS_TO_SHOW;
                    notification.append("and " + (commits.getIsLastPage() ? moreCommits : (moreCommits + "+")) + " more...");
                    notification.append("\n");
                }
            }

        }
        notification.append("Repo URL: \n" + navBuilder.repo(repository).buildConfigured());
        notification.append("\n");

        if(commitLinks.size() > 0) {
            notification.append("Commit URL(s): \n");
            for (KeyValue commitEntry : commitLinks) {
                notification.append("- " + commitEntry.getKey() + ": " + commitEntry.getValue());
                notification.append("\n");
            }
        }

        if(addedRefs.size() > 0) {
            notification.append("Ref URL(s): \n");
            for(KeyValue addRefEntry : addedRefs){
                notification.append("- " + addRefEntry.getKey() + ": " + addRefEntry.getValue());
                notification.append("\n");
            }
        }

        int notificationLength = notification.length();
        if(notification.charAt(notificationLength-1) == '\n')
            notification.deleteCharAt(notificationLength-1);

        return notification;
    }

    /**
     * returns the type of the ref in question
     * @param ref
     * @return
     */
    private String getRefType(RefChange ref){
        if (ref.getRefId().startsWith(GitRefPattern.HEADS.getPath()))
            return RefType.BRANCH;
        else if(ref.getRefId().startsWith(GitRefPattern.TAGS.getPath()))
            return RefType.TAG;
        return RefType.DEFAULT;
    }

    /**
     * checks if a hook is enabled on a particular repository or not
     * @param repository
     * @param hookKey
     * @return
     */
    private boolean isHookEnabled(final Repository repository, final String hookKey){
        try {
            return securityService.withPermission(Permission.REPO_ADMIN, "Access required to check hook's status").call(new Operation<Boolean, Exception>() {
                @Override
                public Boolean perform() throws Exception {
                    return repositoryHookService.getByKey(repository, hookKey).isEnabled();
                }
            });
        } catch (Exception e) {
            log.error("Unexpected exception trying to get hold of the hook");
        }
        return false;
    }

    /**
     * get the settings of an hook that is enabled on a particular repository
     * @param repository
     * @param hookKey
     * @return
     */
    private Settings getSettings(final Repository repository, final String hookKey) {

        Settings settings = null;
        if (isHookEnabled(repository, hookKey)) {
            try {
                settings = securityService.withPermission(Permission.REPO_ADMIN, "Access required to get hook settings.").call(new Operation<Settings, Exception>() {
                    @Override
                    public Settings perform() throws Exception {
                        return repositoryHookService.getSettings(repository, hookKey);
                    }
                });
            } catch (Exception e) {
                log.error("Unexpected exception trying to get the hook settings");
            }

            if (settings == null)
                log.error("Settings not found");
        } else {
            log.error(hookKey + " is not enabled on " + repository.getName());
        }

        return settings;
    }
}