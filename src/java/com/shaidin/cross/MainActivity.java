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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.StringTokenizer;

public class MainActivity extends Activity
{
    WebView web_view_;
    int sender_;
    HashMap<String, String> http_params_ = new HashMap<String, String>();

    static {System.loadLibrary("native");}
    public native void Setup();
    public native void Begin();
    public native void End();
    public native void Create();
    public native void Destroy();
    public native void Start();
    public native void Stop();
    public native void Restart();
    public native byte[] FeedUri(String uri);
    public native void Escape();
    public native void Handle(String id, String command, String info);
    public native void HandleAsync(
        int sender, String id, String command, String info);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        web_view_ = findViewById(R.id.webView);
        web_view_.getSettings().setJavaScriptEnabled(true);
        web_view_.getSettings().setAllowUniversalAccessFromFileURLs(true);
        web_view_.addJavascriptInterface(this, "Handler_");
        http_params_ = new HashMap<String, String>();
        Setup();
        Begin();
    }

    @Override protected void onDestroy()
    {
        End();
        super.onDestroy();
    }

    @Override protected void onStart()
    {
        super.onStart();
        Create();
    }

    @Override protected void onStop()
    {
        super.onStop();
        Destroy();
    }

    @Override protected void onResume()
    {
        super.onResume();
        Start();
    }

    @Override protected void onPause()
    {
        super.onPause();
        Stop();
    }

    @Override public void onBackPressed()
    {
        Escape();
    }

    @android.webkit.JavascriptInterface public void postMessage(
        int receiver, String id, String command, String info)
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

    public void LoadView(int sender, int view_info, String fileName)
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
        if ((view_info & 4) != 0)
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web_view_.post(new Runnable()
        {
            public void run()
            { 
                web_view_.setWebViewClient(new WebViewClient()
                {
                    @Override public boolean shouldOverrideUrlLoading(
                        WebView view, WebResourceRequest request)
                    {
                        return true;
                    }
                    @Override public void onPageFinished(
                        WebView web_view, String url)
                    {
                        super.onPageFinished(web_view, url);
                        web_view.evaluateJavascript(
                            "var Handler = Handler_;" +
                            "var Handler_Receiver = " + sender + ";" +
                            "function CallHandler(id, command, info)" +
                            "{" +
                            "Handler.postMessage(" +
                            "Handler_Receiver, id, command, info);" +
                            "}" +
                            "var cross_asset_domain_ = 'asset://';" +
                            "var cross_asset_async_ = true;" +
                            "var cross_pointer_type_ = 'touch';" +
                            "var cross_pointer_upsidedown_ = false;"
                            , null);
                        HandleAsync(sender, "body", "ready", "");
                    }
                    @Override public WebResourceResponse shouldInterceptRequest(
                        WebView view, WebResourceRequest request)
                    {
                        String url = request.getUrl().toString();
                        if (url.startsWith("cross://"))
                        {
                            byte[] data = FeedUri(url);
                            if (data != null && data.length > 0)
                            {
                                return new WebResourceResponse("", "",
                                    new ByteArrayInputStream(data));
                            }
                        }
                        else if (url.startsWith("asset://"))
                        {
                            try
                            {
                                String file = url.replace("asset://", "");
                                InputStream input = getAssets().open(file);
                                int size = input.available();
                                byte[] data = new byte[size];
                                input.read(data);
                                input.close();
                                return new WebResourceResponse("", "",
                                    new ByteArrayInputStream(data));
                            }
                            catch (IOException e)
                            {
                            }
                        }
                        return super.shouldInterceptRequest(view, request);
                    }
                });
                web_view_.loadUrl("file:///android_asset/" + fileName + ".htm");
            }
        });
    }

    public void CallFunction(String function)
    {
        web_view_.evaluateJavascript(function, null);
    }

    public String GetPreference(String key)
    {
        return getPreferences(Context.MODE_PRIVATE).getString(key, "");
    }

    public void SetPreference(String key, String value)
    {
        getPreferences(
            Context.MODE_PRIVATE).edit().putString(key, value).commit();
    }

    public void AsyncMessage(int sender, String id, String command, String info)
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
                    HttpURLConnection httpURLConnection =
                        (HttpURLConnection) dest.openConnection();
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setRequestProperty(
                        "Content-Type", "application/x-www-form-urlencoded");
                    try
                    {
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.setDoOutput(true);
                        httpURLConnection.setChunkedStreamingMode(0);
                        OutputStream outputStream = new BufferedOutputStream(
                            httpURLConnection.getOutputStream());
                        OutputStreamWriter outputStreamWriter =
                            new OutputStreamWriter(outputStream);
                        for (HashMap.Entry<String, String> entry :
                            params.entrySet())
                        {
                            outputStreamWriter.write(URLEncoder.encode(
                                entry.getKey(),
                                java.nio.charset.StandardCharsets.UTF_8.toString
                                ()));
                            outputStreamWriter.write("=");
                            outputStreamWriter.write(URLEncoder.encode(
                                entry.getValue(),
                                java.nio.charset.StandardCharsets.UTF_8.toString
                                ()));
                            outputStreamWriter.write("&");
                        }
                        outputStreamWriter.flush();
                        outputStreamWriter.close();
                        InputStream inputStream = new BufferedInputStream(
                            httpURLConnection.getInputStream());
                        InputStreamReader inputStreamReader =
                            new InputStreamReader(inputStream);
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
                                HandleAsync(
                                    sender, id, command, final_response);
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

    public void CreateImage(String id, String parent)
    {
        web_view_.evaluateJavascript(
            "var img = document.createElement('img');" +
            "img.setAttribute('id', '" + id + "');" +
            "document.getElementById('" + parent +
            "').appendChild(img);"
            , null);
    }

    public void ResetImage(int sender, int index, String id)
    {
        web_view_.evaluateJavascript(
            "resetImage(" + sender + "," + index + ",'" + id + "')", null);
    }

    public void Exit()
    {
        finish();
    }

    public void Escape(View view)
    {
        Escape();
    }

}
