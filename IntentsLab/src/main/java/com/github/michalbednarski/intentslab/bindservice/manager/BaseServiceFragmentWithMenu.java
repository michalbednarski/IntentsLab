/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
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
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.michalbednarski.intentslab.bindservice.manager;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;
import com.github.michalbednarski.intentslab.bindservice.DumpSysFragment;

/**
 * Created by mb on 19.08.14.
 */
public class BaseServiceFragmentWithMenu extends BaseServiceFragment {
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bound_service, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean canDump = getSystemServiceName() != null; // TODO: hide if denied
        MenuItem item = menu.findItem(R.id.dump);
        item.setVisible(canDump);
        item.setEnabled(canDump);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.dump) {
            startActivity(
                    new Intent(getActivity(), SingleFragmentActivity.class)
                    .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, DumpSysFragment.class.getName())
                    .putExtra(DumpSysFragment.ARG_SERVICE_NAME, getSystemServiceName())
            );
            return true;
        }
        return false;
    }

    private String getSystemServiceName() {
        ServiceDescriptor descriptor = getArguments().getParcelable(ARG_SERVICE_DESCRIPTOR);
        if (descriptor instanceof SystemServiceDescriptor) {
            return ((SystemServiceDescriptor) descriptor).mServiceName;
        }
        return null;
    }
}
