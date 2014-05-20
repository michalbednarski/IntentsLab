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

package com.github.michalbednarski.intentslab.runas;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;

/**
 * Class managing running RunAs remote processes
 */
public class RunAsManager {
    private static final SparseArray<IRemoteInterface> sRemoteInterfaces = new SparseArray<IRemoteInterface>();
    private static Handler sHandler = null;
    private static RunAsSelectorAdapter sSelectorAdapter = null;
    private static final int PREVENT_ANDROID_R_IMPORT = R.id.action;

    private static final long ID_SELF = -0x100000000L;

    static long sSelectedId = ID_SELF;

    static boolean registerRemote(final int uid, IRemoteInterface remoteInterface) {
        synchronized (sRemoteInterfaces) {
            // Ensure interface is not already registered
            final IRemoteInterface oldRemoteInterface = sRemoteInterfaces.get(uid);
            if (oldRemoteInterface != null && oldRemoteInterface.asBinder().isBinderAlive()) {
                return false;
            }

            // Register death recipient
            try {
                remoteInterface.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        synchronized (sRemoteInterfaces) {
                            // Remove from list
                            sRemoteInterfaces.remove(uid);

                            // Update list if exists
                            if (sHandler != null) {
                                sHandler.sendEmptyMessage(0);
                            }
                        }
                    }
                }, 0);
            } catch (RemoteException e) {
                return false;
            }

            // Register interface
            sRemoteInterfaces.put(uid, remoteInterface);

            // Update list if exists
            if (sHandler != null) {
                sHandler.sendEmptyMessage(0);
            }
        }

        return true;
    }

    static BaseAdapter getSelectorAdapter() {
        if (sSelectorAdapter == null) {
            sSelectorAdapter = new RunAsSelectorAdapter();
            sHandler = new Handler() {
                @Override
                public void dispatchMessage(Message msg) {
                    sSelectorAdapter.notifyDataSetChanged();
                }
            };
        }
        return sSelectorAdapter;
    }

    static class RunAsSelectorAdapter extends BaseAdapter {

        // Disallow constructing outside RunAsManager
        private RunAsSelectorAdapter() {}

        @Override
        public int getCount() {
            return sRemoteInterfaces.size() + 1;
        }

        @Override
        public IRemoteInterface getItem(int position) {
            if (position == 0) {
                return null;
            }
            return sRemoteInterfaces.valueAt(position - 1);
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) {
                return ID_SELF;
            }
            return sRemoteInterfaces.keyAt(position - 1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get context
            Context context = parent.getContext();

            // Create view if needed
            if (convertView == null) {
                final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            }

            // Get id and text
            long itemId = getItemId(position);
            String text;
            if (itemId == ID_SELF) {
                text = "Self";
            } else {
                int uid = (int) itemId;
                text = context.getPackageManager().getNameForUid(uid);
                if (Utils.stringEmptyOrNull(text)) {
                    text = "uid=" + uid;
                }
            }

            // Put text
            ((TextView) convertView).setText(text);

            return convertView;
        }
    }

    static int getSelectedSpinnerPosition() {
        if (sSelectedId == ID_SELF) {
            return 0;
        }
        final int indexOfUid = sRemoteInterfaces.indexOfKey((int) sSelectedId);
        if (indexOfUid < 0) {
            return 0;
        }
        return indexOfUid + 1;
    }

    public static IRemoteInterface getSelectedRemoteInterface() {
        if (sSelectedId == ID_SELF) {
            return null;
        }
        return sRemoteInterfaces.get((int) sSelectedId);
    }

    public static IRemoteInterface getRemoteInterfaceForSystemDebuggingCommands() {
        // Try to get remote interface running as shell (adb)
        IRemoteInterface shellRemoteInterface = sRemoteInterfaces.get(2000);
        if (shellRemoteInterface != null && shellRemoteInterface.asBinder().isBinderAlive()) {
            return shellRemoteInterface;
        }

        // Try to get remote interface running as root
        IRemoteInterface rootRemoteInterface = sRemoteInterfaces.get(0);
        if (rootRemoteInterface != null && rootRemoteInterface.asBinder().isBinderAlive()) {
            return rootRemoteInterface;
        }

        // Neither exists nor is alive
        return null;
    }

    public static IRemoteInterface getRemoteInterfaceHavingPermission(Context context, String permission) {
        // Try to get remote interface running as root
        IRemoteInterface remoteInterface = sRemoteInterfaces.get(0);
        if (remoteInterface != null && remoteInterface.asBinder().isBinderAlive()) {
            return remoteInterface;
        }

        // Try to get remote interface running as shell (adb)
        remoteInterface = getRemoteInterfaceForUidIfItHasPermission(context, 2000, permission);
        if (remoteInterface != null) {
            return remoteInterface;
        }

        // Try all other interfaces
        for (int i = 0, j = sRemoteInterfaces.size(); i < j; i++) {
            int uid = sRemoteInterfaces.keyAt(i);
            if (uid == 2000) {
                continue; // Skip adb shell, already tested
            }
            remoteInterface = getRemoteInterfaceForUidIfItHasPermission(context, uid, permission);
            if (remoteInterface != null) {
                return remoteInterface;
            }
        }

        // Neither exists nor is alive
        return null;
    }

    private static IRemoteInterface getRemoteInterfaceForUidIfItHasPermission(Context context, int uid, String permission) {
        IRemoteInterface remoteInterface = sRemoteInterfaces.get(uid);
        if (remoteInterface != null && remoteInterface.asBinder().isBinderAlive()
                && context.checkPermission(permission, 0, uid) == PackageManager.PERMISSION_GRANTED) {
            return remoteInterface;
        }
        return null;
    }
}
