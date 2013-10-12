package com.github.michalbednarski.intentslab;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mb on 09.09.13.
 */
public class AssetProvider extends ContentProvider {
    public static final String AUTHORITY = "intentslab.assetsprovider";
    private static final Pattern PATH_PATTERN = Pattern.compile("/([^/]*)/(.*)");
    private static final String PREF_USE_REAL_FILES = "use-real-files-in-open-asset-file";

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
        try {
            return convertAssetFileDescriptorToParcelFileDescriptor(openAssetFile(uri, mode));
        } catch (IOException e) {
            throw new FileNotFoundException("Asset conversion failed");
        }
    }

    private ParcelFileDescriptor convertAssetFileDescriptorToParcelFileDescriptor(AssetFileDescriptor assetFileDescriptor) throws IOException {
        // Generate temp file
        File tmpFile = File.createTempFile("tmp_res_", "", getContext().getCacheDir());

        try {
            // Copy asset to real file
            FileOutputStream outputStream = new FileOutputStream(tmpFile);
            final FileInputStream inputStream = assetFileDescriptor.createInputStream();
            for (;;) {
                byte[] buffer = new byte[2048];
                int len = inputStream.read(buffer);
                if (len == -1) {
                    break;
                }
                outputStream.write(buffer, 0, len);
            }
            outputStream.close();
            inputStream.close();

            // Open file and return in
            return ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_ONLY);

        } finally {
            // Delete temporary file once it's opened
            tmpFile.delete();
        }
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        final Matcher matcher = PATH_PATTERN.matcher(uri.getPath());
        if (!matcher.find()) {
            throw new FileNotFoundException("uri parse error");
        }
        final String packageName = matcher.group(1);
        final String pathInApk = matcher.group(2);
        final Context context = getContext();
        try {
            final AssetFileDescriptor descriptor = context.createPackageContext(packageName, 0).getAssets().openNonAssetFd(pathInApk);
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USE_REAL_FILES, true)) {
                return new AssetFileDescriptor(convertAssetFileDescriptorToParcelFileDescriptor(descriptor), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
            }
            return descriptor;
        } catch (Exception e) {
            throw new FileNotFoundException("Unable to get underlying file");
        }
    }
}
