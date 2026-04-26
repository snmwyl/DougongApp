package edu.hebut.dougongapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream

class GameFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var server: LocalFileServer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        copyModelsToInternalStorage()

        server = LocalFileServer(requireContext().filesDir.absolutePath + "/models/")
        try { server.start(8080) } catch (e: Exception) { }

        webView = view.findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadDataWithBaseURL(null, getGameHtml(), "text/html", "UTF-8", null)

        view.findViewById<Button>(R.id.btn_assemble_ludou).setOnClickListener {
            webView.evaluateJavascript("showParts('ludou')", null)
        }
        view.findViewById<Button>(R.id.btn_assemble_huagong).setOnClickListener {
            webView.evaluateJavascript("showParts('huagong')", null)
        }
        view.findViewById<Button>(R.id.btn_assemble_ang).setOnClickListener {
            webView.evaluateJavascript("showParts('ang')", null)
        }
        view.findViewById<Button>(R.id.btn_reset).setOnClickListener {
            webView.evaluateJavascript("resetParts()", null)
        }
    }

    private fun copyModelsToInternalStorage() {
        val modelFiles = listOf(
            "ludou_bottom.glb", "ludou_top.glb",
            "huagong_back.glb", "huagong_front.glb",
            "ang_bottom.glb", "ang_top.glb"
        )
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
                    println("复制成功: $fileName")
                }
            } catch (e: Exception) {
                println("复制失败: $fileName - ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }

    private fun getGameHtml(): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>拼装游戏</title>
<style>
    body{margin:0;overflow:hidden;background:#f5f0e8;}
    .guide-tip{
        position:absolute;
        top:16px;
        right:16px;
        background:rgba(0,0,0,0.5);
        padding:6px 12px;
        border-radius:20px;
        font-size:10px;
        color:#C8A45C;
        z-index:20;
        pointer-events:none;
    }
    .status-tip{
        position:absolute;
        bottom:10px;
        left:12px;
        right:12px;
        background:rgba(0,0,0,0.7);
        border-radius:16px;
        padding:10px;
        z-index:20;
        text-align:center;
        font-size:12px;
        color:#C8A45C;
        border-left:3px solid #C8A45C;
        pointer-events:none;
    }
</style>
</head>
<body>
<div class="guide-tip">👆 点击模型拖拽 | 空白处旋转</div>
<div class="status-tip" id="gameStatus">加载模型中...</div>

<script type="importmap">
{
    "imports": {
        "three": "https://unpkg.com/three@0.128.0/build/three.module.js",
        "three/addons/": "https://unpkg.com/three@0.128.0/examples/jsm/"
    }
}
</script>

<script type="module">
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';

// 初始化场景
const scene = new THREE.Scene();
scene.background = new THREE.Color(0xf5f0e8);

// 相机
const camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 1000);
camera.position.set(2.5, 1.8, 3.5);

// 渲染器
const renderer = new THREE.WebGLRenderer({ antialias: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.shadowMap.enabled = true;
document.body.appendChild(renderer.domElement);

// 控制器
const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.target.set(0, 0.5, 0);
controls.enabled = true;

// 灯光
const ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
scene.add(ambientLight);
const mainLight = new THREE.DirectionalLight(0xffffff, 1);
mainLight.position.set(2, 3, 2);
mainLight.castShadow = true;
scene.add(mainLight);
const fillLight = new THREE.PointLight(0xffaa66, 0.5);
fillLight.position.set(1, 1, 2);
scene.add(fillLight);

// 网格
const gridHelper = new THREE.GridHelper(6, 20, 0xccccaa, 0xddddbb);
gridHelper.position.y = -0.8;
scene.add(gridHelper);

// 目标圈圈 - 调整位置贴近模型分半的位置
const targetCircles = {
    // 栌斗：下半部分和上半部分在同一位置
    ludou_bottom: { x: 0, y: -0.15, z: 0, color: 0xB5282C, radius: 0.35 },
    ludou_top: { x: 0, y: 0.15, z: 0, color: 0xB5282C, radius: 0.35 },
    // 华拱：前后两部分位置微调
    huagong_back: { x: 0, y: 0.45, z: -0.25, color: 0xC8A45C, radius: 0.35 },
    huagong_front: { x: 0, y: 0.45, z: 0.25, color: 0xC8A45C, radius: 0.35 },
    // 昂：上下两部分
    ang_bottom: { x: 0, y: 0.85, z: 0, color: 0x4A6A8A, radius: 0.35 },
    ang_top: { x: 0, y: 1.1, z: 0, color: 0x4A6A8A, radius: 0.35 }
};

// 添加圈圈（使用圆环 + 半透明圆盘，更明显）
for (const key in targetCircles) {
    const c = targetCircles[key];
    // 外圈圆环
    const ringGeo = new THREE.RingGeometry(c.radius - 0.05, c.radius, 32);
    const ringMat = new THREE.MeshStandardMaterial({ color: c.color, side: THREE.DoubleSide, emissive: c.color, emissiveIntensity: 0.3 });
    const ring = new THREE.Mesh(ringGeo, ringMat);
    ring.position.set(c.x, c.y, c.z);
    ring.rotation.x = -Math.PI / 2;
    scene.add(ring);
    // 内圈半透明圆盘
    const discGeo = new THREE.CircleGeometry(c.radius - 0.08, 16);
    const discMat = new THREE.MeshStandardMaterial({ color: c.color, transparent: true, opacity: 0.3 });
    const disc = new THREE.Mesh(discGeo, discMat);
    disc.position.set(c.x, c.y + 0.01, c.z);
    disc.rotation.x = -Math.PI / 2;
    scene.add(disc);
}

// 部件
const partList = {
    ludou_bottom: { url: 'ludou_bottom.glb', name: '栌斗底座', group: 'ludou', startX: -1.2, startY: -0.15, startZ: -1, model: null, originalColor: null },
    ludou_top: { url: 'ludou_top.glb', name: '栌斗上部', group: 'ludou', startX: -1.2, startY: 0.15, startZ: -1.2, model: null, originalColor: null },
    huagong_back: { url: 'huagong_back.glb', name: '华拱后部', group: 'huagong', startX: 1.2, startY: 0.45, startZ: 1, model: null, originalColor: null },
    huagong_front: { url: 'huagong_front.glb', name: '华拱前部', group: 'huagong', startX: 1.2, startY: 0.45, startZ: 1.2, model: null, originalColor: null },
    ang_bottom: { url: 'ang_bottom.glb', name: '昂下部', group: 'ang', startX: -1, startY: 0.85, startZ: 1.3, model: null, originalColor: null },
    ang_top: { url: 'ang_top.glb', name: '昂上部', group: 'ang', startX: -1, startY: 1.1, startZ: 1.5, model: null, originalColor: null }
};

let loaded = 0;
const total = Object.keys(partList).length;
let currentSelectedModel = null;

// 高亮模型 - 改变颜色 + 发光
function highlightModel(model, enable) {
    model.traverse(node => {
        if (node.isMesh && node.material) {
            if (enable) {
                // 保存原始颜色
                if (!node.userData.originalColor) {
                    node.userData.originalColor = node.material.color.getHex();
                }
                // 高亮：变成亮橙色，增加自发光
                node.material.color.setHex(0xff6600);
                node.material.emissive = new THREE.Color(0xff4400);
                node.material.emissiveIntensity = 0.5;
            } else {
                // 恢复原始颜色
                if (node.userData.originalColor) {
                    node.material.color.setHex(node.userData.originalColor);
                }
                node.material.emissive = new THREE.Color(0x000000);
                node.material.emissiveIntensity = 0;
            }
        }
    });
}

// 加载所有模型
async function loadAll() {
    const loader = new GLTFLoader();
    for (const key of Object.keys(partList)) {
        const part = partList[key];
        const url = 'http://localhost:8080/' + part.url;
        document.getElementById('gameStatus').innerHTML = '加载 ' + part.name + '...';
        
        await new Promise(resolve => {
            loader.load(url, gltf => {
                const m = gltf.scene;
                m.position.set(part.startX, part.startY, part.startZ);
                const box = new THREE.Box3().setFromObject(m);
                const size = box.getSize();
                const scale = 0.6 / Math.max(size.x, size.y, size.z);
                m.scale.setScalar(scale);
                if (key === 'huagong_front' || key === 'huagong_back') m.rotation.y = Math.PI;
                scene.add(m);
                part.model = m;
                m.visible = false;
                loaded++;
                resolve();
            }, null, err => { console.error(err); resolve(); });
        });
    }
    document.getElementById('gameStatus').innerHTML = '✓ 模型已加载，点击下方按钮开始';
}

// 显示部件
window.showParts = function(group) {
    if (currentSelectedModel) {
        highlightModel(currentSelectedModel, false);
        currentSelectedModel = null;
    }
    for (const key in partList) {
        const p = partList[key];
        if (p.model) p.model.visible = (p.group === group);
    }
    let name = group === 'ludou' ? '栌斗' : (group === 'huagong' ? '华拱' : '昂');
    document.getElementById('gameStatus').innerHTML = '🔧 点击 ' + name + ' 部件拖拽到发光圈圈位置';
};

// 重置
window.resetParts = function() {
    if (currentSelectedModel) {
        highlightModel(currentSelectedModel, false);
        currentSelectedModel = null;
    }
    for (const key in partList) {
        const p = partList[key];
        if (p.model) {
            p.model.position.set(p.startX, p.startY, p.startZ);
            p.model.visible = false;
        }
    }
    document.getElementById('gameStatus').innerHTML = '已重置 | 点击下方按钮选择部件';
};

// ========== 触摸拖拽功能 ==========
let dragModel = null;
let dragging = false;
const raycaster = new THREE.Raycaster();
const touchPoint = new THREE.Vector2();

// 获取触摸点坐标
function getTouchPosition(touch, rect) {
    touchPoint.x = ((touch.clientX - rect.left) / rect.width) * 2 - 1;
    touchPoint.y = -((touch.clientY - rect.top) / rect.height) * 2 + 1;
}

// 开始触摸
renderer.domElement.addEventListener('touchstart', (e) => {
    e.preventDefault();
    const rect = renderer.domElement.getBoundingClientRect();
    const touch = e.touches[0];
    getTouchPosition(touch, rect);
    
    raycaster.setFromCamera(touchPoint, camera);
    const intersects = raycaster.intersectObjects(scene.children, true);
    
    let hitModel = null;
    for (let i = 0; i < intersects.length; i++) {
        let obj = intersects[i].object;
        while (obj.parent !== scene) obj = obj.parent;
        for (const key in partList) {
            if (partList[key].model === obj && obj.visible) {
                hitModel = obj;
                break;
            }
        }
        if (hitModel) break;
    }
    
    if (hitModel) {
        if (currentSelectedModel) highlightModel(currentSelectedModel, false);
        currentSelectedModel = hitModel;
        highlightModel(currentSelectedModel, true);
        dragModel = hitModel;
        dragging = true;
        controls.enabled = false;
        renderer.domElement.style.cursor = 'grabbing';
    } else {
        if (currentSelectedModel) highlightModel(currentSelectedModel, false);
        currentSelectedModel = null;
        dragModel = null;
        dragging = false;
        controls.enabled = true;
        renderer.domElement.style.cursor = 'default';
    }
});

// 移动触摸
renderer.domElement.addEventListener('touchmove', (e) => {
    e.preventDefault();
    if (!dragging || !dragModel) return;
    
    const rect = renderer.domElement.getBoundingClientRect();
    const touch = e.touches[0];
    getTouchPosition(touch, rect);
    
    raycaster.setFromCamera(touchPoint, camera);
    const groundPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), dragModel.position.y);
    const point = new THREE.Vector3();
    if (raycaster.ray.intersectPlane(groundPlane, point)) {
        dragModel.position.x = point.x;
        dragModel.position.z = point.z;
    }
});

// 结束触摸
renderer.domElement.addEventListener('touchend', (e) => {
    e.preventDefault();
    if (dragModel) {
        let partKey = null;
        for (const key in partList) {
            if (partList[key].model === dragModel) {
                partKey = key;
                break;
            }
        }
        if (partKey && targetCircles[partKey]) {
            const target = targetCircles[partKey];
            const distance = dragModel.position.distanceTo(new THREE.Vector3(target.x, target.y, target.z));
            if (distance < target.radius) {
                dragModel.position.set(target.x, target.y, target.z);
                document.getElementById('gameStatus').innerHTML = '✅ ' + partList[partKey].name + ' 已就位！';
                setTimeout(() => {
                    document.getElementById('gameStatus').innerHTML = '点击下方按钮继续拼装';
                }, 1500);
            } else {
                document.getElementById('gameStatus').innerHTML = '⚠️ 没有对准圈圈，再试试看';
                setTimeout(() => {
                    document.getElementById('gameStatus').innerHTML = '继续拖拽 ' + partList[partKey].name + ' 到圈圈';
                }, 1000);
            }
        }
    }
    dragModel = null;
    dragging = false;
    controls.enabled = true;
    renderer.domElement.style.cursor = 'default';
});

// 鼠标事件（电脑调试）
renderer.domElement.addEventListener('mousedown', (e) => {
    const rect = renderer.domElement.getBoundingClientRect();
    touchPoint.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
    touchPoint.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
    
    raycaster.setFromCamera(touchPoint, camera);
    const intersects = raycaster.intersectObjects(scene.children, true);
    
    let hitModel = null;
    for (let i = 0; i < intersects.length; i++) {
        let obj = intersects[i].object;
        while (obj.parent !== scene) obj = obj.parent;
        for (const key in partList) {
            if (partList[key].model === obj && obj.visible) {
                hitModel = obj;
                break;
            }
        }
        if (hitModel) break;
    }
    
    if (hitModel) {
        if (currentSelectedModel) highlightModel(currentSelectedModel, false);
        currentSelectedModel = hitModel;
        highlightModel(currentSelectedModel, true);
        dragModel = hitModel;
        dragging = true;
        controls.enabled = false;
        renderer.domElement.style.cursor = 'grabbing';
    } else {
        if (currentSelectedModel) highlightModel(currentSelectedModel, false);
        currentSelectedModel = null;
        dragModel = null;
        dragging = false;
        controls.enabled = true;
        renderer.domElement.style.cursor = 'default';
    }
});

window.addEventListener('mousemove', (e) => {
    if (!dragging || !dragModel) return;
    const rect = renderer.domElement.getBoundingClientRect();
    touchPoint.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
    touchPoint.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
    raycaster.setFromCamera(touchPoint, camera);
    const groundPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), dragModel.position.y);
    const point = new THREE.Vector3();
    if (raycaster.ray.intersectPlane(groundPlane, point)) {
        dragModel.position.x = point.x;
        dragModel.position.z = point.z;
    }
});

window.addEventListener('mouseup', () => {
    if (dragModel) {
        let partKey = null;
        for (const key in partList) {
            if (partList[key].model === dragModel) {
                partKey = key;
                break;
            }
        }
        if (partKey && targetCircles[partKey]) {
            const target = targetCircles[partKey];
            const distance = dragModel.position.distanceTo(new THREE.Vector3(target.x, target.y, target.z));
            if (distance < target.radius) {
                dragModel.position.set(target.x, target.y, target.z);
                document.getElementById('gameStatus').innerHTML = '✅ ' + partList[partKey].name + ' 已就位！';
                setTimeout(() => {
                    document.getElementById('gameStatus').innerHTML = '点击下方按钮继续拼装';
                }, 1500);
            } else {
                document.getElementById('gameStatus').innerHTML = '⚠️ 没有对准圈圈，再试试看';
            }
        }
    }
    dragModel = null;
    dragging = false;
    controls.enabled = true;
    renderer.domElement.style.cursor = 'default';
});

function animate() {
    requestAnimationFrame(animate);
    controls.update();
    renderer.render(scene, camera);
}
animate();

loadAll();

window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
});
</script>
</body>
</html>
    """.trimIndent()
}