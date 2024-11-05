/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.videokit.videoenhance;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.hihonor.videokit.BaseActivity;
import com.hihonor.videokit.R;

public class EHdrEntranceActivity extends BaseActivity {
    private static final String VIDEO_PATH = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/test_HDR.mp4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ehdr_entrance);
    }

    public void enableSurfaceView(View view) {
        startActivity(EHdrSurfaceActivity.class);
    }

    public void enableTextureView(View view) {
        startActivity(EHdrTextureActivity.class);
    }

    private void startActivity(Class<?> cls) {
        File file = new File(VIDEO_PATH);
        if (!file.exists()) {
            Toast.makeText(mContext, "请先push test_HDR.mp4文件到 /sdcard/DCIM/Camera/目录下", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, cls);
        startActivity(intent);
    }
}
