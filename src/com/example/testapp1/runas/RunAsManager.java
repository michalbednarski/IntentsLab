package com.example.testapp1.runas;

import android.content.Context;
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
import com.example.testapp1.R;
import com.example.testapp1.Utils;

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
}
