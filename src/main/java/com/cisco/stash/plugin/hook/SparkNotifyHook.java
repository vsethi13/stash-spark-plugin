package com.cisco.stash.plugin.hook;

import com.atlassian.event.api.EventListener;
import com.atlassian.stash.commit.CommitService;
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;
import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.setting.*;
import com.atlassian.stash.user.StashAuthenticationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class SparkNotifyHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private final CommitService commitService;
    private final StashAuthenticationContext stashAuthenticationContext;
    private final NavBuilder navBuilder;

    private static final Logger log = LoggerFactory.getLogger(SparkNotifyHook.class);

    public SparkNotifyHook(StashAuthenticationContext stashAuthenticationContext, CommitService commitService, NavBuilder navBuilder) {
        this.commitService = commitService;
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.navBuilder = navBuilder;
    }

    /**
     * Connects to a configured URL to notify of all changes.
     */
    @Override
    public void postReceive(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges) {

        Notifier notifier = new Notifier(stashAuthenticationContext, commitService, navBuilder);
        notifier.publishNotification(notifier.createNotification(repositoryHookContext, refChanges));
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        if (settings.getString("roomName", "").isEmpty()) {
            errors.addFieldError("roomName", "'Room Name' field is blank, please supply one");
        }
    }
}