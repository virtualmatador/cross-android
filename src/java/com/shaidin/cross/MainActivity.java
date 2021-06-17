//
//  MainActivity.java
//  android
//
//  Created by Ali Asadpoor on 1/15/19.
//  Copyright © 2020 Shaidin. All rights reserved.
//

package com.shaidin.cross;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.AudioAttributes;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.StringTokenizer;

public class MainActivity extends Activity
{
    WebView web_view_;
    ImageView image_view_;
    View close_button_;
    int sender_;
    int[] pixels_;
    Bitmap bmp_;
    HashMap<String, String> http_params_ = new HashMap<String, String>();
    SoundPool player_ = null;;
    int[] tracks_;

    static {System.loadLibrary("native");}
    public native void Setup();
    public native void Begin();
    public native void End();
    public native void Create();
    public native void Destroy();
    public native void Start();
    public native void Stop();
    public native void Restart();
    public native void Escape();
    public native void Handle(String id, String command, String info);
    public native void HandleAsync(int sender, String id, String command, String info);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        web_view_ = findViewById(R.id.webView);
        web_view_.getSettings().setJavaScriptEnabled(true);
        web_view_.addJavascriptInterface(this, "Handler_");
        image_view_ = findViewById(R.id.imageView);
        image_view_.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                switch (motionEvent.getAction())
                {
                case MotionEvent.ACTION_DOWN:
                    Handle("body", "touch-begin", motionEvent.getX() + " " + motionEvent.getY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    Handle("body", "touch-move", motionEvent.getX() + " " + motionEvent.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    Handle("body", "touch-end", motionEvent.getX() + " " + motionEvent.getY());
                    break;
                }
                return true;
            }
        });
        close_button_ = findViewById(R.id.imageButton);
        http_params_ = new HashMap<String, String>();
        Setup();
        Begin();
    }
    @Override
    protected void onDestroy()
    {
        End();
        super.onDestroy();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Create();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Destroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Stop();
    }

    @Override
    public void onBackPressed()
    {
        Escape();
    }

    @android.webkit.JavascriptInterface public void postMessage(int receiver, String id, String command, String info)
    {
        web_view_.post(new Runnable()
        {
            public void run()
            {
                HandleAsync(receiver, id, command, info);
            }
        });
    }

    public void NeedRestart()
    {
        findViewById(android.R.id.content).post(new Runnable()
        {
            public void run()
            {
                Restart();
            }
        });
    }

    public void LoadWebView(int sender, int view_info, String fileName, String waves)
    {
        pixels_ = null;
        image_view_.setVisibility(View.GONE);
        LoadView(web_view_, view_info, sender);
        web_view_.setVisibility(View.VISIBLE);
        web_view_.post(new Runnable()
        {
            public void run()
            { 
                web_view_.setWebViewClient(new WebViewClient()
                {
                    @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request)
                    {
                        Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                        view.getContext().startActivity(intent);
                        return true;
                    }
                    @Override public void onPageFinished(WebView web_view, String url)
                    {
                        super.onPageFinished(web_view, url);
                        web_view.evaluateJavascript(
                            "var Handler = Handler_;" +
                            "var Handler_Receiver = " + sender + ";" +
                            "function CallHandler(id, command, info)" +
                            "{" +
                            "Handler.postMessage(Handler_Receiver, id, command, info);" +
                            "}", null);
                        LoadAudio(waves, new Runnable()
                        {
                            public void run()
                            {
                                HandleAsync(sender, "body", "ready", "");
                            }
                        });
                    }
                });
                web_view_.loadUrl("file:///android_asset/html/" + fileName + ".htm");
            }
        });
    }

    public void LoadImageView(int sender, int view_info, int image_width, String waves)
    {
        web_view_.setWebViewClient(null);
        web_view_.loadUrl("about:blank");
        web_view_.setVisibility(View.GONE);
        LoadView(image_view_, view_info, sender);
        image_view_.setVisibility(View.VISIBLE);
        image_view_.post(new Runnable()
        {
            public void run()
            {
                float scale = (float)image_width / (float)image_view_.getWidth();
                bmp_ = Bitmap.createBitmap(image_width,
                    (int)(image_view_.getHeight() * scale), Bitmap.Config.ARGB_8888);
                pixels_ = new int[bmp_.getWidth() * bmp_.getHeight()];
                LoadAudio(waves, new Runnable()
                {
                    public void run()
                    {
                        HandleAsync(sender, "body", "ready", (int)(getResources().getDisplayMetrics().xdpi * scale) + " " +
                            bmp_.getWidth() + " " + bmp_.getHeight() + " " + 0x02010003);
                    }
                });
            }
        });
    }

    private void LoadView(View v, int view_info, int sender)
    {
        sender_ = sender;
        switch (view_info & 3)
        {
        case 0:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            break;
        case 1:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            break;
        case 2:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            break;
        }
        if ((view_info & 8) != 0)
            close_button_.setVisibility(View.VISIBLE);
        else
            close_button_.setVisibility(View.GONE);
        if ((view_info & 4) != 0)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void RefreshImageView()
    {
        bmp_.setPixels(pixels_, 0, bmp_.getWidth(), 0, 0, bmp_.getWidth(), bmp_.getHeight());
        image_view_.setImageBitmap(bmp_);
    }

    public void CallFunction(String function)
    {
        web_view_.evaluateJavascript(function, null);;
    }

    public String GetAsset(String key)
    {
        String assetValue;
        try
        {
            InputStream stream = getAssets().open(key);
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            assetValue = new String(buffer);

        }
        catch (IOException e)
        {
            assetValue = "";
        }
        return assetValue;
    }

    public String GetPreference(String key)
    {
        return getPreferences(Context.MODE_PRIVATE).getString(key, "");
    }

    public void SetPreference(String key, String value)
    {
        getPreferences(Context.MODE_PRIVATE).edit().putString(key, value).commit();
    }

    public void PostThreadMessage(int sender, String id, String command, String info)
    {
        findViewById(android.R.id.content).post(new Runnable()
        {
            public void run()
            {
                HandleAsync(sender, id, command, info);
            }
        });
    }

    public void AddParam(String key, String value)
    {
        http_params_.put(key, value);
    }

    public void PostHttp(int sender, String id, String command, String url)
    {
        HashMap<String, String> params = http_params_;
        http_params_ = new HashMap<String, String>();
        new AsyncTask<Void, Void, Void>()
        {
            @Override protected Void doInBackground(Void ... args)
            {
                try
                {
                    URL dest = new URL(url);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) dest.openConnection();
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    try
                    {
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.setDoOutput(true);
                        httpURLConnection.setChunkedStreamingMode(0);
                        OutputStream outputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                        for (HashMap.Entry<String, String> entry : params.entrySet())
                        {
                            outputStreamWriter.write(URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8.toString()));
                            outputStreamWriter.write("=");
                            outputStreamWriter.write(URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8.toString()));
                            outputStreamWriter.write("&");
                        }
                        outputStreamWriter.flush();
                        outputStreamWriter.close();
                        InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        String response = new String();
                        if (httpURLConnection.getResponseCode() == 200)
                        {
                            int buffer;
                            while ((buffer = inputStreamReader.read()) != -1)
                            {
                                response += (char)buffer;
                            }
                        }
                        String final_response = response;
                        runOnUiThread(new Runnable()
                        {
                            public void run()
                            {
                                HandleAsync(sender, id, command, final_response);
                            }
                        });
                        inputStreamReader.close();
                    }
                    catch (Exception e)
                    {
                    }
                    finally
                    {
                        httpURLConnection.disconnect();
                    }
                }
                catch (Exception e)
                {
                }
                return null;
            }
        }.execute();
    }

    public void PlayAudio(int index)
    {
        if (tracks_[index] != -1)
            player_.play(tracks_[index], 1.0f, 1.0f, 0, 0, 1.0f);
    }

    public void Exit()
    {
        ReleasePlayer();
        finish();
    }

    public void Escape(View view)
    {
        Escape();
    }

    private void LoadAudio(String waves, Runnable callback)
    {
        ReleasePlayer();
        List<String> tokens = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(waves, " ");
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }
        if (tokens.size() > 0)
        {
            tracks_ = new int[tokens.size()];
            AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            player_ = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(tokens.size())
                .build();
            Runnable after_load = new Runnable()
            {
                int index = 0;
                public void run()
                {
                    if (++index == tokens.size())
                        callback.run();
                }
            };
            player_.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener()
            {
                public void onLoadComplete(SoundPool sp, int sid, int status)
                {
                    after_load.run();
                }
            });
            int index = 0;
            for (String token : tokens)
            {
                try
                {
                    AssetFileDescriptor descriptor = getAssets().openFd("wave/" + token + ".wav");
                    tracks_[index] = player_.load(descriptor, 1);
                }
                catch (IOException e)
                {
                    after_load.run();
                    tracks_[index] = -1;
                }
                ++index;
            }
        }
        else
        {
            callback.run();
        }
    }

    private void ReleasePlayer()
    {
        if (player_ != null)
        {
            player_.release();
            player_ = null;
        }
    }
}
