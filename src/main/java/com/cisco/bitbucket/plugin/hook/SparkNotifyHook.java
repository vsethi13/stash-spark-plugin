package com.cisco.bitbucket.plugin.hook;

import com.atlassian.bitbucket.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.RepositorySettingsValidator;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.cisco.bitbucket.plugin.Notifier;
import com.cisco.bitbucket.plugin.event.RefChangeEvent;
import com.cisco.bitbucket.plugin.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class SparkNotifyHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private SettingsService settingsService;
    private RefChangeEvent refChangeEvent;

    private static final Logger log = LoggerFactory.getLogger(SparkNotifyHook.class);

    public SparkNotifyHook(SettingsService settingsService, RefChangeEvent refChangeEvent) {
        this.settingsService = settingsService;
        this.refChangeEvent = refChangeEvent;
    }

    /**
     * validates configuration settings of a hook
     *
     * @param settings
     * @param errors
     * @param repository
     */
    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        if (settings.getString(Notifier.SPACE_ID, "").isEmpty()) {
            errors.addFieldError(Notifier.SPACE_ID, "'Space ID' field is blank, please supply one");
        }
    }

    /**
     * Connects to a configured URL to notify of all changes.
     *
     * @param repositoryHookContext
     * @param refChanges
     */
    @Override
    public void postReceive(RepositoryHookContext repositoryHookContext, Collection<RefChange> refChanges) {

        Settings repoSettings = settingsService.getSettings(repositoryHookContext.getRepository(), Notifier.REPO_HOOK_KEY);
        if (repoSettings != null) {
            StringBuilder message = refChangeEvent.createRefChangeNotification(repositoryHookContext, refChanges);
            if (message != null) {
                new Notifier().pushNotification(repoSettings.getString(Notifier.SPACE_ID, ""), message);
            }
        }
    }
}