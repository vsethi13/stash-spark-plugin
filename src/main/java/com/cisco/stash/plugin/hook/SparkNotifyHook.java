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
import com.atlassian.stash.util.PageRequestImpl;
import com.cisco.stash.plugin.util.Notifier;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SparkNotifyHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private CommitService commitService;
    private StashAuthenticationContext stashAuthenticationContext;
    private RepositoryHookService repositoryHookService;
    private SecurityService securityService;
    private NavBuilder navBuilder;
    private static final Logger log = LoggerFactory.getLogger(SparkNotifyHook.class);

    public SparkNotifyHook(StashAuthenticationContext stashAuthenticationContext, CommitService commitService, RepositoryHookService repositoryHookService, SecurityService securityService, NavBuilder navBuilder) {
        this.commitService = commitService;
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.repositoryHookService = repositoryHookService;
        this.securityService = securityService;
        this.navBuilder = navBuilder;
    }

    /**
     * Connects to a configured URL to notify of all changes.
     */
    @Override
    public void postReceive(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges) {

        Settings repoSettings = getSettings(repositoryHookContext.getRepository());
        if(repoSettings != null) {
            new Notifier().publishNotification(repoSettings.getString(Notifier.ROOM_ID), createRefChangeNotification(repositoryHookContext, refChanges));
        }
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        if (settings.getString(Notifier.ROOM_ID, "").isEmpty()) {
            errors.addFieldError(Notifier.ROOM_ID, "'Room Id' field is blank, please supply one");
        }

        if(settings.getString(Notifier.BEARER_TOKEN, "").isEmpty()) {
            errors.addFieldError(Notifier.BEARER_TOKEN, "'Bearer Token' field is blank, please supply one");
            //TODO: add machine account to spark room
        }
    }

    private StringBuilder createRefChangeNotification(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges){
        StringBuilder notification = new StringBuilder(1024);
        Map<String, String> commitLinks = new HashMap<String, String>();
        Map<String, String> addedRefs = new HashMap<String, String>();
        StashUser stashUser = stashAuthenticationContext.getCurrentUser();
        Repository repository = repositoryHookContext.getRepository();
        notification.append(stashUser.getDisplayName() + "[" + stashUser.getEmailAddress() + "] ");
        notification.append("committed to " + refChanges.size() + " branch(es) ");
        notification.append("at " + repository.getProject().getName() + "/" + repository.getName());
        notification.append("\n");

        for(RefChange refChange : refChanges){
            if(refChange.getType() == RefChangeType.ADD){
                notification.append("New ref " + StringUtils.removeStart(refChange.getRefId(), GitRefPattern.HEADS.getPath()) + " has been added to the repo");
                addedRefs.put(StringUtils.removeStart(refChange.getRefId(), GitRefPattern.HEADS.getPath()), navBuilder.repo(repository).browse().atRevision(refChange.getRefId()).buildAbsolute());
                notification.append("\n");
            }
            else if(refChange.getType() == RefChangeType.DELETE){
                notification.append("The ref " + StringUtils.removeStart(refChange.getRefId(), GitRefPattern.HEADS.getPath()) + " has been deleted from the repo");
                notification.append("\n");
            }
            else if(refChange.getType() == RefChangeType.UPDATE){
                notification.append("On ref " + StringUtils.removeStart(refChange.getRefId(), GitRefPattern.HEADS.getPath()) + ":");
                notification.append("\n");

                CommitsBetweenRequest commitsBetweenRequest;
                commitsBetweenRequest = new CommitsBetweenRequest.Builder(repository)
                        .include(refChange.getToHash())
                        .exclude(refChange.getFromHash())
                        .build();

                //TODO: do something about the hardcoded literals
                //TODO: limit amount of commit info to be displayed
                for (Commit commit : commitService.getCommitsBetween(commitsBetweenRequest, new PageRequestImpl(0, 10)).getValues()) {
                    notification.append("- " + commit.getMessage() + "(" + commit.getDisplayId() + ") ");
                    notification.append("@ " + commit.getAuthorTimestamp());
                    notification.append("\n");
                    commitLinks.put(commit.getDisplayId(), navBuilder.repo(repository).commit(commit.getId()).buildConfigured());
                }
            }
        }
        notification.append("Repo URL: " + navBuilder.repo(repository).buildConfigured());
        notification.append("\n");
        if(commitLinks.size() > 0) {
            notification.append("Commit URL(s): \n");
            for (Map.Entry<String, String> commitEntry : commitLinks.entrySet()) {
                notification.append("- " + commitEntry.getKey() + ": " + commitEntry.getValue());
                notification.append("\n");
            }
        }

        if(addedRefs.size() > 0) {
            notification.append("Ref URL(s): \n");
            for(Map.Entry<String, String> addRefEntry : addedRefs.entrySet()){
                notification.append("- " + addRefEntry.getKey() + ": " + addRefEntry.getValue());
                notification.append("\n");
            }
        }

        int notificationLength = notification.length();
        if(notification.charAt(notificationLength-1) == '\n')
            notification.deleteCharAt(notificationLength-1);

        return notification;
    }

    private Settings getSettings(final Repository repository){

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

        return settings;
    }
}