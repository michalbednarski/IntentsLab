package com.example.testapp1;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mb on 09.09.13.
 */
public class AssetProvider extends ContentProvider {
    public static final String AUTHORITY = "intentslab.assetsprovider";
    private static final Pattern PATH_PATTERN = Pattern.compile("/([^/]*)/(.*)");

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return openAssetFile(uri, mode).getParcelFileDescriptor();
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        final Matcher matcher = PATH_PATTERN.matcher(uri.getPath());
        if (!matcher.find()) {
            throw new FileNotFoundException("uri parse error");
        }
        final String packageName = matcher.group(1);
        final String pathInApk = matcher.group(2);
        try {
            return getContext().createPackageContext(packageName, 0).getAssets().openNonAssetFd(pathInApk);
        } catch (Exception e) {
            throw new FileNotFoundException("Unable to get underlying file");
        }
    }
}
