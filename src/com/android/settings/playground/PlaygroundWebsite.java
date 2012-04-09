package com.android.settings.playground;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.settings.R;

public class PlaygroundWebsite extends Activity {

	private static final int MENU_BACKFORD = 0;
	private static final int MENU_RELOAD = 2;
	private static final int MENU_FORWARD = 1;
	String userPass = "";
	String userName = "";
	WebView mWebView;
	boolean validated = false;
	String vDownload;
	int numDown = 0;

	public String spot;
	Bitmap bm;
	public int viewer;
	SharedPreferences mPrefs;

	String subject;
	String android_id;
	String address;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		setContentView(R.layout.playground_webview);

		firstRunPreferences();

		android_id = Secure.getString(this.getContentResolver(),
				Secure.ANDROID_ID);

		mWebView = (WebView) findViewById(R.id.webview);
		if (savedInstanceState != null)
			((WebView) findViewById(R.id.webview))
					.restoreState(savedInstanceState);
		mWebView.getSettings().setBuiltInZoomControls(true);
		// mWebView.getSettings().setLayoutAlgorithm(
		// LayoutAlgorithm.NARROW_COLUMNS);
		mWebView.setInitialScale(0);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setPluginsEnabled(true);
		mWebView.getSettings().setAllowFileAccess(true);
		mWebView.loadUrl("http://twistedumbrella.github.com/Twisted-Playground");
		mWebView.setWebViewClient(new HelloWebViewClient());

	}

	private class HelloWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {
			try {
				String username = mPrefs.getString("userName", null);
				String password = mPrefs.getString("passWord", null);
				handler.proceed(username, password);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onLoadResource(WebView view, String url) {

			if (url.endsWith(".mp4") || url.endsWith(".3gp")) {
				WebView webView = (WebView) findViewById(R.id.webview);
				webView.setDownloadListener(new DownloadListener() {
					@Override
					public void onDownloadStart(String url, String userAgent,
							String contentDisposition, String mimeType,
							long size) {
						Intent viewIntent = new Intent(Intent.ACTION_VIEW);
						viewIntent.setDataAndType(Uri.parse(url), mimeType);

						try {
							startActivity(viewIntent);
						} catch (ActivityNotFoundException ex) {
							Log.w("YourLogTag",
									"Couldn't find activity to view mimetype: "
											+ mimeType);
						}
					}
				});

			}

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_BACKFORD, Menu.NONE, R.string.str_Backward)
				.setIcon(android.R.drawable.ic_media_rew);
		menu.add(Menu.NONE, MENU_RELOAD, Menu.NONE, "Reload").setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(Menu.NONE, MENU_FORWARD, Menu.NONE, R.string.str_Forward)
				.setIcon(android.R.drawable.ic_media_ff);

		return super.onCreateOptionsMenu(menu);
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {

		case MENU_BACKFORD:
			if (mWebView.canGoBack())
				mWebView.goBack();
			break;
		case MENU_RELOAD:
			mWebView.reload();
			break;
		case MENU_FORWARD:
			if (mWebView.canGoForward())
				mWebView.goForward();
			break;
		}
		return true;

	}

	private void userRequest() {
		final Intent emailIntent = new Intent(
				android.content.Intent.ACTION_SEND);
		emailIntent.setType("plain/text");
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
				new String[] { "TwistedUmbrella@gmail.com" });
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		startActivity(Intent.createChooser(emailIntent, "Contact Developer: "));
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
			mWebView.goBack();
			return true;
		} else if ((keyCode == KeyEvent.KEYCODE_BACK)
				&& mWebView.canGoBack() == false) {
		    mWebView.clearHistory();
		    mWebView.clearFormData();
		    mWebView.clearCache(true);
			super.onDestroy();
			finish();
		} else if (keyCode == KeyEvent.KEYCODE_HOME) {
		    mWebView.clearHistory();
		    mWebView.clearFormData();
		    mWebView.clearCache(true);
			super.onDestroy();
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onStop() {
		super.onStop();

	}

	// Fires after the OnStop() state
	@Override
	protected void onDestroy() {
		mWebView.clearCache(true);
		super.onDestroy();

		try {
			trimCache(this);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}

	public static void trimCache(Context context) {
		try {

			File dir = context.getCacheDir();
			if (dir != null && dir.isDirectory()) {
				deleteDir(dir);

			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}

}
