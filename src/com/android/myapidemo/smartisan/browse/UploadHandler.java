/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.myapidemo.smartisan.browse;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.service.wallpaper.WallpaperService.Engine;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ValueCallback;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.net.URLEncoder;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserConfig;

import android.net.Uri;


/**
 * Handle the file upload callbacks from WebView here
 */
public class UploadHandler {

    private static final String TAG = "UploadHandler";
    /*
     * The Object used to inform the WebView of the file to upload.
     */
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<String[]> mUploadFilePaths;
    private String mCameraFilePath;

    private boolean mHandled;
    private boolean mCaughtActivityNotFoundException;

    private Controller mController;

    public UploadHandler(Controller controller) {
        mController = controller;
    }

    String getFilePath() {
        return mCameraFilePath;
    }

    protected boolean handled() {
        return mHandled;
    }

    protected void setHandled(boolean handled) {
        mHandled = handled;
        mCaughtActivityNotFoundException = false;
        // If  upload dialog shown to the user got dismissed
        if (!mHandled) {
            mUploadFilePaths.onReceiveValue(null);
        }
        mUploadFilePaths = null;
    }

    void onResult(int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_CANCELED && mCaughtActivityNotFoundException) {
            // Couldn't resolve an activity, we are going to try again so skip
            // this result.
            mCaughtActivityNotFoundException = false;
            return;
        }

        Uri result = intent == null || resultCode != Activity.RESULT_OK ? null
                : intent.getData();

        // As we ask the camera to save the result of the user taking
        // a picture, the camera application does not return anything other
        // than RESULT_OK. So we need to check whether the file we expected
        // was written to disk in the in the case that we
        // did not get an intent returned but did get a RESULT_OK. If it was,
        // we assume that this result has came back from the camera.
        if (result == null && intent == null && resultCode == Activity.RESULT_OK) {
            File cameraFile = new File(mCameraFilePath);
            if (cameraFile.exists()) {
                result = Uri.fromFile(cameraFile);
                // Broadcast to the media scanner that we have a new photo
                // so it will be added into the gallery for the user.
                mController.getActivity().sendBroadcast(
                        new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
            }
        }

        boolean hasGoodFilePath = false;
        String filePath = null;
        if (result != null) {
            String scheme = result.getScheme();
            // try to get local file path from uri
            if ("file".equals(scheme)) {
                filePath = result.getPath();
                hasGoodFilePath = filePath != null && !filePath.isEmpty();
            } else if ("content".equals(scheme)) {
                // simplified logic since engine already has helper method
                // to fetch content URI file name
                filePath = null;//Engine.getContentUriDisplayName(result.toString());;
                hasGoodFilePath = filePath != null && !filePath.isEmpty();
            }
        }

        // Add for carrier feature - prevent uploading DRM type files based on file extension. This
        // is not a secure implementation since malicious users can trivially modify the filename.
        // DRM files can be securely detected by inspecting their integrity protected content.
        boolean drmUploadEnabled = false;//BrowserConfig.getInstance(mController.getContext())
                //.hasFeature(BrowserConfig.Feature.DRM_UPLOADS);
        boolean isDRMFileType = false;
        if (drmUploadEnabled && filePath != null
                && (filePath.endsWith(".fl") || filePath.endsWith(".dm")
                || filePath.endsWith(".dcf") || filePath.endsWith(".dr")
                || filePath.endsWith(".dd"))) {
            isDRMFileType = true;
            Toast.makeText(mController.getContext(), R.string.drm_file_unsupported,
                    Toast.LENGTH_LONG).show();
        }

        if (mUploadMessage != null) {
            if (!isDRMFileType) {
                mUploadMessage.onReceiveValue(result);
            } else {
                mUploadMessage.onReceiveValue(null);
            }
        }

        if (mUploadFilePaths != null) {
            if (hasGoodFilePath && !isDRMFileType) {
                mUploadFilePaths.onReceiveValue( new String[]{result.toString()});
            } else {
                mUploadFilePaths.onReceiveValue(null);
            }
        }

        //reset everything
        setHandled(true);
    }

    void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        final String imageMimeType = "image/*";
        final String videoMimeType = "video/*";
        final String audioMimeType = "audio/*";
        final String mediaSourceKey = "capture";
        final String mediaSourceValueCamera = "camera";
        final String mediaSourceValueFileSystem = "filesystem";
        final String mediaSourceValueCamcorder = "camcorder";
        final String mediaSourceValueMicrophone = "microphone";

        // According to the spec, media source can be 'filesystem' or 'camera' or 'camcorder'
        // or 'microphone' and the default value should be 'filesystem'.
        String mediaSource = mediaSourceValueFileSystem;

        if (mUploadMessage != null) {
            // Already a file picker operation in progress.
            return;
        }

        mUploadMessage = uploadMsg;

        // Parse the accept type.
        String params[] = acceptType.split(";");
        String mimeType = params[0];

        if (capture.length() > 0) {
            mediaSource = capture;
        }

        if (capture.equals(mediaSourceValueFileSystem)) {
            // To maintain backwards compatibility with the previous implementation
            // of the media capture API, if the value of the 'capture' attribute is
            // "filesystem", we should examine the accept-type for a MIME type that
            // may specify a different capture value.
            for (String p : params) {
                String[] keyValue = p.split("=");
                if (keyValue.length == 2) {
                    // Process key=value parameters.
                    if (mediaSourceKey.equals(keyValue[0])) {
                        mediaSource = keyValue[1];
                    }
                }
            }
        }

        //Ensure it is not still set from a previous upload.
        mCameraFilePath = null;

        if (mimeType.equals(imageMimeType)) {
            if (mediaSource.equals(mediaSourceValueCamera)) {
                // Specified 'image/*' and requested the camera, so go ahead and launch the
                // camera directly.
                startActivity(createCameraIntent());
                return;
            } else {
                // Specified just 'image/*', capture=filesystem, or an invalid capture parameter.
                // In all these cases we show a traditional picker filetered on accept type
                // so launch an intent for both the Camera and image/* OPENABLE.
                Intent chooser = createChooserIntent();
                chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(imageMimeType));
                startActivity(chooser);
                return;
            }
        } else if (mimeType.equals(videoMimeType)) {
            if (mediaSource.equals(mediaSourceValueCamcorder)) {
                // Specified 'video/*' and requested the camcorder, so go ahead and launch the
                // camcorder directly.
                startActivity(createCamcorderIntent());
                return;
           } else {
                // Specified just 'video/*', capture=filesystem or an invalid capture parameter.
                // In all these cases we show an intent for the traditional file picker, filtered
                // on accept type so launch an intent for both camcorder and video/* OPENABLE.
                Intent chooser = createChooserIntent();
                chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(videoMimeType));
                startActivity(chooser);
                return;
            }
        } else if (mimeType.equals(audioMimeType)) {
            if (mediaSource.equals(mediaSourceValueMicrophone)) {
                // Specified 'audio/*' and requested microphone, so go ahead and launch the sound
                // recorder.
                startActivity(createSoundRecorderIntent());
                return;
            } else {
                // Specified just 'audio/*',  capture=filesystem of an invalid capture parameter.
                // In all these cases so go ahead and launch an intent for both the sound
                // recorder and audio/* OPENABLE.
                Intent chooser = createChooserIntent(createSoundRecorderIntent());
                chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(audioMimeType));
                startActivity(chooser);
                return;
            }
        }

        // No special handling based on the accept type was necessary, so trigger the default
        // file upload chooser.
        startActivity(createDefaultOpenableIntent());
    }

    void showFileChooser(ValueCallback<String[]> uploadFilePaths, String acceptTypes,
            boolean capture) {

        final String imageMimeType = "image/";
        final String videoMimeType = "video/";
        final String audioMimeType = "audio/";
        if (mUploadFilePaths != null) {
            // Already a file picker operation in progress.
            return;
        }

        mUploadFilePaths = uploadFilePaths;

        // Parse the accept type.
        String params[] = acceptTypes.split(";");
        String mimeType = params[0];
        if(mimeType == null){
            return;
        }
        //Ensure it is not still set from a previous upload.
        mCameraFilePath = null;
        if (mimeType.startsWith(imageMimeType)) {
            if (capture) {
                // Specified 'image/*' and requested the camera, so go ahead and launch the
                // camera directly.
                startActivity(createCameraIntent());
                return;
            } else {
                // Specified just 'image/*', capture=filesystem, or an invalid capture parameter.
                // In all these cases we show a traditional picker filetered on accept type
                // so launch an intent for both the Camera and image/* OPENABLE.
                Intent chooser = createChooserIntent(createCameraIntent());
                chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(imageMimeType));
                startActivity(chooser);
                return;
            }
        } else if (mimeType.startsWith(videoMimeType)) {
            if (capture) {
                // Specified 'video/*' and requested the camcorder, so go ahead and launch the
                // camcorder directly.
                startActivity(createCamcorderIntent());
                return;
           } else {
                // Specified just 'video/*', capture=filesystem or an invalid capture parameter.
                // In all these cases we show an intent for the traditional file picker, filtered
                // on accept type so launch an intent for both camcorder and video/* OPENABLE.
                Intent chooser = createChooserIntent(createCamcorderIntent());
                chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(videoMimeType));
                startActivity(chooser);
                return;
            }
        } else if (mimeType.startsWith(audioMimeType)) {
            if (capture) {
                // Specified 'audio/*' and requested microphone, so go ahead and launch the sound
                // recorder.
                startActivity(createSoundRecorderIntent());
                return;
            } else {
                // Specified just 'audio/*',  capture=filesystem of an invalid capture parameter.
                // In all these cases so go ahead and launch an intent for both the sound
                // recorder and audio/* OPENABLE.
                Intent chooser = createChooserIntent(createSoundRecorderIntent());
                chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(audioMimeType));
                startActivity(chooser);
                return;
            }
        }

        // No special handling based on the accept type was necessary, so trigger the default
        // file upload chooser.
        startActivity(createDefaultOpenableIntent());
    }

    private void startActivity(Intent intent) {
        try {
            mController.getActivity().startActivityForResult(intent, Controller.FILE_SELECTED);
        } catch (ActivityNotFoundException e) {
            // No installed app was able to handle the intent that
            // we sent, so fallback to the default file upload control.
            try {
                mCaughtActivityNotFoundException = true;
                mController.getActivity().startActivityForResult(createDefaultOpenableIntent(),
                        Controller.FILE_SELECTED);
            } catch (ActivityNotFoundException e2) {
                // Nothing can return us a file, so file upload is effectively disabled.
                Toast.makeText(mController.getActivity(), R.string.uploads_disabled,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private Intent createDefaultOpenableIntent() {
        // Create and return a chooser with the default OPENABLE
        // actions including the camera, camcorder and sound
        // recorder where available.
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");

        Intent chooser = createChooserIntent(createCameraIntent(), createCamcorderIntent(),
                createSoundRecorderIntent());
        chooser.putExtra(Intent.EXTRA_INTENT, i);
        return chooser;
    }

    public void initiateActivity(Intent intent) {
        startActivity(intent);
    }

    private Intent createChooserIntent(Intent... intents) {
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
        chooser.putExtra(Intent.EXTRA_TITLE,
                mController.getActivity().getResources()
                        .getString(R.string.choose_upload));
        return chooser;
    }

    private Intent createOpenableIntent(String type) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(type);
        return i;
    }

    private Intent createCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File externalDataDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM);
        File cameraDataDir = new File(externalDataDir.getAbsolutePath() +
                File.separator + "browser-photos");
        cameraDataDir.mkdirs();
        mCameraFilePath = cameraDataDir.getAbsolutePath() + File.separator +
                System.currentTimeMillis() + ".jpg";
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mCameraFilePath)));
        return cameraIntent;
    }

    private Intent createCamcorderIntent() {
        return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    }

    private Intent createSoundRecorderIntent() {
        return new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
    }

}
