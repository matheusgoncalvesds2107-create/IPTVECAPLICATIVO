package com.iptvec.mobile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val executor = Executors.newSingleThreadExecutor()

    private fun telaCheia() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        telaCheia()

        webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = false

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // Lista M3U integrada: carrega automaticamente ao abrir o IPTVEC.
                val listaIntegrada = "https://demo.nxsplus.xyz/get.php?username=27810127&password=69956901&type=m3u"
                val js = "if(window.Android && Android.loadM3U){Android.loadM3U(" +
                    JSONObject.quote(listaIntegrada) + ");}"
                view.evaluateJavascript(js, null)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf(
                            "application/octet-stream",
                            "text/plain",
                            "application/x-mpegurl",
                            "audio/x-mpegurl"
                        )
                    )
                }

                startActivityForResult(intent, FILE_PICKER_REQUEST)
                return true
            }
        }

        webView.addJavascriptInterface(M3UBridge(), "Android")
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class M3UBridge {
        @JavascriptInterface
        fun loadM3U(urlString: String) {
            executor.execute {
                try {
                    val url = URL(urlString)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 20000
                        readTimeout = 30000
                        instanceFollowRedirects = true
                        setRequestProperty(
                            "User-Agent",
                            "IPTVEC/1.1 Android WebView"
                        )
                        setRequestProperty("Accept", "*/*")
                    }

                    val code = connection.responseCode
                    if (code !in 200..399) {
                        throw Exception("Servidor respondeu HTTP $code")
                    }

                    val input = if (code >= 400) {
                        connection.errorStream
                    } else {
                        connection.inputStream
                    }

                    val text = BufferedReader(
                        InputStreamReader(input, Charsets.UTF_8)
                    ).use { it.readText() }

                    connection.disconnect()

                    if (text.isBlank()) {
                        throw Exception("A resposta veio vazia")
                    }

                    val jsText = JSONObject.quote(text)
                    runOnUiThread {
                        webView.evaluateJavascript(
                            "window.onNativeM3USuccess($jsText);",
                            null
                        )
                    }
                } catch (e: Exception) {
                    val message = JSONObject.quote(
                        e.message ?: "erro desconhecido"
                    )
                    runOnUiThread {
                        webView.evaluateJavascript(
                            "window.onNativeM3UError($message);",
                            null
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) telaCheia()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == FILE_PICKER_REQUEST) {
            val results = if (resultCode == RESULT_OK && data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        webView.removeJavascriptInterface("Android")
        webView.destroy()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        private const val FILE_PICKER_REQUEST = 7001
    }
}
