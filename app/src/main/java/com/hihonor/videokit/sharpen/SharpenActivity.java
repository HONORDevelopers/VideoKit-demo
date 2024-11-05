/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.videokit.sharpen;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hihonor.videokit.BaseActivity;
import com.hihonor.videokit.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

public class SharpenActivity extends BaseActivity implements SurfaceHolder.Callback {
    private static final String LOG_TAG = "SharpenActivity";

    private static final String SUPER_RES_FILEPATH =
        Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/test_super_res.mp4";

    private static final String SHARPEN_FILEPATH =
        Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/test_sharpen.mp4";

    private static final int REQUEST_VIDEO = 1;

    // 0:configured 1:flushed 2:running 3:EOS 4uninitialized 4:stopped 5:release 6:error
    private int mCodecStat;

    private String mFilePath;

    private MediaExtractor mExtractor;

    private MediaFormat mDecoderFormat;

    private MediaCodec mMediaCodec;

    private String mMimeType;

    private SurfaceView mSurfaceView;

    private TextView mTvVideoRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharpen);
        initView();
    }

    public void startPlayVideo(View view) {
        if (TextUtils.isEmpty(mFilePath)) {
            Toast.makeText(this, "请先选择视频文件", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(mMimeType)) {
            Toast.makeText(this, "素材异常，未发现视频轨道", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startPlayVideo(mSurfaceView.getHolder().getSurface());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void selectVideo(View view) {
        // 选择视频 （mp4 3gp 是android支持的视频格式）
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_VIDEO);
    }

    private void setVideo() {
        int mVideoWidth;
        int mVideoHeight;
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mFilePath);

            // 记录轨道索引id，MediaExtractor 读取数据之前需要指定分离的轨道索引
            int trackId = -1;
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                mDecoderFormat = mExtractor.getTrackFormat(i);
                String mimeType = mDecoderFormat.getString(MediaFormat.KEY_MIME);
                if (!TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_VIDEO_TYPE)) {
                    trackId = i;
                    mMimeType = mimeType;
                    break;
                }
            }
            if (trackId == -1) {
                Toast.makeText(this, "视频文件中不存在视频轨道", Toast.LENGTH_SHORT).show();
                return;
            }
            mExtractor.selectTrack(trackId);
            mVideoWidth = mDecoderFormat.getInteger("width");
            mVideoHeight = mDecoderFormat.getInteger("height");
            if (mVideoWidth <= 0 || mVideoHeight <= 0) {
                Toast.makeText(mContext, "视频异常，请重新选择", Toast.LENGTH_SHORT).show();
                return;
            }
            mSurfaceView.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
            if (mVideoWidth >= mVideoHeight) {
                // 横屏视频
                layoutParams.width = mScreenWidth * 9 / 10;
                layoutParams.height = mScreenWidth * 9 / 10 * mVideoHeight / mVideoWidth;
            } else {
                // 竖屏视频
                layoutParams.width = mScreenWidth * 6 / 10;
                layoutParams.height = mScreenWidth * 6 / 10 * mVideoHeight / mVideoWidth;
            }
            mSurfaceView.setLayoutParams(layoutParams);
        } catch (IOException e) {
            Log.e(LOG_TAG, "initCodec: " + e.getMessage());
            return;
        }
        mTvVideoRes.setText(String.format(Locale.ENGLISH, "视频分辨率： %d x %d", mVideoWidth, mVideoHeight));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_VIDEO) {
                Uri videoUri = data.getData();
                if (videoUri == null) {
                    return;
                }
                String[] filePathColumn = {MediaStore.Video.Media.DATA};
                Cursor cursor = getContentResolver().query(videoUri, filePathColumn, null, null, null);
                if (cursor == null) {
                    return;
                }
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mFilePath = cursor.getString(columnIndex);
                cursor.close();
                setVideo();
            }
        }
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceView.getHolder().addCallback(this);
        mTvVideoRes = findViewById(R.id.tv_video_res);
    }

    public void startPlayVideo(Surface surface) throws IOException {
        Toast.makeText(this, "已自动打开视频超分能力和自适应锐化", Toast.LENGTH_SHORT).show();

        // 0:关闭视频超分能力和自适应锐化 1：打开视频超分能力和自适应锐化;
        mDecoderFormat.setInteger("honor.video.aisr.enable", 1);
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        mMediaCodec = MediaCodec.createDecoderByType(mMimeType);
        PlayClock clock = new PlayClock();
        clock.setSpeed(1f);
        clock.start();

        mMediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                // Retrieve the set of input buffers. Call this after start() returns. After calling this method, any
                // ByteBuffers previously returned by an earlier call to this method MUST no longer be used.
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                if (inputBuffer == null) {
                    mCodecStat = 3;
                    return;
                }
                int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
                if (sampleSize < 0) {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    mCodecStat = 3;
                } else {
                    codec.queueInputBuffer(index, 0, sampleSize, mExtractor.getSampleTime(), 0);
                    mExtractor.advance();
                    mCodecStat = 2;
                }
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index,
                @NonNull MediaCodec.BufferInfo info) {
                // 检查帧的分辨率
                MediaFormat bufferFormat = codec.getOutputFormat(index);
                if (bufferFormat.containsKey(MediaFormat.KEY_WIDTH)
                    && bufferFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
                    int width = bufferFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int height = bufferFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    Log.d(LOG_TAG, "frame resolution: " + width + " x " + height);
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
                                Log.e(LOG_TAG, "onOutputBufferAvailable sleep: " + e.getMessage());
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
        mMediaCodec.configure(mDecoderFormat, surface, null, 0);
        mMediaCodec.start();
        mCodecStat = 2;
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

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaCodec != null && mCodecStat == 2) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
            mCodecStat = 5;
            mExtractor.release();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}