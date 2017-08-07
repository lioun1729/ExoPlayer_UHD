/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.google.android.exoplayer2.UHDTest;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.lifemedia.BatteryCollect;
import com.google.android.exoplayer2.lifemedia.ContextInformation;
import com.google.android.exoplayer2.lifemedia.HDMIConnectionEvent;
import com.google.android.exoplayer2.lifemedia.InetInfoCollect;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends Activity implements OnClickListener, ExoPlayer.EventListener,
    PlaybackControlView.VisibilityListener {

  public static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";
  public static final String DRM_LICENSE_URL = "drm_license_url";
  public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";
  public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

  public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
  public static final String EXTENSION_EXTRA = "extension";

  public static final String ACTION_VIEW_LIST =
      "com.google.android.exoplayer.demo.action.VIEW_LIST";
  public static final String URI_LIST_EXTRA = "uri_list";
  public static final String EXTENSION_LIST_EXTRA = "extension_list";

  public static final String PROXY_ADDRESS_EXTRA = "proxy_address";
  public static final String PROXY_PORT_EXTRA = "proxy_port";

  public static final String TAG = "TestUHD"; // For LogCat

  private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
  private static final CookieManager DEFAULT_COOKIE_MANAGER;

  private static long currentTimeInfo;

  static {
    DEFAULT_COOKIE_MANAGER = new CookieManager();
    DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private Handler mainHandler;
  private EventLogger eventLogger;
  private SimpleExoPlayerView simpleExoPlayerView;
  private LinearLayout debugRootView;
  private TextView debugTextView;
  private Button retryButton;

  private DataSource.Factory mediaDataSourceFactory;
  private SimpleExoPlayer player;
  private DefaultTrackSelector trackSelector;
  private TrackSelectionHelper trackSelectionHelper;
  private DebugTextViewHelper debugViewHelper;
  private boolean needRetrySource;
  private TrackGroupArray lastSeenTrackGroupArray;

  private boolean shouldAutoPlay;
  private int resumeWindow;
  private long resumePosition;
  private static final String TAG_PLAY = "PlayerActivity";

  private Uri contentUri;
  private int contentType;
  private String contentId;
  private String provider;
  private boolean enableBackgroundAudio;
  private long playerPosition;

  private boolean CountDownActivated = false;
  private CountDownTimer cdTimer = null;
  private boolean CountDownCanceled = false;

  private boolean GetSaveTimeActivated = false;
  private String playerState = "stop";
  private String currentVideoURL;
  private String serverVideoURL;
  private String proxyVideoURL;
  private boolean streamChanged = false;
  private boolean enableProxySetting;
  private boolean streamNowChanging = false;
  private boolean stallingEvent = false;
  private String existingState = "";
  private Context context;

  private int lastReportedPlaybackState;
  private boolean lastReportedPlayWhenReady;
  private final CopyOnWriteArrayList<ExoPlayer.EventListener> listeners = new CopyOnWriteArrayList<>();

  private ContextInformation contextInfo;
  private BatteryCollect batteryInfo;
  private InetInfoCollect inetInfo;
  private HDMIConnectionEvent hdmiInfo;

  private String proxyAddress;
  private String proxyPort;

  private int uhd_stallingCount;
  private double uhd_stallingStartTime;
  private double uhd_stallingEndTime;
  private double uhd_stallingTime;

  // Activity lifecycle

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ////////// Device Related Context Information //////////
    contextInfo = new ContextInformation();
    batteryInfo = new BatteryCollect(contextInfo);
    inetInfo = new InetInfoCollect(contextInfo);
    hdmiInfo = new HDMIConnectionEvent(contextInfo);
    registerReceiver(batteryInfo, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    registerReceiver(inetInfo, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
    registerReceiver(inetInfo, new IntentFilter(Context.CONNECTIVITY_SERVICE));
    registerReceiver(hdmiInfo, new IntentFilter("android.intent.action.HDMI_PLUGGED"));

    shouldAutoPlay = true;
    clearResumePosition();
    mediaDataSourceFactory = buildDataSourceFactory(true);
    mainHandler = new Handler();
    if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
      CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
    }

    setContentView(R.layout.player_activity);
    View rootView = findViewById(R.id.root);
    rootView.setOnClickListener(this);
    debugRootView = (LinearLayout) findViewById(R.id.controls_root);
    debugTextView = (TextView) findViewById(R.id.debug_text_view);
    retryButton = (Button) findViewById(R.id.retry_button);
    retryButton.setOnClickListener(this);

    simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
    simpleExoPlayerView.setControllerVisibilityListener(this);
    simpleExoPlayerView.requestFocus();

    uhd_stallingCount = 0;
    uhd_stallingStartTime = 0;
    uhd_stallingEndTime = 0;
    uhd_stallingTime = 0;

  }

  @Override
  public void onNewIntent(Intent intent) {
    releasePlayer();
    shouldAutoPlay = true;
    clearResumePosition();
    setIntent(intent);
  }

  @Override
  public void onStart() {
    super.onStart();
    if (Util.SDK_INT > 23) {
      initializePlayer();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if ((Util.SDK_INT <= 23 || player == null)) {
      initializePlayer();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (Util.SDK_INT <= 23) {
      releasePlayer();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (Util.SDK_INT > 23) {
      releasePlayer();
    }

    CountDownCanceled = true;
    sendState("stop");
    playerState = "stop";

    unregisterReceiver(batteryInfo);
    unregisterReceiver(inetInfo);
    unregisterReceiver(hdmiInfo);

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      initializePlayer();
    } else {
      showToast(R.string.storage_permission_denied);
      finish();
    }
  }

  // Activity input

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    // Show the controls on any key event.
    simpleExoPlayerView.showController();
    // If the event was not handled then see if the player view can handle it as a media key event.
    return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event);
  }

  // OnClickListener methods

  @Override
  public void onClick(View view) {
    if (view == retryButton) {
      initializePlayer();
    } else if (view.getParent() == debugRootView) {
      MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo != null) {
        trackSelectionHelper.showSelectionDialog(this, ((Button) view).getText(),
            trackSelector.getCurrentMappedTrackInfo(), (int) view.getTag());
      }
    }
  }

  // PlaybackControlView.VisibilityListener implementation

  @Override
  public void onVisibilityChange(int visibility) {
    debugRootView.setVisibility(visibility);
  }

  // Internal methods

  private void initializePlayer() {
    Intent intent = getIntent();
    boolean needNewPlayer = player == null;
    if (needNewPlayer) {
      TrackSelection.Factory adaptiveTrackSelectionFactory =
          new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
      trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
      trackSelectionHelper = new TrackSelectionHelper(trackSelector, adaptiveTrackSelectionFactory);
      lastSeenTrackGroupArray = null;
      eventLogger = new EventLogger(trackSelector);

      UUID drmSchemeUuid = intent.hasExtra(DRM_SCHEME_UUID_EXTRA)
          ? UUID.fromString(intent.getStringExtra(DRM_SCHEME_UUID_EXTRA)) : null;
      DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
      if (drmSchemeUuid != null) {
        String drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL);
        String[] keyRequestPropertiesArray = intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES);
        try {
          drmSessionManager = buildDrmSessionManager(drmSchemeUuid, drmLicenseUrl,
              keyRequestPropertiesArray);
        } catch (UnsupportedDrmException e) {
          int errorStringId = Util.SDK_INT < 18 ? R.string.error_drm_not_supported
              : (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                  ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
          showToast(errorStringId);
          return;
        }
      }

      ////////// Get Proxy Information from Intent //////////

      proxyAddress = intent.getStringExtra(PROXY_ADDRESS_EXTRA);
      proxyPort = intent.getStringExtra(PROXY_PORT_EXTRA);

      Toast.makeText(getApplicationContext(), "Main : " + proxyAddress + ":" + proxyPort, Toast.LENGTH_SHORT).show();

      boolean preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false);
      @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode =
          ((DemoApplication) getApplication()).useExtensionRenderers()
              ? (preferExtensionDecoders ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
              : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
              : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
      DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this,
          drmSessionManager, extensionRendererMode);

      player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
      player.addListener(this);
      player.addListener(eventLogger);
      player.setAudioDebugListener(eventLogger);
      player.setVideoDebugListener(eventLogger);
      player.setMetadataOutput(eventLogger);

      simpleExoPlayerView.setPlayer(player);
      player.setPlayWhenReady(shouldAutoPlay);
      debugViewHelper = new DebugTextViewHelper(player, debugTextView);
      debugViewHelper.start();
    }
    if (needNewPlayer || needRetrySource) {
      String action = intent.getAction();
      Uri[] uris;
      String[] extensions;
      if (ACTION_VIEW.equals(action)) {
        uris = new Uri[] {intent.getData()};
        extensions = new String[] {intent.getStringExtra(EXTENSION_EXTRA)};
      } else if (ACTION_VIEW_LIST.equals(action)) {
        String[] uriStrings = intent.getStringArrayExtra(URI_LIST_EXTRA);
        uris = new Uri[uriStrings.length];
        for (int i = 0; i < uriStrings.length; i++) {
          uris[i] = Uri.parse(uriStrings[i]);
        }
        extensions = intent.getStringArrayExtra(EXTENSION_LIST_EXTRA);
        if (extensions == null) {
          extensions = new String[uriStrings.length];
        }
      } else {
        showToast(getString(R.string.unexpected_intent_action, action));
        return;
      }
      if (Util.maybeRequestReadExternalStoragePermission(this, uris)) {
        // The player will be reinitialized if the permission is granted.
        return;
      }
      MediaSource[] mediaSources = new MediaSource[uris.length];
      for (int i = 0; i < uris.length; i++) {
        mediaSources[i] = buildMediaSource(uris[i], extensions[i]);
      }
      MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
          : new ConcatenatingMediaSource(mediaSources);
      boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
      if (haveResumePosition) {
        player.seekTo(resumeWindow, resumePosition);
      }
      player.prepare(mediaSource, !haveResumePosition, false);
      needRetrySource = false;
      updateButtonVisibilities();
    }
  }


  ////////// Build Media Source according to Source Type //////////
  private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
    int type = TextUtils.isEmpty(overrideExtension) ? Util.inferContentType(uri)
        : Util.inferContentType("." + overrideExtension);
    switch (type) {
      case C.TYPE_SS:
        return new SsMediaSource(uri, buildDataSourceFactory(false),
            new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
      case C.TYPE_DASH:
        return new DashMediaSource(uri, buildDataSourceFactory(false),
            new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
      case C.TYPE_HLS:
        return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
      case C.TYPE_OTHER:
        return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
            mainHandler, eventLogger);
      default: {
        throw new IllegalStateException("Unsupported type: " + type);
      }
    }
  }

  private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(UUID uuid,
      String licenseUrl, String[] keyRequestPropertiesArray) throws UnsupportedDrmException {
    if (Util.SDK_INT < 18) {
      return null;
    }
    HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
        buildHttpDataSourceFactory(false));
    if (keyRequestPropertiesArray != null) {
      for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
        drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
            keyRequestPropertiesArray[i + 1]);
      }
    }
    return new DefaultDrmSessionManager<>(uuid,
        FrameworkMediaDrm.newInstance(uuid), drmCallback, null, mainHandler, eventLogger);
  }

  private void releasePlayer() {
    if (player != null) {
      debugViewHelper.stop();
      debugViewHelper = null;
      shouldAutoPlay = player.getPlayWhenReady();
      updateResumePosition();
      player.release();
      player = null;
      trackSelector = null;
      trackSelectionHelper = null;
      eventLogger = null;
    }
  }

  private void updateResumePosition() {
    resumeWindow = player.getCurrentWindowIndex();
    resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
        : C.TIME_UNSET;
  }

  private void clearResumePosition() {
    resumeWindow = C.INDEX_UNSET;
    resumePosition = C.TIME_UNSET;
  }

  /**
   * Returns a new DataSource factory.
   *
   * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
   *     DataSource factory.
   * @return A new DataSource factory.
   */
  private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
    return ((DemoApplication) getApplication())
        .buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
  }

  /**
   * Returns a new HttpDataSource factory.
   *
   * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
   *     DataSource factory.
   * @return A new HttpDataSource factory.
   */
  private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
    return ((DemoApplication) getApplication())
        .buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
  }

  // ExoPlayer.EventListener implementation

  @Override
  public void onLoadingChanged(boolean isLoading) {
    // Do nothing.
  }

  ////////// Player State Transmission //////////
  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    if (playbackState == ExoPlayer.STATE_ENDED) {
      needRetrySource = true;
      showControls();
    }

    switch(playbackState){
      case ExoPlayer.STATE_BUFFERING:
        Log.d(TAG,"STATE_BUFFERING");
        if(playerState.equals("start")){
          uhd_stallingStartTime = System.currentTimeMillis();
          //Log.d(TAG, "Stalling Start Time : " + String.valueOf(stallingStartTime));
          stallingEvent = true;
          sendState("stalling");
        }
        break;

      case ExoPlayer.STATE_ENDED:
        Log.d(TAG,"STATE_ENDED");
        retry();
        if(!playerState.equals("end")){
          sendState("end");
          playerState = "end";
        }
        break;

      case ExoPlayer.STATE_IDLE:
        Log.d(TAG,"STATE_IDLE");
        break;

      case ExoPlayer.STATE_READY:
        Log.d(TAG,"STATE_READY");
        if(GetSaveTimeActivated && !playerState.equals("init")){
          // Pause
          Log.d(TAG,"init");
          sendState("init");
          playerState = "init";
        }

        else if(player.getPlayWhenReady() && !playerState.equals("start")){
          Log.d(TAG,"start");
          sendState("start");
          playerState = "start";
        }

        else if(!player.getPlayWhenReady() && playerState.equals("start")){
          Log.d(TAG,"pause");
          sendState("pause");
          playerState = "pause";
        }

        else if(stallingEvent){
          Log.d(TAG,"stalling");
          sendState("start");
          stallingEvent = false;
          uhd_stallingCount++;
          uhd_stallingEndTime = System.currentTimeMillis();
          uhd_stallingTime = uhd_stallingEndTime - uhd_stallingStartTime;
          //Log.d(TAG, "Stalling End Time : " + String.valueOf(stallingEndTime));
          //Log.d(TAG, "Stalling Time : " + String.valueOf(stallingEndTime - stallingStartTime));
        }

        if(!GetSaveTimeActivated){
          Log.d(TAG_PLAY,"CountDown");
          if(CountDownActivated){
            cdTimer.cancel();
            cdTimer = null;
          }

          CountDownPlayer(player.getDuration(), player.getCurrentPosition());
          cdTimer.start();
        }

        else {
          ; // Pause
        }
        break;

      default:
        break;
    }

    updateButtonVisibilities();
    maybeReportPlayerState();

  }

  @Override
  public void onPositionDiscontinuity() {
    if (needRetrySource) {
      // This will only occur if the user has performed a seek whilst in the error state. Update the
      // resume position so that if the user then retries, playback will resume from the position to
      // which they seeked.
      updateResumePosition();
    }
  }

  @Override
  public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    // Do nothing.
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest) {
    // Do nothing.
  }

  @Override
  public void onPlayerError(ExoPlaybackException e) {
    String errorString = null;
    if (e.type == ExoPlaybackException.TYPE_RENDERER) {
      Exception cause = e.getRendererException();
      if (cause instanceof DecoderInitializationException) {
        // Special case for decoder initialization failures.
        DecoderInitializationException decoderInitializationException =
            (DecoderInitializationException) cause;
        if (decoderInitializationException.decoderName == null) {
          if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
            errorString = getString(R.string.error_querying_decoders);
          } else if (decoderInitializationException.secureDecoderRequired) {
            errorString = getString(R.string.error_no_secure_decoder,
                decoderInitializationException.mimeType);
          } else {
            errorString = getString(R.string.error_no_decoder,
                decoderInitializationException.mimeType);
          }
        } else {
          errorString = getString(R.string.error_instantiating_decoder,
              decoderInitializationException.decoderName);
        }
      }
    }
    if (errorString != null) {
      showToast(errorString);
    }
    needRetrySource = true;
    if (isBehindLiveWindow(e)) {
      clearResumePosition();
      initializePlayer();
    } else {
      updateResumePosition();
      updateButtonVisibilities();
      showControls();
    }
  }

  @Override
  @SuppressWarnings("ReferenceEquality")
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    updateButtonVisibilities();
    if (trackGroups != lastSeenTrackGroupArray) {
      MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo != null) {
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          showToast(R.string.error_unsupported_video);
        }
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          showToast(R.string.error_unsupported_audio);
        }
      }
      lastSeenTrackGroupArray = trackGroups;
    }
  }

  // User controls

  private void updateButtonVisibilities() {
    debugRootView.removeAllViews();

    retryButton.setVisibility(needRetrySource ? View.VISIBLE : View.GONE);
    debugRootView.addView(retryButton);

    if (player == null) {
      return;
    }

    MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
    if (mappedTrackInfo == null) {
      return;
    }

    for (int i = 0; i < mappedTrackInfo.length; i++) {
      TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
      if (trackGroups.length != 0) {
        Button button = new Button(this);
        int label;
        switch (player.getRendererType(i)) {
          case C.TRACK_TYPE_AUDIO:
            label = R.string.audio;
            break;
          case C.TRACK_TYPE_VIDEO:
            label = R.string.video;
            break;
          case C.TRACK_TYPE_TEXT:
            label = R.string.text;
            break;
          default:
            continue;
        }
        button.setText(label);
        button.setTag(i);
        button.setOnClickListener(this);
        debugRootView.addView(button, debugRootView.getChildCount() - 1);
      }
    }
  }

  private void showControls() {
    debugRootView.setVisibility(View.VISIBLE);
  }

  private void showToast(int messageId) {
    showToast(getString(messageId));
  }

  private void showToast(String message) {
    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
  }

  private static boolean isBehindLiveWindow(ExoPlaybackException e) {
    if (e.type != ExoPlaybackException.TYPE_SOURCE) {
      return false;
    }
    Throwable cause = e.getSourceException();
    while (cause != null) {
      if (cause instanceof BehindLiveWindowException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }


  ////////// For UHD Test //////////

  public void CountDownPlayer(long VideoDuration, long CurrentPosition) {
    //Log.d(TAG_PLAY,"CountDownPlayer");
    CountDownActivated = true;
    cdTimer = new CountDownTimer(VideoDuration - CurrentPosition, 1000) {
      @Override
      public void onFinish() {
      }

      @Override
      public void onTick(long millisUntilFinished) {

        if (CountDownCanceled) {
          cancel();
        }

        else {
          try {
            sendCurrentPosition();
          }

          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    };
  }

  private void maybeReportPlayerState() {
    //Log.d(TAG_PLAY,"maybeReportPlayerState");
    boolean playWhenReady = player.getPlayWhenReady();
    int playbackState = player.getPlaybackState();
    if (lastReportedPlayWhenReady != playWhenReady || lastReportedPlaybackState != playbackState) {
      for (ExoPlayer.EventListener listener : listeners) {
        listener.onPlayerStateChanged(playWhenReady, playbackState);
      }
      lastReportedPlayWhenReady = playWhenReady;
      lastReportedPlaybackState = playbackState;
    }
  }

  ////////// Send Context Information to Proxy //////////
  ////////// Current for LifeMedia Monitor Servlet //////////
  ////////// Must Change to Other Servlet //////////
  public void sendCurrentPosition() {
    Log.d(TAG,"sendCurrentPosition");

    /*String ProxyURL = getServerURL("ProxyURL");
    String ProxyPort = getServerURL("ProxyPort");*/

    String ProxyURL = proxyAddress;
    String ProxyPort = proxyPort;

    ProxyURL = ProxyURL + ":" + ProxyPort + "/dash_lifemedia/lifemedia-monitoring/MonitorServlet?";

    String RequestMessage = "";
    Map<String, String> requestHeader = new HashMap<String, String>();

    long currentPosition = player.getCurrentPosition();
    double videoDuration = player.getDuration();
    int videoHeight = 0;
    double currentBitrate = 0;
    int RSSI = ContextInformation.RSSI;
    int batteryPercent = ContextInformation.batteryPercent;
    double estimatedBandwidth = BANDWIDTH_METER.getBitrateEstimate();
    double estimatedBandwidthJitter = BANDWIDTH_METER.getBitrateJitterEstimate();

    //Log.d(TAG_PLAY,"Estimated Bandwidth : " + estimatedBandwidth);

    currentTimeInfo = currentPosition;

    if(player.getVideoFormat() == null){
      videoHeight = -1;
      currentBitrate = -1;
    }

    else{
      currentBitrate = player.getVideoFormat().bitrate;
      videoHeight = player.getVideoFormat().height;
    }

    //Log.d(TAG_PLAY,"Bitrate : " + currentBitrate);

    double bufferedPostionPercent = ((double) player.getBufferedPercentage() / 100.0)
            * (double) player.getDuration();
    double positionMs = (bufferedPostionPercent - (double) player.getCurrentPosition());

    double parsePosition = Double.parseDouble(String.format("%.3f",(positionMs / 1000)));
    double parseCurrentBitrate = Double.parseDouble(String.format("%.3f",(currentBitrate / 1000000)));
    double parseEstimatedBandwidth = Double.parseDouble(String.format("%.3f",(estimatedBandwidth / 1000000)));
    double parseEstimatedBandwidthJitter = Double.parseDouble(String.format("%.3f",(estimatedBandwidthJitter / 1000000)));
    double parseVideoDuration = Double.parseDouble(String.format("%.3f",(videoDuration / 1000)));
    double parseStallingTime = Double.parseDouble(String.format("%.3f",(uhd_stallingTime / 1000)));

    requestHeader.put("X-LifeMedia-AndroidID", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID));
    requestHeader.put("User-Agent", Util.getUserAgent(getApplicationContext(), "ExoPlayerDemo"));
    requestHeader.put("X-LifeMedia-CurrentPosition", String.valueOf(currentPosition / 1000));
    requestHeader.put("X-LifeMedia-CurrentBitrate", String.valueOf(parseCurrentBitrate));
    requestHeader.put("X-LifeMedia-CurrentQuality", String.valueOf(videoHeight));
    requestHeader.put("X-LifeMedia-Duration", String.valueOf(parseVideoDuration));
    requestHeader.put("X-LifeMedia-CurrentBuffer", String.valueOf(parsePosition));
    requestHeader.put("X-LifeMedia-EstimatedBandwidth", String.valueOf(parseEstimatedBandwidth));
    //requestHeader.put("X-LifeMedia-ContentURL", currentVideoURL);
    requestHeader.put("X-LifeMedia-EstimatedBandwidthJitter", String.valueOf(parseEstimatedBandwidthJitter));
    requestHeader.put("X-LifeMedia-RSSI", String.valueOf(RSSI));
    requestHeader.put("X-LifeMedia-BatteryPercent",String.valueOf(batteryPercent));
    requestHeader.put("X-LifeMedia-RequestURL", DefaultHttpDataSource.requestUrl.toString());
    requestHeader.put("X-LifeMedia-RequestByteRange", DefaultHttpDataSource.requestRange);
    requestHeader.put("X-LifeMedia-StallingCount", String.valueOf(uhd_stallingCount));
    requestHeader.put("X-LifeMedia-StallingTime", String.valueOf(parseStallingTime));

    //Log.d(TAG_PLAY,"Player State : " + playerState);

    if(playerState != "pause"){ // Notify Real-Time Update
      sendState("start");
    }

    RequestEvent();

    requestHeader.put("X-LifeMedia-DeviceType",ContextInformation.deviceType);
    requestHeader.put("X-LifeMedia-ConnectionType",ContextInformation.connectionType);
    requestHeader.put("X-LifeMedia-DeviceResolutionWidth",String.valueOf(ContextInformation.width));
    requestHeader.put("X-LifeMedia-DeviceResolutionHeight",String.valueOf(ContextInformation.height));

    RequestMessage = "currentInfo=OKOK";
    // Why this message exists?

    //Log.d(TAG_PLAY,"Current Quality : " + String.valueOf(videoHeight));


    try {
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);
      Util.executePost(ProxyURL, RequestMessage.getBytes(), requestHeader);
    }

    catch (IOException e) {
      e.printStackTrace();
    }

    catch (Exception e) {
      e.printStackTrace();
    }
  }

  ////////// Send State to Proxy //////////
  @SuppressLint("SimpleDateFormat")
  public void sendState(String clientState) {

    Log.d(TAG,"sendState");

    //Log.d(TAG_PLAY,"Response Quality : " + Util.responseHeader);

   /* String ProxyURL = getServerURL("ProxyURL");
    String ProxyPort = getServerURL("ProxyPort");*/

    String ProxyURL = proxyAddress;
    String ProxyPort = proxyPort;

    ProxyURL = ProxyURL + ":" + ProxyPort + "/dash_lifemedia/lifemedia-monitoring/MonitorServlet?";
    Map<String, String> requestHeader = new HashMap<String, String>();
    requestHeader.put("X-LifeMedia-AndroidID", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID));
    //requestHeader.put("User-Agent", Util.getUserAgent(getApplicationContext(), "ExoPlayerDemo"));
    int playType = 0;

    /*if(!enableProxySetting){
      playType = QoSNotEnable;
    }

    else{

      if(ContextInformation.contextEnable){
        playType = QoSandContextEnable;
      }

      else{
        playType = QoSEnable;
      }
    }*/

    //Log.d(TAG_PLAY,"Client State : " + clientState);

    if (clientState.equals("stop")) {
      requestHeader.put("X-LifeMedia-ViewingDuration", String.valueOf(currentTimeInfo / 1000));
      requestHeader.put("X-LifeMedia-ViewingDeviceType", ContextInformation.deviceType);
      //long time = System.currentTimeMillis();
      Date today = new Date();

      SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd aa", Locale.US);
      SimpleDateFormat time = new SimpleDateFormat("hh:mm:ss",Locale.US);

      String dayString = day.format(today);
      String timeString = time.format(today);

      requestHeader.put("X-LifeMedia-ViewingTime", dayString + " " + timeString);

      //Log.d(TAG_PLAY,"Viewing Duration : " + String.valueOf(playerPosition));
    }

    try {
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);
      String stateData = "state=" + clientState + "&playtype=" + playType;
      existingState = clientState;
      Util.executePost(ProxyURL, stateData.getBytes(), requestHeader);
    }

    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void retry(){
    //Log.d(TAG_PLAY , "retry");
    toggleControlsVisibility();
  }

  private void toggleControlsVisibility(){
    //Log.d(TAG_PLAY, "toggleControlsVisibility");
    debugRootView.setVisibility(View.GONE);
  }

  ////////// Get Device Information, RSSI and Battery Percent //////////
  public void RequestEvent() {
    Log.d(TAG,"RequestEvent");
    // Display

    WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
    Display display = windowManager.getDefaultDisplay();
    Point displaySize = new Point();

    if (Util.SDK_INT >= 23) {
      // Log.d(TAG_PLAY,"SDK_INT >= 23");
      getDisplaySizeV23(display, displaySize);
    }

    else if (Util.SDK_INT >= 17) {
      // Log.d(TAG_PLAY,"SDK_INT >= 17");
      getDisplaySizeV17(display, displaySize);
    }

    else if (Util.SDK_INT >= 16) {
      // Log.d(TAG_PLAY,"SDK_INT >= 16");
      getDisplaySizeV16(display, displaySize);
    }

    else {
      //Log.d(TAG_PLAY,"SDK_INT >= 9");
      getDisplaySizeV9(display, displaySize);
    }

    // Internet Connection Type
    WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    WifiInfo wifiInfo = null;
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    boolean isConnected = activeNetwork.isConnectedOrConnecting();
    boolean isWiFi;
    String connectionType;
    int RSSI = 0;

    if(wifiManager != null){
      wifiInfo = wifiManager.getConnectionInfo();
      if(wifiInfo != null){
        RSSI = wifiInfo.getRssi();
      }
    }

    Intent temporalIntent = getIntent();

    int batteryStatus = temporalIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    boolean isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING || batteryStatus == BatteryManager.BATTERY_STATUS_FULL;
    int level = temporalIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    int scale = temporalIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    int batteryRatio = (level * 100) / scale;

    if(isCharging && batteryRatio != 0){
      ContextInformation.batteryPercent = batteryRatio;
    }

    else if(batteryRatio < 100 && batteryRatio != 0){
      ContextInformation.batteryPercent = batteryRatio;
    }

    if (isConnected) {
      isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
      ContextInformation.connectionType = isWiFi ? "WiFi" : "Mobile";
    }

    connectionType = ContextInformation.connectionType;

    if(connectionType != null){
      if(connectionType.equals("Mobile")){
        ContextInformation.RSSI = 0;
      }

      else if(connectionType.equals("WiFi") && RSSI != 0 && wifiInfo != null){
        ContextInformation.RSSI = RSSI;
      }
    }

    String deviceType = isTablet(getApplicationContext()) ? "Tab" : "Phone";

    double bufferedPostionPercent = ((double) player.getBufferedPercentage() / 100.0) * (double) player.getDuration();
    double positionMs = (bufferedPostionPercent - (double) player.getCurrentPosition());


    // Network Context Information
    ContextInformation.bandwidth = BANDWIDTH_METER.getBitrateEstimate();
    ContextInformation.bitrateJitter = BANDWIDTH_METER.getBitrateJitterEstimate();
    // Device Context Information
    ContextInformation.deviceModel = Build.MODEL;
    ContextInformation.width = displaySize.x;
    ContextInformation.height = displaySize.y;
    ContextInformation.deviceType = deviceType;
    ContextInformation.bufferMs = positionMs;
    ContextInformation.androidID = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

  }

  @TargetApi(23)
  private static void getDisplaySizeV23(Display display, Point outSize) {
    Display.Mode mode = display.getMode();
    outSize.x = mode.getPhysicalWidth();
    outSize.y = mode.getPhysicalHeight();
  }

  @TargetApi(17)
  private static void getDisplaySizeV17(Display display, Point outSize) {
    display.getRealSize(outSize);
  }

  @TargetApi(16)
  private static void getDisplaySizeV16(Display display, Point outSize) {
    display.getSize(outSize);
  }

  @SuppressWarnings("deprecation")
  private static void getDisplaySizeV9(Display display, Point outSize) {
    outSize.x = display.getWidth();
    outSize.y = display.getHeight();
  }

  private static boolean isTablet(Context context) {
    return (context.getResources().getConfiguration().screenLayout
            & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
  }

}
