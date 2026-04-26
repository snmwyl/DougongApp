package edu.hebut.dougongapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var thumbnail: ImageView
    private lateinit var videoContainer: FrameLayout
    private lateinit var playButton: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.video_webview)
        thumbnail = view.findViewById(R.id.video_thumbnail)
        videoContainer = view.findViewById(R.id.video_container)
        playButton = view.findViewById(R.id.btn_play_video)

        // 配置 WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // 点击播放按钮：隐藏缩略图，加载并显示视频
        playButton.setOnClickListener {
            thumbnail.visibility = View.GONE
            webView.visibility = View.VISIBLE

            // B站嵌入视频地址（无弹幕，高清）
            val videoUrl = "https://player.bilibili.com/player.html?bvid=BV1my4y1K7oC&page=1&high_quality=1&danmaku=0"
            webView.loadUrl(videoUrl)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 释放 WebView 资源
        webView.loadUrl("about:blank")
        webView.destroy()
    }
}