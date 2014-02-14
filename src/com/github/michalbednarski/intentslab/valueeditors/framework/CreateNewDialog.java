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
import com.github.michalbednarski.intentslab.CategorizedAdapter;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxedType;
import com.github.michalbednarski.intentslab.valueeditors.ConstructorDialog;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Dialog managing object creators
 */
public class CreateNewDialog extends ValueEditorDialogFragment {

    private static final CreatorBase[] CREATOR_REGISTRY = new CreatorBase[] {
            new ConstructorDialog.ValueCreator()
    };


    /**
     * Common interface for SyncCreator and AsyncCreator
     * must not be implemented directly
     */
    interface CreatorBase {
        int getCategoryNameResource();
    }

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

    public interface SyncCreator extends CreatorBase {
        CreatorOption[] getCreatorOptions(SandboxedType sandboxedType, boolean allowSandbox);
    }

    public interface AsyncCreator extends CreatorBase {
        public interface Callback {
            void creatorOptionsReady(CreatorOption[] options);
            void noCreatorOptionsAvailable();
        }

        void getCreatorOptionsAsync(SandboxedType sandboxedType, boolean allowSandbox, Callback callback);
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
    private int mOptionsToLoadLeft = 1; // Dummy

    static final String ARG_SANDBOXED_TYPE = "CreateNewDialog.sandboxedType";
    static final String ARG_ALLOW_SANDBOX = "CreateNewDialog.allowSandbox";



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        SandboxedType sandboxedType = getArguments().getParcelable(ARG_SANDBOXED_TYPE);
        boolean allowSandbox = getArguments().getBoolean(ARG_ALLOW_SANDBOX);


        for (int i = 0, j = CREATOR_REGISTRY.length; i < j; i++) {
            CreatorBase creator = CREATOR_REGISTRY[i];
            if (creator instanceof SyncCreator) {
                CreatorOption[] options = ((SyncCreator) creator).getCreatorOptions(sandboxedType, allowSandbox);
                if (options != null && options.length != 0) {
                    mOptions[i] = options;
                }
            } else {
                mOptionsToLoadLeft++;
                final int mI = i;
                ((AsyncCreator) creator).getCreatorOptionsAsync(sandboxedType, allowSandbox, new AsyncCreator.Callback() {
                    private boolean mCalled = false;

                    @Override
                    public void creatorOptionsReady(CreatorOption[] options) {
                        assert !mCalled;
                        mCalled = true;

                        if (options != null && options.length != 0) {
                            mOptions[mI] = options;
                        }

                        mOptionsToLoadLeft--;
                        checkAllCreatorsReady();
                   }

                    @Override
                    public void noCreatorOptionsAvailable() {
                        assert !mCalled;
                        mCalled = true;
                        mOptionsToLoadLeft--;
                        checkAllCreatorsReady();
                    }
                });
            }
        }

        // Count dummy for actually-sync AsyncCreators and check if everything is ready
        mOptionsToLoadLeft--;
        checkAllCreatorsReady();
    }

    private void checkAllCreatorsReady() {
        assert mOptionsToLoadLeft >= 0;
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
                        editor.show(getActivity().getSupportFragmentManager(), getTag() + "Redirected");
                        dismiss();
                    }

                    @Override
                    public void returnObject(Object object) {
                        sendResultAndDismiss(object);
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
}
