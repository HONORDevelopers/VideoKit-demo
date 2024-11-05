/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.videokit.npu;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.hihonor.videokit.R;

import java.io.IOException;
import java.io.InputStream;

public class NpuActivity extends Activity {
    private static final String TAG = "NpuActivity";

    private static final int RAW_PICTURE = R.raw.butterfly_lr;

    private Surface mSurface;

    private SurfaceView mSurfaceView;

    private ImageView mIvResult;

    static {
        System.loadLibrary("npu");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_npu);
        initView();
    }

    private void initView() {
        mIvResult = findViewById(R.id.iv_result);
        mSurfaceView = findViewById(R.id.view_surface);
        try (InputStream resources = getResources().openRawResource(RAW_PICTURE)) {
            Bitmap mInputBitmap = BitmapFactory.decodeStream(resources);
            ImageView imageRaw = findViewById(R.id.iv_raw);
            imageRaw.setImageBitmap(mInputBitmap);
            mSurfaceView.getHolder().setFixedSize(mInputBitmap.getWidth(), mInputBitmap.getHeight());
        } catch (IOException e) {
            Log.e(TAG, "get input bitmap error");
        }

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                mSurface = holder.getSurface();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
    }

    public void trans(View view) {
        try (InputStream resources = getResources().openRawResource(RAW_PICTURE)) {
            Bitmap lutBitmap = BitmapFactory.decodeStream(resources);
            Glcontext.createEGLContext(mSurface);
            getPictureTextureId(lutBitmap.getWidth(), lutBitmap.getHeight());
            mSurfaceView.setVisibility(View.GONE);
            mSurfaceView.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.e(TAG, "get lut bitmap error");
        }
    }

    private void getPictureTextureId(int width, int height) {
        // native 调用
        int nativeResult = nativeGetTextureID(width, height, getAssets());
        if (nativeResult != 0) {
            Log.i(TAG, "nativeResult = " + nativeResult);
            return;
        }
        // 显示图片
        covertBuffer();
    }

    private void covertBuffer() {
        // 创建一个Bitmap对象，并将IntBuffer中的数据填充到Bitmap中
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/Download/result0.png";
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        if (bitmap == null) {
            Log.e(TAG, "covertBuffer: decode image fail");
        }
        // 把图片显示出来
        mIvResult.setImageBitmap(bitmap);
        Log.i(TAG, "covertBuffer success");
    }

    private native int nativeGetTextureID(int inWidth, int inHeight, AssetManager assetManager);
}