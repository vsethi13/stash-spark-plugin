package com.cisco.bitbucket.plugin.service;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;

/**
 * Created by Sagar on 01/06/15.
 */
public interface SettingsService {

    Settings getSettings(final Repository repository, final String hookKey);

    boolean isHookEnabled(final Repository repository, final String hookKey);

}
