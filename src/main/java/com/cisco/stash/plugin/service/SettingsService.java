package com.cisco.stash.plugin.service;

import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;

/**
 * Created by Sagar on 01/06/15.
 */
public interface SettingsService {

    public Settings getSettings(final Repository repository, final String hookKey);

    public boolean isHookEnabled(final Repository repository, final String hookKey);

}