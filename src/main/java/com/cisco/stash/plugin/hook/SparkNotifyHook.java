package com.cisco.stash.plugin.hook;

import com.atlassian.stash.commit.Commit;
import com.atlassian.stash.commit.CommitService;
import com.atlassian.stash.commit.CommitsBetweenRequest;
import com.atlassian.stash.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.scm.git.GitRefPattern;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.atlassian.stash.util.PageRequestImpl;
import com.cisco.stash.plugin.Notifier;
import com.cisco.stash.plugin.pojo.RefType;
import com.cisco.stash.plugin.publisher.SparkPublisher;
import com.cisco.stash.plugin.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SparkNotifyHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private CommitService commitService;
    private StashAuthenticationContext stashAuthenticationContext;
    private NavBuilder navBuilder;
    private SettingsService settingsService;

    private static final Logger log = LoggerFactory.getLogger(SparkNotifyHook.class);

    public SparkNotifyHook(StashAuthenticationContext stashAuthenticationContext, CommitService commitService, NavBuilder navBuilder, SettingsService settingsService) {
        this.commitService = commitService;
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.settingsService = settingsService;
        this.navBuilder = navBuilder;
    }

    /**
     * Connects to a configured URL to notify of all changes.
     * @param repositoryHookContext
     * @param refChanges
     */
    @Override
    public void postReceive(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges) {

        Settings repoSettings = settingsService.getSettings(repositoryHookContext.getRepository(), Notifier.REPO_HOOK_KEY);
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
        Map<String, String> commitLinks = new HashMap<String, String>();
        Map<String, String> addedRefs = new HashMap<String, String>();
        StashUser stashUser = stashAuthenticationContext.getCurrentUser();
        Repository repository = repositoryHookContext.getRepository();
        notification.append(stashUser.getDisplayName() + " ");
        notification.append("committed to " + refChanges.size() +  ((refChanges.size() > 1) ? " branches " : " branch "));
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
                addedRefs.put(displayRefId, navBuilder.repo(repository).browse().atRevision(refChange.getRefId()).buildAbsolute());
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

                //TODO: do something about the hardcoded literals
                //TODO: limit amount of commit info to be displayed
                for (Commit commit : commitService.getCommitsBetween(commitsBetweenRequest, new PageRequestImpl(0, 10)).getValues()) {
                    notification.append("- " + commit.getMessage() + " (" + commit.getDisplayId() + ") ");
//                    notification.append("@ " + commit.getAuthorTimestamp());
                    notification.append("\n");
                    commitLinks.put(commit.getDisplayId(), navBuilder.repo(repository).commit(commit.getId()).buildConfigured());
                }
            }

        }
        notification.append("Repo URL: \n" + navBuilder.repo(repository).buildConfigured());
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
}