/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2016 ZionSoft
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.zionsoft.obadiah.misc.license;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

public class OpenSourceLicenseActivity extends BaseAppCompatActivity implements OpenSourceLicenseView {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, OpenSourceLicenseActivity.class);
    }

    @Inject
    OpenSourceLicensePresenter openSourceLicensePresenter;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.license_list)
    RecyclerView licenseList;

    @BindView(R.id.loading_spinner)
    ProgressBar loadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(OpenSourceLicenseComponentFragment.FRAGMENT_TAG) == null) {
            fm.beginTransaction()
                    .add(OpenSourceLicenseComponentFragment.newInstance(),
                            OpenSourceLicenseComponentFragment.FRAGMENT_TAG)
                    .commit();
        }

        setContentView(R.layout.activity_open_source_license);
        toolbar.setLogo(R.drawable.ic_action_bar);
        toolbar.setTitle(R.string.activity_open_source_license);
        licenseList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof OpenSourceLicenseComponentFragment) {
            ((OpenSourceLicenseComponentFragment) fragment).getComponent().inject(this);

            final View rootView = getWindow().getDecorView();
            final Settings settings = openSourceLicensePresenter.getSettings();
            rootView.setKeepScreenOn(settings.keepScreenOn());
            rootView.setBackgroundColor(settings.getBackgroundColor());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        openSourceLicensePresenter.takeView(this);
        openSourceLicensePresenter.loadLicense();
    }

    @Override
    protected void onStop() {
        openSourceLicensePresenter.dropView();
        super.onStop();
    }

    @Override
    public void onLicensesLoaded(List<String> licenses) {
        licenseList.setAdapter(new OpenSourceLicenseListAdapter(
                this, openSourceLicensePresenter.getSettings(), licenses));
        loadingSpinner.setVisibility(View.GONE);
    }
}
