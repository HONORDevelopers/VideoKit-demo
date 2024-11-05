/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.videokit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.hihonor.videokit.hdrtranscode.Hdr2SdrActivity;
import com.hihonor.videokit.npu.NpuActivity;
import com.hihonor.videokit.sdrplus.SdrPLusActivity;
import com.hihonor.videokit.sharpen.SharpenActivity;
import com.hihonor.videokit.videoenhance.EHdrEntranceActivity;

public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
    }

    // 视频增强
    public void startVideoEnhance(View view) {
        startActivity(EHdrEntranceActivity.class);
    }

    // 超分 锐化
    public void startSharpenActivity(View view) {
        startActivity(SharpenActivity.class);
    }

    public void startNpuActivity(View view) {
        startActivity(NpuActivity.class);
    }

    public void startSdrPlusActivity(View view) {
        startActivity(SdrPLusActivity.class);
    }

    public void startSdr2HdrActivity(View view) {
        startActivity(Hdr2SdrActivity.class);
    }

    private void startActivity(Class<?> cls) {
        Intent intent = new Intent(MainActivity.this, cls);
        startActivity(intent);
    }

    private void checkPermission() {
        boolean isGranted =
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            isGranted = false;
        }
        Log.i("读写权限获取", " ： " + isGranted);
        if (!isGranted) {
            requestPermissions(
                new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                102);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}