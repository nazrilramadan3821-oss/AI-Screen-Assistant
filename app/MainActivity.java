package com.nazril.asisten;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
  private WebView w;
  private ValueCallback<Uri[]> cb;

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    w = new WebView(this);
    setContentView(w);
    WebSettings s = w.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
      .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
      .build();
    w.setWebViewClient(new WebViewClient() {
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
        return loader.shouldInterceptRequest(r.getUrl());
      }
    });
    w.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> c, FileChooserParams p) {
        if (cb != null) cb.onReceiveValue(null);
        cb = c;
        try {
          startActivityForResult(p.createIntent(), 1);
        } catch (Exception e) {
          cb = null;
          return false;
        }
        return true;
      }
    });
    w.loadUrl("https://appassets.androidplatform.net/assets/index.html");
  }

  @Override
  protected void onActivityResult(int rc, int res, Intent d) {
    if (rc == 1 && cb != null) {
      cb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, d));
      cb = null;
    }
  }

  @Override
  public void onBackPressed() {
    if (w.canGoBack()) w.goBack(); else super.onBackPressed();
  }
        }
