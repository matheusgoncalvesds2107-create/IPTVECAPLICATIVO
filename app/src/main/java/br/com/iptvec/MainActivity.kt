package br.com.iptvec

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import java.io.*
import java.net.URL
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.regex.Pattern

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val exec=Executors.newSingleThreadExecutor()
    private val picker=1001
    private lateinit var db:DB

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        db=DB(this)
        web=WebView(this)
        web.settings.javaScriptEnabled=true
        web.settings.domStorageEnabled=true
        web.settings.mediaPlaybackRequiresUserGesture=false
        web.settings.allowFileAccess=true
        web.settings.allowContentAccess=true
        web.webChromeClient=WebChromeClient()
        web.webViewClient=WebViewClient()
        web.addJavascriptInterface(Bridge(),"Android")
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
    }
    inner class Bridge {
        @JavascriptInterface fun chooseFile(){runOnUiThread{
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                type="*/*"; addCategory(Intent.CATEGORY_OPENABLE)
            },picker)
        }}
        @JavascriptInterface fun loadUrl(url:String){parseSource{URL(url).openStream()}}
        @JavascriptInterface fun page(group:String,q:String,offset:Int,limit:Int){
            exec.execute{
                val rows=db.page(group,q,offset,limit)
                val json=rows.joinToString(","){"{\"name\":${q(it[0])},\"logo\":${q(it[1])},\"url\":${q(it[2])}}"}
                runOnUiThread{web.evaluateJavascript("showItems(${q(group)},[$json],$offset)",null)}
            }
        }
        @JavascriptInterface fun status(msg:String){runOnUiThread{Toast.makeText(this@MainActivity,msg,Toast.LENGTH_SHORT).show()}}
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
                val cats=db.categories()
                val cj=cats.joinToString(","){"{\"name\":${q(it.first)},\"count\":${it.second}}"}
                runOnUiThread{web.evaluateJavascript("finishLoad(${count},[$cj])",null)}
            }catch(e:Exception){runOnUiThread{web.evaluateJavascript("loadError(${q(e.message?:"Erro ao carregar lista")})",null)}}
        }
    }
    private fun attr(line:String,key:String):String{
        val m=Pattern.compile("""$key\s*=\s*["']([^"']*)["']""",Pattern.CASE_INSENSITIVE).matcher(line)
        return if(m.find())m.group(1)?:"" else ""
    }
    private fun q(s:String)= "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ")+"\""
}
