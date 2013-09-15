package com.example.testapp1.providerlab.proxy;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PathPermission;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.example.testapp1.providerlab.AdvancedQueryActivity;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mb on 10.09.13.
 */
public class ProxyProvider extends ContentProvider {
    public static final String AUTHORITY = "intentslab.proxyprovider";

    private static HashSet<String> DISALLOWED_AUTHORITIES = new HashSet<String>();
    static {
        DISALLOWED_AUTHORITIES.add(ProxyProvider.AUTHORITY);
        DISALLOWED_AUTHORITIES.add(ProxyProviderForGrantUriPermission.AUTHORITY);
    }

    private enum PermissionEnforcement {
        ENFORCE_READ,
        ENFORCE_WRITE,
        UNPROTECTED
    }

    private static final Pattern PATH_PATTERN = Pattern.compile("^/([^/]*)(/.*)$");

    protected boolean shouldSkipPermissionChecks() {
        return false;
    }

    private Uri unwrapUriAndCheckPermissions(Uri wrappedUri, PermissionEnforcement enforcement) {
        // Unwrap
        final Matcher matcher = PATH_PATTERN.matcher(wrappedUri.getPath());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unable to match uri");
        }
        final String authority = matcher.group(1);
        final String path = matcher.group(2);
        Uri unwrappedUri = Uri.parse("content://" + authority + path);

        // Check blacklist
        if (DISALLOWED_AUTHORITIES.contains(authority)) {
            throw new SecurityException("Not allowed to proxy to " + authority);
        }

        // Check permissions
        final boolean isUnprotected = shouldSkipPermissionChecks() || enforcement == PermissionEnforcement.UNPROTECTED;
        if (!isUnprotected) {
            final boolean enforceWrite = enforcement == PermissionEnforcement.ENFORCE_WRITE;

            // Get provider info
            final ProviderInfo providerInfo = getContext().getPackageManager().resolveContentProvider(wrappedUri.getAuthority(), 0);
            if (providerInfo == null) {
                throw new SecurityException("Unknown wrapped provider");
            }

            // Check also normal permissions
            boolean hasNormalPermissionGranted;
            if (!providerInfo.exported) {
                // Provider not exported, never grant normal permission
                hasNormalPermissionGranted = false;
            } else {
                // Get default provider-global permission
                String permission = enforceWrite ? providerInfo.writePermission : providerInfo.readPermission;

                // Find path permission if there are such permissions defined
                // and there's no provider-global permission set for that, which takes precedence
                if (providerInfo.pathPermissions != null && permission == null) {
                    for (PathPermission pathPermission : providerInfo.pathPermissions) {
                        if (pathPermission.match(unwrappedUri.getPath())) {
                            permission = enforceWrite ? pathPermission.getWritePermission() : pathPermission.getReadPermission();
                            break;
                        }
                    }
                }

                // Check that permission
                if (permission == null) {
                    hasNormalPermissionGranted = true;
                } else {
                    hasNormalPermissionGranted = getContext().checkCallingPermission(permission) == PackageManager.PERMISSION_GRANTED;
                }
            }

            // Check if caller has granted runtime permission
            final int modeFlags = enforceWrite ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : Intent.FLAG_GRANT_READ_URI_PERMISSION;
            final boolean hasRuntimePermissionGranted = getContext().checkCallingUriPermission(unwrappedUri, modeFlags) != PackageManager.PERMISSION_GRANTED;

            // Throw if permission isn't granted
            if (!hasNormalPermissionGranted && !hasRuntimePermissionGranted) {
                throw new SecurityException("Wrapped permission check");
            }
        }

        return unwrappedUri;
    }

    private ProxyProviderDatabase.OperationLogEntryBuilder makeLogEntryBuilder(int method, Uri unwrappedUri) {
        return ProxyProviderDatabase.getInstance(getContext())
                .new OperationLogEntryBuilder(method, unwrappedUri);
    }


    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Uri unwrappedUri = unwrapUriAndCheckPermissions(uri, PermissionEnforcement.ENFORCE_READ);

        // Prepare log entry
        final ProxyProviderDatabase.OperationLogEntryBuilder logBuilder = makeLogEntryBuilder(AdvancedQueryActivity.METHOD_QUERY, unwrappedUri);

        logBuilder
                .setProjection(projection)
                .setSelection(selection)
                .setSelectionArgs(selectionArgs)
                .setSortOrder(sortOrder);

        // Execute
        try {
            final Cursor result = getContext().getContentResolver().query(unwrappedUri, projection, selection, selectionArgs, sortOrder);
            logBuilder.setResult(result == null ? -1 : result.getCount());
            return result;
        } catch (RuntimeException e) {
            logBuilder.setException(e);
            throw e;
        } finally {
            logBuilder.writeToLog();
        }
    }

    @Override
    public String getType(Uri uri) {
        Uri unwrappedUri = unwrapUriAndCheckPermissions(uri, PermissionEnforcement.ENFORCE_READ);

        // Prepare log entry
        final ProxyProviderDatabase.OperationLogEntryBuilder logBuilder = makeLogEntryBuilder(AdvancedQueryActivity.METHOD_GET_TYPE, unwrappedUri);

        // Execute
        try {
            final String result = getContext().getContentResolver().getType(unwrappedUri);
            logBuilder.setResult(result);
            return result;
        } catch (RuntimeException e) {
            logBuilder.setException(e);
            throw e;
        } finally {
            logBuilder.writeToLog();
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri unwrappedUri = unwrapUriAndCheckPermissions(uri, PermissionEnforcement.ENFORCE_WRITE);

        // Prepare log entry
        final ProxyProviderDatabase.OperationLogEntryBuilder logBuilder = makeLogEntryBuilder(AdvancedQueryActivity.METHOD_INSERT, unwrappedUri);

        logBuilder.setValues(values);

        // Execute
        try {
            final Uri result = getContext().getContentResolver().insert(unwrappedUri, values);
            if (result != null) {
                logBuilder.setResult(result.toString());
            }
            return result;
        } catch (RuntimeException e) {
            logBuilder.setException(e);
            throw e;
        } finally {
            logBuilder.writeToLog();
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Uri unwrappedUri = unwrapUriAndCheckPermissions(uri, PermissionEnforcement.ENFORCE_WRITE);

        // Prepare log entry
        final ProxyProviderDatabase.OperationLogEntryBuilder logBuilder = makeLogEntryBuilder(AdvancedQueryActivity.METHOD_DELETE, unwrappedUri);

        logBuilder
                .setSelection(selection)
                .setSelectionArgs(selectionArgs);

        // Execute
        try {
            final int result = getContext().getContentResolver().delete(unwrappedUri, selection, selectionArgs);
            logBuilder.setResult(result);
            return result;
        } catch (RuntimeException e) {
            logBuilder.setException(e);
            throw e;
        } finally {
            logBuilder.writeToLog();
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Uri unwrappedUri = unwrapUriAndCheckPermissions(uri, PermissionEnforcement.ENFORCE_WRITE);

        // Prepare log entry
        final ProxyProviderDatabase.OperationLogEntryBuilder logBuilder = makeLogEntryBuilder(AdvancedQueryActivity.METHOD_UPDATE, unwrappedUri);

        logBuilder.setValues(values);
        logBuilder.setSelection(selection);
        logBuilder.setSelectionArgs(selectionArgs);

        // Execute
        try {
            final int result = getContext().getContentResolver().update(unwrappedUri, values, selection, selectionArgs);
            logBuilder.setResult(result);
            return result;
        } catch (RuntimeException e) {
            logBuilder.setException(e);
            throw e;
        } finally {
            logBuilder.writeToLog();
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Uri unwrappedUri = unwrapUriAndCheckPermissions(
                uri,
                (mode != null && mode.contains("w")) ?
                        PermissionEnforcement.ENFORCE_WRITE :
                        PermissionEnforcement.ENFORCE_READ
        );

        // Prepare log entry
        final ProxyProviderDatabase.OperationLogEntryBuilder logBuilder = makeLogEntryBuilder(AdvancedQueryActivity.METHOD_OPEN_FILE, unwrappedUri);
        logBuilder.setMode(mode);

        // Execute
        try {
            return getContext().getContentResolver().openFileDescriptor(unwrappedUri, mode);
        } catch (RuntimeException e) {
            logBuilder.setException(e);
            throw e;
        } finally {
            logBuilder.writeToLog();
        }
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        Uri unwrappedUri = unwrapUriAndCheckPermissions(
                uri,
                (mode != null && mode.contains("w")) ?
                        PermissionEnforcement.ENFORCE_WRITE :
                        PermissionEnforcement.ENFORCE_READ
        );

        // Prepare log entry
        final ProxyProviderDatabase.OperationLogEntryBuilder logBuilder = makeLogEntryBuilder(AdvancedQueryActivity.METHOD_OPEN_ASSET_FILE, unwrappedUri);
        logBuilder.setMode(mode);

        // Execute
        try {
            return getContext().getContentResolver().openAssetFileDescriptor(unwrappedUri, mode);
        } catch (RuntimeException e) {
            logBuilder.setException(e);
            throw e;
        } finally {
            logBuilder.writeToLog();
        }
    }
}
