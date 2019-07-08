package com.dmp.project.kamatis2;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.LauncherActivity;
import android.content.Intent;
import android.os.Bundle;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import timber.log.Timber;

public class Launcher extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        askPermission();
    }

    private void askPermission() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */

                if (report.areAllPermissionsGranted()) {
                    Timber.d("permission granted.");
                    startActivity(new Intent(Launcher.this, Version2TestActivity.class));
                    finish();
                } else {
                    Timber.d("else");
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                Timber.d("error?");
                token.continuePermissionRequest();

                /* ... */
            }
        })
                .onSameThread()
                .check();
    }

}

