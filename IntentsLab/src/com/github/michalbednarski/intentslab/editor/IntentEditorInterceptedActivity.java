package com.github.michalbednarski.intentslab.editor;

/**
 * IntentEditorActivity that is used for interception
 *
 * This has following differences in behavior from normal IntentEditorActivity:
 * <ul>
 *     <li> Reads it's intent in it's raw form
 *     <li> Automatically detects componentType=ACTIVITY and method=startActivity(ForResult)
 *     <li> Allows returning result
 * </ul>
 */
public class IntentEditorInterceptedActivity extends IntentEditorActivity {
    @Override
    protected boolean isInterceptedIntent() {
        return true;
    }
}
