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

package com.github.michalbednarski.intentslab.valueeditors.framework;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.BuildConfig;
import com.github.michalbednarski.intentslab.CategorizedAdapter;
import com.github.michalbednarski.intentslab.bindservice.callback.CreateCallbackDialog;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxedType;
import com.github.michalbednarski.intentslab.valueeditors.ConstructorDialog;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Dialog managing object creators
 */
public class CreateNewDialog extends ValueEditorDialogFragment {

    private static final ValueCreator[] CREATOR_REGISTRY = new ValueCreator[] {
            new ConstructorDialog.ValueCreator(),
            new CreateCallbackDialog.ValueCreator()
    };


    /**
     * Option in creator chooser
     */
    public abstract static class CreatorOption {
        final String mOptionText;

        public CreatorOption(String optionText) {
            mOptionText = optionText;
        }

        public abstract void onOptionSelected(EditorRedirect redirect);
    }

    public interface ValueCreator {
        CreatorOption[] getCreatorOptions(SandboxedType sandboxedType, boolean allowSandbox, Callback callback);

        int getCategoryNameResource();

        public interface Callback {
            void goAsync();
            void creatorOptionsReady(CreatorOption[] options);
            void noCreatorOptionsAvailable();
        }
    }

    /**
     * Callback interface for {@link CreatorOption#onOptionSelected(EditorRedirect)}
     */
    public interface EditorRedirect {
        Context getContext();
        void runEditorInActivity(Intent intent, Context context);
        void runEditorInDialogFragment(ValueEditorDialogFragment editor, Bundle args);
        void returnObject(Object object);
        void returnSandboxedObject(SandboxedObject object);
    }


    /*
     *
     *
     * End registry stuff
     *
     * Begin dialog stuff
     *
     *
     */


    private ListView mListView;
    private CreatorOption[][] mOptions = new CreatorOption[CREATOR_REGISTRY.length][];
    private int mOptionsToLoadLeft = CREATOR_REGISTRY.length;

    static final String ARG_SANDBOXED_TYPE = "CreateNewDialog.sandboxedType";
    static final String ARG_ALLOW_SANDBOX = "CreateNewDialog.allowSandbox";



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        SandboxedType sandboxedType = getArguments().getParcelable(ARG_SANDBOXED_TYPE);
        boolean allowSandbox = getArguments().getBoolean(ARG_ALLOW_SANDBOX);

        // Query creators
        for (int i = 0, j = CREATOR_REGISTRY.length; i < j; i++) {
            ValueCreator creator = CREATOR_REGISTRY[i];
            CreatorCallbackImpl callback = new CreatorCallbackImpl(i);
            CreatorOption[] options = creator.getCreatorOptions(sandboxedType, allowSandbox, callback);

            if (callback.mState == CreatorCallbackState.SYNCHRONOUS_MODE) {
                // Synchronous mode
                callback.mState = CreatorCallbackState.FINISHED;
                if (options != null && options.length != 0) {
                    mOptions[i] = options;
                }
                mOptionsToLoadLeft--;
            } else {
                // Asynchronous mode
                if (BuildConfig.DEBUG && options != null) {
                    throw new AssertionError("Creator " + creator.getClass().getName() + " returned non-null value after goAsync()");
                }
            }
        }

        // Check if no creators went async
        checkAllCreatorsReady();
    }

    private void checkAllCreatorsReady() {
        if (BuildConfig.DEBUG && mOptionsToLoadLeft < 0) {
            throw new AssertionError("mOptionsToLoadLeft < 0");
        }
        if (mOptionsToLoadLeft == 0 && mListView != null) {
            mListView.setAdapter(new Adapter());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListView = new ListView(getActivity());
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CreatorOption option = (CreatorOption) mListView.getItemAtPosition(position);
                option.onOptionSelected(new EditorRedirect() {

                    @Override
                    public Context getContext() {
                        return getActivity();
                    }

                    @Override
                    public void runEditorInActivity(Intent intent, Context context) {
                        throw new RuntimeException("Not yet implemented");
                    }

                    @Override
                    public void runEditorInDialogFragment(ValueEditorDialogFragment editor, Bundle args) {
                        FragmentManager fragmentManager = getFragmentManager();
                        if (args == null) {
                            args = new Bundle();
                        }
                        Bundle chooserArguments = getArguments();
                        args.putString(EXTRA_KEY, chooserArguments.getString(EXTRA_KEY));
                        args.putString(Editor.EXTRA_TITLE, chooserArguments.getString(Editor.EXTRA_TITLE));
                        editor.setArguments(args);
                        editor.setTargetFragment(getTargetFragment(), 0);
                        editor.show(getActivity().getSupportFragmentManager(), getTag() + "Redirected");
                        dismiss();
                    }

                    @Override
                    public void returnObject(Object object) {
                        sendResult(object);
                        dismiss();
                    }

                    @Override
                    public void returnSandboxedObject(SandboxedObject object) {
                        throw new RuntimeException("Not yet implemented");
                    }
                });
            }
        });
        checkAllCreatorsReady();
        return mListView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView = null;
    }

    private class Adapter extends CategorizedAdapter {

        private final CreatorOption[] mUncategorizedOptions;

        Adapter() {
            ArrayList<CreatorOption> uncategorizedOptions = new ArrayList<CreatorOption>();
            for (int i = 0, mOptionsLength = mOptions.length; i < mOptionsLength; i++) {
                CreatorOption[] options = mOptions[i];
                if (options != null && CREATOR_REGISTRY[i].getCategoryNameResource() == 0) {
                    uncategorizedOptions.addAll(Arrays.asList(options));
                }
            }
            mUncategorizedOptions = uncategorizedOptions.toArray(new CreatorOption[uncategorizedOptions.size()]);
        }

        @Override
        protected CreatorOption getItemInCategory(int category, int positionInCategory) {
            if (category == -1) {
                return mUncategorizedOptions[positionInCategory];
            }
            return mOptions[category][positionInCategory];
        }

        @Override
        protected int getCategoryCount() {
            return CREATOR_REGISTRY.length;
        }

        @Override
        protected int getCountInCategory(int category) {
            if (category == -1) {
                return mUncategorizedOptions.length; // Uncategorized
            }
            if (mOptions[category] == null || CREATOR_REGISTRY[category].getCategoryNameResource() == 0) {
                return 0; // Don't show category, global or empty
            }
            return mOptions[category].length;
        }

        @Override
        protected String getCategoryName(int category) {
            return getString(CREATOR_REGISTRY[category].getCategoryNameResource());
        }

        @Override
        protected int getViewTypeInCategory(int category, int positionInCategory) {
            return 1;
        }

        @Override
        protected View getViewInCategory(int category, int positionInCategory, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            ((TextView) convertView).setText(getItemInCategory(category, positionInCategory).mOptionText);
            return convertView;
        }

        @Override
        protected boolean isItemInCategoryEnabled(int category, int positionInCategory) {
            return true;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }
    }

    private enum CreatorCallbackState {
        SYNCHRONOUS_MODE,
        WENT_ASYNC,
        FINISHED
    }

    private class CreatorCallbackImpl implements ValueCreator.Callback {

        private final int mCreatorIndex;
        private CreatorCallbackState mState = CreatorCallbackState.SYNCHRONOUS_MODE;

        public CreatorCallbackImpl(int creatorIndex) {
            mCreatorIndex = creatorIndex;
        }

        @Override
        public void goAsync() {
            if (BuildConfig.DEBUG && mState != CreatorCallbackState.SYNCHRONOUS_MODE) {
                throw new AssertionError("Cannot goAsync() when state=" + mState);
            }
            mState = CreatorCallbackState.WENT_ASYNC;
        }

        @Override
        public void creatorOptionsReady(CreatorOption[] options) {
            if (BuildConfig.DEBUG && mState != CreatorCallbackState.SYNCHRONOUS_MODE) {
                throw new AssertionError("Cannot creatorOptionsReady() when state=" + mState);
            }
            mState = CreatorCallbackState.FINISHED;

            if (options != null && options.length != 0) {
                mOptions[mCreatorIndex] = options;
            }

            mOptionsToLoadLeft--;
            checkAllCreatorsReady();
        }

        @Override
        public void noCreatorOptionsAvailable() {
            creatorOptionsReady(null);
        }
    }
}
