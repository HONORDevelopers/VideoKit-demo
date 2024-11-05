/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.videokit.hdrtranscode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hihonor.videokit.BaseActivity;
import com.hihonor.videokit.R;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Hdr2SdrActivity extends BaseActivity implements SurfaceHolder.Callback {
    public static final String LOG_TAG = "Hdr2SdrActivity";

    private final String mFilePath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/test_HDR.mp4";

    // 0:configured 1:flushed 2:running 3:EOS 4uninitialized 4:stopped 5:release 6:error
    private int mCodecStat;

    private MediaExtractor mHdrExtractor;

    private MediaExtractor mSdrExtractor;

    private MediaFormat mHdrMediaFormat;

    private MediaFormat mSdrMediaFormat;

    private String mMimeType;

    private MediaCodec mHdrMediaCodec;

    private MediaCodec mSdrMediaCodec;

    private SurfaceView mHdrSurfaceView;

    private SurfaceView mSdrSurfaceView;

    private boolean mIsPlayingHdrVideo = false;

    private boolean mIsPlayingSdrVideo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hdr_2_sdr);
        initView();

        mHdrSurfaceView.getHolder().addCallback(this);
        mSdrSurfaceView.getHolder().addCallback(this);
    }

    public void playHdrVideo(View view) {
        try {
            File file = new File(mFilePath);
            if (!file.exists()) {
                Toast.makeText(mContext, "请先push test_HDR.mp4文件到 /sdcard/DCIM/Camera/目录下", Toast.LENGTH_SHORT).show();
                return;
            }
            playHdrVideo(mHdrSurfaceView.getHolder().getSurface());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void playSdrVideo(View view) {
        try {
            File file = new File(mFilePath);
            if (!file.exists()) {
                Toast.makeText(mContext, "请先push test_HDR.mp4文件到 /sdcard/DCIM/Camera/目录下", Toast.LENGTH_SHORT).show();
                return;
            }
            playSdrVideo(mSdrSurfaceView.getHolder().getSurface());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mHdrMediaCodec != null && mCodecStat == 2) {
            mHdrMediaCodec.stop();
            mHdrMediaCodec.release();
            mHdrMediaCodec = null;
        }
        if (mSdrMediaCodec != null && mCodecStat == 2) {
            mSdrMediaCodec.stop();
            mSdrMediaCodec.release();
            mSdrMediaCodec = null;
        }
        if (mSdrExtractor != null) {
            mSdrExtractor.release();
        }
        if (mHdrExtractor != null) {
            mHdrExtractor.release();
        }
        mCodecStat = 5;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void initView() {
        mHdrSurfaceView = findViewById(R.id.surfaceview_hdr);
        mSdrSurfaceView = findViewById(R.id.surfaceview_sdr);
    }

    public void initHdrCodec() {
        mHdrExtractor = new MediaExtractor();
        try {
            mHdrExtractor.setDataSource(mFilePath);
        } catch (IOException e) {
            Log.e(LOG_TAG, "initCodec: " + e.getMessage());
        }
        // 媒体文件中的轨道数量 （一般有视频，音频，字幕等）
        int trackCount = mHdrExtractor.getTrackCount();

        // 记录轨道索引id，MediaExtractor 读取数据之前需要指定分离的轨道索引
        int trackID = -1;
        for (int i = 0; i < trackCount; i++) {
            mHdrMediaFormat = mHdrExtractor.getTrackFormat(i);
            String mimeType = mHdrMediaFormat.getString(MediaFormat.KEY_MIME);
            if (!TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_VIDEO_TYPE)) {
                trackID = i;
                mMimeType = mimeType;
                break;
            }
        }

        // 媒体文件中存在视频轨道
        if (trackID != -1) {
            mHdrExtractor.selectTrack(trackID);
            Log.i(LOG_TAG, "trackID = " + trackID);
        }
    }

    public void initSdrCodec() {
        mSdrExtractor = new MediaExtractor();
        try {
            mSdrExtractor.setDataSource(mFilePath);
        } catch (IOException e) {
            Log.e(LOG_TAG, "initCodec2: " + e.getMessage());
        }
        // 媒体文件中的轨道数量 （一般有视频，音频，字幕等）
        int trackCount = mSdrExtractor.getTrackCount();
        // 记录轨道索引id，MediaExtractor 读取数据之前需要指定分离的轨道索引
        int trackId = -1;
        // mime type 指示需要分离的轨道类型
        for (int i = 0; i < trackCount; i++) {
            mSdrMediaFormat = mSdrExtractor.getTrackFormat(i);
            String mimeType = mSdrMediaFormat.getString(MediaFormat.KEY_MIME);
            if (!TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_VIDEO_TYPE)) {
                trackId = i;
                mMimeType = mimeType;
                break;
            }
        }
        // 媒体文件中存在视频轨道
        if (trackId != -1) {
            mSdrExtractor.selectTrack(trackId);
            Log.i(LOG_TAG, "trackId = " + trackId);
        }
    }

    public void playHdrVideo(Surface surface) throws IOException {
        initHdrCodec();
        if (mHdrMediaCodec != null) {
            mHdrMediaCodec.stop();
            mHdrMediaCodec.release();
            mHdrMediaCodec = null;
        }
        mHdrMediaCodec = MediaCodec.createDecoderByType(mMimeType);
        PlayClock clock = new PlayClock();
        clock.setSpeed(1f);
        clock.start();

        mHdrMediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                // Retrieve the set of input buffers. Call this after start() returns. After calling this method, any
                // ByteBuffers previously returned by an earlier call to this method MUST no longer be used.
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                if (inputBuffer == null) {
                    return;
                }
                int sampleSize = mHdrExtractor.readSampleData(inputBuffer, 0);
                if (sampleSize < 0) {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    mCodecStat = 3;
                } else {
                    codec.queueInputBuffer(index, 0, sampleSize, mHdrExtractor.getSampleTime(), 0);
                    mHdrExtractor.advance();
                    mCodecStat = 2;
                }
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index,
                @NonNull MediaCodec.BufferInfo info) {
                // 测试当前播放的视频是否是hdr视频
                if (!mIsPlayingHdrVideo) {
                    MediaFormat bufferFormat = codec.getOutputFormat(index);
                    checkMediaFormat(bufferFormat);
                    mIsPlayingHdrVideo = true;
                }

                switch (index) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d(LOG_TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d(LOG_TAG, "New format " + codec.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(LOG_TAG, "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        // 控制播放速度
                        while (info.presentationTimeUs / 1000 > clock.getCurrentPlayTime()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Log.e(LOG_TAG, "onOutputBufferAvailable1: " + e.getMessage());
                                break;
                            }
                        }
                        if (surface.isValid() && mCodecStat != 6 && mCodecStat != 5) {
                            codec.releaseOutputBuffer(index, true);
                        }
                        break;
                }
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                mCodecStat = 6;
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

            }
        });
        mHdrMediaCodec.configure(mHdrMediaFormat, surface, null, 0);
        mHdrMediaCodec.start();
        mCodecStat = 2;
    }

    public void playSdrVideo(Surface surface) throws IOException {
        initSdrCodec();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mSdrMediaFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER_REQUEST, MediaFormat.COLOR_TRANSFER_SDR_VIDEO);
        }
        if (mSdrMediaCodec != null) {
            mSdrMediaCodec.stop();
            mSdrMediaCodec.release();
            mSdrMediaCodec = null;
        }
        mSdrMediaCodec = MediaCodec.createDecoderByType(mMimeType);

        PlayClock clock = new PlayClock();
        clock.setSpeed(1f);
        clock.start();

        mSdrMediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                // Retrieve the set of input buffers. Call this after start() returns. After calling this method, any
                // ByteBuffers previously returned by an earlier call to this method MUST no longer be used.
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                if (inputBuffer == null) {
                    mCodecStat = 3;
                    return;
                }
                int sampleSize = mSdrExtractor.readSampleData(inputBuffer, 0);
                if (sampleSize < 0) {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    mCodecStat = 3;
                } else {
                    codec.queueInputBuffer(index, 0, sampleSize, mSdrExtractor.getSampleTime(), 0);
                    mSdrExtractor.advance();
                    mCodecStat = 2;
                }
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index,
                @NonNull MediaCodec.BufferInfo info) {
                if (!mIsPlayingSdrVideo) {
                    MediaFormat bufferFormat = codec.getOutputFormat(index);
                    checkMediaFormat(bufferFormat);
                    mIsPlayingSdrVideo = true;
                }
                switch (index) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d(LOG_TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d(LOG_TAG, "New format " + codec.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(LOG_TAG, "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        // 控制播放速度
                        while (info.presentationTimeUs / 1000 > clock.getCurrentPlayTime()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Log.e(LOG_TAG, "onOutputBufferAvailable2: " + e.getMessage());
                                break;
                            }
                        }
                        if (surface.isValid() && mCodecStat != 6 && mCodecStat != 5) {
                            codec.releaseOutputBuffer(index, true);
                        }
                        break;
                }
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                mCodecStat = 6;
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

            }
        });
        mSdrMediaCodec.configure(mSdrMediaFormat, surface, null, 0);
        MediaFormat inputFormatSupport = mSdrMediaCodec.getInputFormat();

        // 判断是否支持HDR转码为SDR
        if (inputFormatSupport
            .getInteger(MediaFormat.KEY_COLOR_TRANSFER_REQUEST) != MediaFormat.COLOR_TRANSFER_SDR_VIDEO) {
            // 支持HDR转码为SDR
            Log.e(LOG_TAG, "不支持HDR转码为SDR");
            return;
        } else {
            Log.i(LOG_TAG, "支持HDR转码为SDR");
        }
        mSdrMediaCodec.start();
        mCodecStat = 2;
    }

    private void checkMediaFormat(MediaFormat format) {
        if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
            int colorStandard = format.getInteger(MediaFormat.KEY_COLOR_STANDARD);
            if (colorStandard == MediaFormat.COLOR_STANDARD_BT2020) {
                // 这是一个 HDR 帧
                Log.i(LOG_TAG, "HDR Frame");
                runOnUiThread(() -> Toast.makeText(Hdr2SdrActivity.this, "您播放了一个HDR视频", Toast.LENGTH_SHORT).show());
            } else {
                // 这是一个 SDR 帧
                Log.i(LOG_TAG, "SDR Frame");
                runOnUiThread(() -> Toast.makeText(Hdr2SdrActivity.this, "您播放了一个SDR视频", Toast.LENGTH_SHORT).show());
            }
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
    }

    static class PlayClock {
        private long start;

        private float speed = 1.0f;

        public void setSpeed(float speed) {
            this.speed = speed;
        }

        public void start() {
            start = SystemClock.elapsedRealtime();
        }

        public long getCurrentPlayTime() {
            return (long) ((float) (SystemClock.elapsedRealtime() - start) * speed);
        }
    }
}