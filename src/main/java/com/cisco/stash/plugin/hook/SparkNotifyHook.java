package com.cisco.stash.plugin.hook;

import com.atlassian.stash.commit.Commit;
import com.atlassian.stash.commit.CommitService;
import com.atlassian.stash.commit.CommitsBetweenRequest;
import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.setting.*;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.atlassian.stash.util.PageRequestImpl;
import java.util.Collection;

public class SparkNotifyHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private final CommitService commitService;
    private final StashAuthenticationContext stashAuthenticationContext;

    public SparkNotifyHook(CommitService commitService, StashAuthenticationContext stashAuthenticationContext) {
        this.commitService = commitService;
        this.stashAuthenticationContext = stashAuthenticationContext;
    }

    /**
     * Connects to a configured URL to notify of all changes.
     */
    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
        StashUser currentUser = stashAuthenticationContext.getCurrentUser();
        System.out.println(currentUser.getDisplayName() + "[" + currentUser.getEmailAddress() + "]" + " committed to " + refChanges.size() + " branch(es) at " + context.getRepository().getProject().getName() + "/" + context.getRepository().getName());

        for (RefChange refChange : refChanges) {
            if (refChange.getType() == RefChangeType.ADD) {
                System.out.println("New ref '" + refChange.getRefId() + "' created.");
            } else if (refChange.getType() == RefChangeType.UPDATE) {
                System.out.println("On ref '" + refChange.getRefId() + "':");

                CommitsBetweenRequest commitsBetweenRequest;
                commitsBetweenRequest = new CommitsBetweenRequest.Builder(context.getRepository())
                        .include(refChange.getToHash())
                        .exclude(refChange.getFromHash())
                        .build();

                //todo: do something about the hardcoded literals
                for (Commit commit : commitService.getCommitsBetween(commitsBetweenRequest, new PageRequestImpl(0, 100)).getValues()) {
                    System.out.println("- " + commit.getMessage() + "(" + commit.getDisplayId() + ") @ " + commit.getAuthorTimestamp());
                }

            } else if (refChange.getType() == RefChangeType.DELETE) {
                System.out.println("The ref '" + refChange.getRefId() + "' deleted.");
            }
        }
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        if (settings.getString("roomName", "").isEmpty()) {
            errors.addFieldError("roomName", "'Room Name' field is blank, please supply one");
        }
    }
}