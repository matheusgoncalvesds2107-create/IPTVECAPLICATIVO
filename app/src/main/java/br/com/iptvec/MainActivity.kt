package br.com.iptvec

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.*
import java.net.URL
import java.net.HttpURLConnection
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.regex.Pattern

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var playerView: PlayerView
    private lateinit var player: ExoPlayer
    private var playerVisible = false
    private val exec=Executors.newSingleThreadExecutor()
    private val picker=1001
    private lateinit var db:DB

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        db=DB(this)

        player=ExoPlayer.Builder(this).build()
        playerView=PlayerView(this).apply {
            this.player=player
            useController=true
            layoutParams=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220))
            setBackgroundColor(0xFF080808.toInt())
            useArtwork = true
        }

        web=WebView(this)
        web.settings.javaScriptEnabled=true
        web.settings.domStorageEnabled=true
        web.settings.mediaPlaybackRequiresUserGesture=false
        web.settings.allowFileAccess=true
        web.settings.allowContentAccess=true
        web.settings.mixedContentMode=WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        web.settings.allowUniversalAccessFromFileURLs=true
        web.settings.allowFileAccessFromFileURLs=true
        web.webChromeClient=WebChromeClient()
        web.webViewClient=WebViewClient()
        web.addJavascriptInterface(Bridge(),"Android")
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.loadUrl("file:///android_asset/index.html")

        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFF050505.toInt()) }
        playerView.visibility = android.view.View.GONE
        root.addView(playerView)
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        setContentView(root)
    }

    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()

    inner class Bridge {
        @JavascriptInterface fun chooseFile(){runOnUiThread{
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                type="*/*"; addCategory(Intent.CATEGORY_OPENABLE)
            },picker)
        }}
        @JavascriptInterface fun loadUrl(url:String){parseSource{URL(url).openStream()}}
        @JavascriptInterface fun play(url:String,name:String,logo:String){
            runOnUiThread {
                try {
                    playerVisible = true
                    playerView.visibility = android.view.View.VISIBLE
                    player.stop()
                    player.clearMediaItems()
                    val lower=url.lowercase()
                    val builder=MediaItem.Builder().setUri(url)
                    if(lower.contains(".m3u8") || lower.contains("m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                    if (logo.isNotBlank()) loadArtwork(logo)
                    player.setMediaItem(builder.build())
                    player.prepare()
                    player.playWhenReady=true
                    web.evaluateJavascript("setPoster(${q(name)},${q(logo)},${q("Reproduzindo / carregando...")})",null)
                } catch(e:Exception) {
                    Toast.makeText(this@MainActivity,"Erro ao abrir canal: ${e.message}",Toast.LENGTH_LONG).show()
                }
            }
        }
        @JavascriptInterface fun page(group:String,qry:String,offset:Int,limit:Int){
            exec.execute{
                try {
                    val rows=db.page(group,qry,offset,limit)
                    val json=rows.joinToString(","){"{\"name\":${q(it[0])},\"logo\":${q(it[1])},\"url\":${q(it[2])}}"}
                    runOnUiThread{web.evaluateJavascript("showItems(${q(group)},[$json],$offset)",null)}
                } catch(e:Exception) {
                    runOnUiThread{web.evaluateJavascript("pageError(${q(group)},${q(e.message ?: "erro")})",null)}
                }
            }
        }
        @JavascriptInterface fun loadImage(url:String, token:String){
            if(url.isBlank()) return
            exec.execute {
                var conn: HttpURLConnection? = null
                try {
                    conn=URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout=12000
                    conn.readTimeout=15000
                    conn.instanceFollowRedirects=true
                    conn.useCaches=true
                    conn.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
                    conn.setRequestProperty("Accept","image/avif,image/webp,image/apng,image/svg+xml,image/png,image/jpeg,*/*;q=0.8")
                    conn.setRequestProperty("Referer","https://www.google.com/")
                    val code=conn.responseCode
                    if(code in 200..299){
                        val bytes=conn.inputStream.use{it.readBytes()}
                        if(bytes.isNotEmpty() && bytes.size <= 2*1024*1024){
                            val rawType=conn.contentType?.substringBefore(';')?.lowercase()?.trim()
                            val mime=when {
                                rawType?.startsWith("image/")==true -> rawType
                                url.lowercase().contains(".svg") -> "image/svg+xml"
                                url.lowercase().contains(".webp") -> "image/webp"
                                url.lowercase().contains(".jpg") || url.lowercase().contains(".jpeg") -> "image/jpeg"
                                else -> "image/png"
                            }
                            val b64=android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)
                            val data="data:$mime;base64,$b64"
                            runOnUiThread{web.evaluateJavascript("setLogo(${q(token)},${q(data)})",null)}
                        }
                    }
                }catch(e:Exception){
                    // O JavaScript mantém o fallback com as iniciais do canal.
                }finally{ conn?.disconnect() }
            }
        }

        @JavascriptInterface fun status(msg:String){runOnUiThread{Toast.makeText(this@MainActivity,msg,Toast.LENGTH_SHORT).show()}}
    }

    private fun loadArtwork(url:String){
        exec.execute {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout=10000; conn.readTimeout=12000; conn.instanceFollowRedirects=true
                conn.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
                if(conn.responseCode in 200..299){
                    val bytes=conn.inputStream.use{it.readBytes()}
                    val bmp=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size)
                    if(bmp!=null) runOnUiThread { playerView.defaultArtwork=android.graphics.drawable.BitmapDrawable(resources, bmp) }
                }
                conn.disconnect()
            }catch(_:Exception){}
        }
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==picker&&resultCode==RESULT_OK){
            data?.data?.let{uri->contentResolver.openInputStream(uri)?.let{input->parseSource{input}}}
        }
    }

    private fun parseSource(open:()->InputStream){
        exec.execute{
            try{
                runOnUiThread{web.evaluateJavascript("startProgress()",null)}
                val input=open()
                db.clear(); db.begin()
                BufferedReader(InputStreamReader(input,Charset.forName("UTF-8")),65536).use{br->
                    var current:String?=null; var group="OUTROS"; var logo=""; var count=0
                    while(true){
                        val raw=br.readLine()?:break
                        val line=raw.trim()
                        if(line.isEmpty())continue
                        if(line.startsWith("#EXTINF:",true)){
                            current=line.substringAfterLast(",").trim().ifEmpty{"Sem nome"}
                            group=attr(line,"group-title").ifEmpty{"OUTROS"}
                            logo=attr(line,"tvg-logo")
                        }else if(!line.startsWith("#")&&current!=null){
                            db.insert(current!!,group,logo,line); count++; current=null
                            if(count%2000==0){db.commit();db.begin();val n=count;runOnUiThread{web.evaluateJavascript("setProgress($n)",null)}}
                        }
                    }
                }
                db.commit();input.close()
                val totalCount=db.totalItems()
                val cats=db.categories()
                val cj=cats.joinToString(","){"{\"name\":${q(it.first)},\"count\":${it.second}}"}
                runOnUiThread{web.evaluateJavascript("finishLoad(${totalCount},[$cj])",null)}
            }catch(e:Exception){runOnUiThread{web.evaluateJavascript("loadError(${q(e.message?:"Erro ao carregar lista")})",null)}}
        }
    }
    private fun attr(line:String,key:String):String{
        val m=Pattern.compile("""$key\s*=\s*["']([^"']*)["']""",Pattern.CASE_INSENSITIVE).matcher(line)
        return if(m.find())m.group(1)?:("") else ""
    }
    private fun q(s:String)="\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ")+"\""

    override fun onDestroy(){
        player.release()
        exec.shutdownNow()
        super.onDestroy()
    }
}
