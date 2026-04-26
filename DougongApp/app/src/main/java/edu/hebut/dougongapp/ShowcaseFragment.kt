package edu.hebut.dougongapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream

class ShowcaseFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var server: LocalFileServer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_showcase, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        copyModelsToInternalStorage()

        server = LocalFileServer(requireContext().filesDir.absolutePath + "/models/")
        try { server.start(8080) } catch (e: Exception) { }

        webView = view.findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadDataWithBaseURL(null, getShowcaseHtml(), "text/html", "UTF-8", null)

        view.findViewById<Button>(R.id.btn_ludou).setOnClickListener {
            webView.evaluateJavascript("switchModel('ludou.glb', '栌斗', '位于柱顶，承受上部荷载，是斗拱最基本的承托构件')", null)
        }
        view.findViewById<Button>(R.id.btn_huagong).setOnClickListener {
            webView.evaluateJavascript("switchModel('huagong.glb', '华拱', '向外挑出，承托屋檐，是斗拱中主要的悬挑构件')", null)
        }
        view.findViewById<Button>(R.id.btn_ang).setOnClickListener {
            webView.evaluateJavascript("switchModel('ang.glb', '昂', '斜向构件，利用杠杆原理传递荷载，具有减震作用')", null)
        }
        view.findViewById<Button>(R.id.btn_front).setOnClickListener {
            webView.evaluateJavascript("setView('front')", null)
        }
        view.findViewById<Button>(R.id.btn_side).setOnClickListener {
            webView.evaluateJavascript("setView('side')", null)
        }
        view.findViewById<Button>(R.id.btn_upview).setOnClickListener {
            webView.evaluateJavascript("setView('up')", null)
        }
    }

    private fun copyModelsToInternalStorage() {
        val modelFiles = listOf("ludou.glb", "huagong.glb", "ang.glb")
        val modelDir = File(requireContext().filesDir, "models")
        if (!modelDir.exists()) modelDir.mkdirs()
        for (fileName in modelFiles) {
            try {
                val destFile = File(modelDir, fileName)
                if (!destFile.exists()) {
                    val inputStream = requireContext().assets.open("models/$fileName")
                    val outputStream = FileOutputStream(destFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                }
            } catch (e: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }

    private fun getShowcaseHtml(): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>科普展示</title>
<style>
    body{margin:0;overflow:hidden;background:#f5f0e8;}
    .info-panel{
        position:absolute;
        bottom:15px;
        left:12px;
        right:12px;
        background:rgba(0,0,0,0.75);
        border-radius:16px;
        padding:12px;
        color:#fff;
        z-index:10;
        border-left:4px solid #C8A45C;
    }
    .info-title{font-size:16px;font-weight:bold;color:#C8A45C;margin-bottom:4px;}
    .info-desc{font-size:12px;line-height:1.4;}
    .guide-tip{
        position:absolute;
        top:16px;
        right:16px;
        background:rgba(0,0,0,0.5);
        padding:6px 12px;
        border-radius:20px;
        font-size:11px;
        color:#C8A45C;
        z-index:10;
    }
</style>
</head>
<body>
<div class="info-panel" id="infoPanel">
    <div class="info-title" id="infoTitle">栌斗</div>
    <div class="info-desc" id="infoDesc">位于柱顶，承受上部荷载，是斗拱最基本的承托构件</div>
</div>
<div class="guide-tip">👆 拖拽旋转 | ✌️ 双指缩放</div>

<script type="importmap">
{"imports":{"three":"https://unpkg.com/three@0.128.0/build/three.module.js","three/addons/":"https://unpkg.com/three@0.128.0/examples/jsm/"}}
</script>

<script type="module">
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';

const scene = new THREE.Scene();
scene.background = new THREE.Color(0xf5f0e0);

const camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 1000);
camera.position.set(2.5, 1.5, 2.5);

const renderer = new THREE.WebGLRenderer({ antialias: true });
renderer.setSize(window.innerWidth, window.innerHeight);
document.body.appendChild(renderer.domElement);

const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.target.set(0, 0, 0);

// 灯光
const ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
scene.add(ambientLight);
const mainLight = new THREE.DirectionalLight(0xffffff, 1.2);
mainLight.position.set(2, 3, 2);
scene.add(mainLight);
const fillLight = new THREE.PointLight(0xffaa66, 0.5);
fillLight.position.set(1, 1, 2);
scene.add(fillLight);

const gridHelper = new THREE.GridHelper(6, 20, 0xccccaa, 0xddddbb);
gridHelper.position.y = -0.8;
scene.add(gridHelper);

let currentModel = null;

window.switchModel = function(url, title, desc) {
    if (currentModel) scene.remove(currentModel);
    document.getElementById('infoTitle').innerHTML = title;
    document.getElementById('infoDesc').innerHTML = desc;
    
    const loader = new GLTFLoader();
    loader.load('http://localhost:8080/' + url, (gltf) => {
        currentModel = gltf.scene;
        scene.add(currentModel);
        
        // 自动适配大小
        const box = new THREE.Box3().setFromObject(currentModel);
        const center = box.getCenter(new THREE.Vector3());
        const size = box.getSize(new THREE.Vector3());
        const maxDim = Math.max(size.x, size.y, size.z);
        const scale = 1.6 / maxDim;
        currentModel.scale.setScalar(scale);
        currentModel.position.set(-center.x * scale, -center.y * scale, -center.z * scale);
        
        controls.target.set(0, 0, 0);
        controls.update();
    });
};

window.setView = function(type) {
    if (type === 'front') camera.position.set(2.5, 1.5, 3);
    else if (type === 'side') camera.position.set(3, 1.2, 0);
    else if (type === 'up') camera.position.set(0, 3, 2);
    controls.target.set(0, 0, 0);
    controls.update();
};

function animate() {
    requestAnimationFrame(animate);
    controls.update();
    renderer.render(scene, camera);
}
animate();

window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
});

setTimeout(() => window.switchModel('ludou.glb', '栌斗', '位于柱顶，承受上部荷载，是斗拱最基本的承托构件'), 500);
</script>
</body>
</html>
    """.trimIndent()
}