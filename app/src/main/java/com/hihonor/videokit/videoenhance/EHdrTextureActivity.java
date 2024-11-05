/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.videokit.videoenhance;

import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hihonor.mcs.media.video.HdrLightnessClient;
import com.hihonor.mcs.media.video.HnVideoClient;
import com.hihonor.mcs.media.video.IVideoServiceCallback;
import com.hihonor.mcs.media.video.common.StatusCode;
import com.hihonor.videokit.BaseActivity;
import com.hihonor.videokit.R;

import java.io.IOException;

public class EHdrTextureActivity extends BaseActivity
    implements IVideoServiceCallback, TextureView.SurfaceTextureListener {
    private static final String TAG = "EHdrTextureActivity";

    private final String mVideoPath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/test_HDR.mp4";

    private TextureView mTextureView;

    private MediaPlayer mMediaPlayer;

    private HnVideoClient mHnVideoClient = null;

    private HdrLightnessClient mHdrLightnessClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ehdr_texture);
        initView();
        initData();
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initView() {
        mTextureView = findViewById(R.id.texture_display);
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onResult(int result) {
        Log.i(TAG, "result = " + result);
        switch (result) {
            case StatusCode.VIDEO_SERVICE_SUCCESS: // 初始化video-kit实例成功
                initHnBrightnessClient();
                break;
            case StatusCode.VIDEO_SERVICE_HDR_FEATURE_SUCCESS: // 创建VideoKit亮度监听特性实例成功
                enableHdr(null);
                registerBrightnessListener();
                break;
            case StatusCode.VIDEO_NO_PERMISSION:
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
                break;
            case StatusCode.VIDEO_CONTEXT_NULL:
                // 构造方法context参数为空, APP需要确保context不能为空，自行处理
                break;
            case StatusCode.VIDEO_SERVICE_DISCONNECTED:
                // 服务断开, APP自行处理
                break;
            case StatusCode.VIDEO_INVALID_CALL:
                // 非法无效调用，当client被destroy或者没有进行初始化，会返回此类错误码
            default:
                break;
        }
    }

    /**
     * 初始化化对象
     */
    private void initData() {
        if (mHnVideoClient != null) {
            return;
        }
        mHnVideoClient = new HnVideoClient(getApplicationContext(), this);
        mHnVideoClient.initialize(); // 初始化绑定服务并设置服务状态回调，这里存在一些耗时，所以采用异步方式（耗时在100-200ms之间）
    }

    // 判断当前设备是否支持HDR亮度监听特性，并创建特性客户端
    private void initHnBrightnessClient() {
        Log.i(TAG, "initHnBrightnessClient");
        if (mHnVideoClient != null
            && mHnVideoClient.isServiceSupported(HnVideoClient.ServiceType.HNVIDEO_SERVICE_LIGHTNESS_INFO)) {
            mHdrLightnessClient =
                mHnVideoClient.createService(HnVideoClient.ServiceType.HNVIDEO_SERVICE_LIGHTNESS_INFO);
        } else {
            Log.e(TAG, "init HnBrightnessClient failed");
            Toast.makeText(this, "当前设备不支持 HDR 视频增强能力", Toast.LENGTH_SHORT).show();
        }
    }

    // 注册亮度监听
    private void registerBrightnessListener() {
        if (mHdrLightnessClient != null) {
            mHdrLightnessClient.registerLightnessChangedListener(light -> {
                Log.i(TAG, "Light value = " + light);
            });
        }
    }

    // 注销亮度监听
    private void unregisterBrightnessListener() {
        if (mHdrLightnessClient != null) {
            mHdrLightnessClient.unregisterLightnessChangedListener();
        }
    }

    // 开启HDR模式
    public void enableHdr(View view) {
        if (mHdrLightnessClient != null) {
            // Log.i(TAG, "enableTextureHdrMode");
            // 开启 TextureView 的 HDR 增强能力
            mHdrLightnessClient.enableTextureHdrMode(mTextureView, null);
            if (view == null) {
                Toast.makeText(this, "已自动开启 HDR 增强能力", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "已开启 HDR 增强能力", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 关闭HDR模式
    public void disableHdr(View view) {
        if (mHdrLightnessClient != null) {
            Log.i(TAG, "disableTextureHdrMode");

            // 关闭 TextureView 的 HDR 增强能力
            mHdrLightnessClient.disableTextureHdrMode(mTextureView);
            Toast.makeText(this, "已关闭 HDR 增强能力", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeVideoSize() {
        int videoWidth = mMediaPlayer.getVideoWidth();
        int videoHeight = mMediaPlayer.getVideoHeight();
        int surfaceWidth = mTextureView.getWidth();
        int surfaceHeight = mTextureView.getHeight();

        // 根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
        float max;
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            // 竖屏模式下按视频宽度计算放大倍数值
            max = Math.max((float) videoWidth / (float) surfaceWidth, (float) videoHeight / (float) surfaceHeight);
        } else {
            // 横屏模式下按视频高度计算放大倍数值
            max = Math.max(((float) videoWidth / (float) surfaceHeight), (float) videoHeight / (float) surfaceWidth);
        }

        // 视频宽高分别/最大倍数值 计算出放大后的视频尺寸
        videoWidth = (int) Math.ceil((float) videoWidth / max);
        videoHeight = (int) Math.ceil((float) videoHeight / max);

        // 无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
        mTextureView.setLayoutParams(new RelativeLayout.LayoutParams(videoWidth, videoHeight));
    }

    // 销毁并注销VideoKit
    protected void destroy() {
        if (mHdrLightnessClient != null) {
            mHdrLightnessClient.unregisterLightnessChangedListener();
            mHdrLightnessClient.destroy();
            mHdrLightnessClient = null;
        }
        if (mHnVideoClient != null) {
            mHnVideoClient.destroy();
            mHnVideoClient = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        try {
            Surface ss = new Surface(surface);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(mVideoPath);
            mMediaPlayer.setSurface(ss);
            mMediaPlayer.setOnCompletionListener(mp -> {
                if (mMediaPlayer == null) {
                    return;
                }
                mMediaPlayer.start();
                mMediaPlayer.setLooping(true);
            });
            mMediaPlayer.setOnVideoSizeChangedListener((mp, width1, height1) -> changeVideoSize());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "onSurfaceTextureAvailable: " + e.getMessage());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableHdr(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 注销亮度监听
        unregisterBrightnessListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }

}