package com.cisco.stash.plugin.service;

import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.util.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Sagar on 01/06/15.
 */
public class ConcreteSettingsService implements SettingsService {

    private SecurityService securityService;
    private RepositoryHookService repositoryHookService;

    private static final Logger log = LoggerFactory.getLogger(ConcreteSettingsService.class);

    public ConcreteSettingsService(SecurityService securityService, RepositoryHookService repositoryHookService) {
        this.securityService = securityService;
        this.repositoryHookService = repositoryHookService;
    }

    /**
     * get the settings of an hook that is enabled on a particular repository
     * @param repository
     * @param hookKey
     * @return
     */
    @Override
    public Settings getSettings(final Repository repository, final String hookKey) {
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

    /**
     * checks if a hook is enabled on a particular repository or not
     * @param repository
     * @param hookKey
     * @return
     */
    @Override
    public boolean isHookEnabled(final Repository repository, final String hookKey) {
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
}
