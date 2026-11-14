package com.forestglass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay
import java.util.Locale

const val EMERGENCY_PIN = "1234" // TODO: let the parent/teacher set it

// Teachers-only content: students can only watch this curated list (later: Firebase, teachers add via a dashboard).
data class LessonItem(val title: String, val youtubeId: String)
val LESSONS = listOf(
    LessonItem("قانون أوم والمقاومة", "VIDEO_ID_1"),
    LessonItem("قانون كيرشوف", "VIDEO_ID_2"),
    LessonItem("المقاومة المكافئة", "VIDEO_ID_3"),
)

@Composable
fun Home(wide: Boolean, go: (Scr) -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("مرحباً بك", color = Color.White.copy(0.85f), fontSize = 15.sp)
        Text("ماذا ندرس اليوم؟", color = Color.White, fontSize = if (wide) 40.sp else 32.sp, fontWeight = FontWeight.Bold)
        GlassCard(Modifier.fillMaxWidth().height(132.dp), onClick = { go(Scr.Lesson) }) {
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text("الفيزياء · الدوائر الكهربائية", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("أكمل من حيث توقفت", color = Color.White.copy(0.8f), fontSize = 13.sp)
            }
        }
        val cols = if (wide) 4 else 2
        listOf("الفيزياء", "الكيمياء", "الأحياء", "الرياضيات").chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { name ->
                    GlassCard(Modifier.weight(1f).height(112.dp), onClick = { go(Scr.Lesson) }) {
                        Text(name, Modifier.align(Alignment.BottomStart).padding(18.dp), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        GlassCard(Modifier.fillMaxWidth().height(68.dp).padding(bottom = 0.dp), RoundedCornerShape(34.dp)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                listOf("الرئيسية", "الدروس", "تقدّمي", "حسابي").forEach { Text(it, color = Color.White, fontSize = 14.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun YouTubeView(id: String) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
        YouTubePlayerView(ctx).apply {
            enableAutomaticInitialization = false
            lifecycle.addObserver(this)
            initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) { youTubePlayer.cueVideo(id, 0f) }
            }, IFramePlayerOptions.Builder().controls(1).rel(0).ivLoadPolicy(3).build())
        }
    })
}

@Composable
fun Lesson(back: () -> Unit, focus: () -> Unit) {
    var sel by remember { mutableStateOf(LESSONS.first()) }
    Column(Modifier.fillMaxSize().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(Modifier.size(44.dp), CircleShape, onClick = back) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", Modifier.align(Alignment.Center), tint = Color.White)
        }
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(28.dp)).background(Color.Black)) {
            key(sel.youtubeId) { YouTubeView(sel.youtubeId) }
        }
        Text(sel.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("[اسم المدرس]", color = Color.White.copy(0.85f), fontSize = 14.sp)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp)) {
                LESSONS.forEach { l ->
                    Text(l.title, Modifier.fillMaxWidth().clickable { sel = l }.padding(14.dp), color = Color.White, fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        GlassCard(Modifier.fillMaxWidth().height(58.dp), RoundedCornerShape(29.dp), onClick = focus) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Lock, null, tint = Color.White)
                Text("ابدأ وضع التركيز", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun Focus(onExit: () -> Unit) {
    var secs by remember { mutableIntStateOf(25 * 60) }
    var askPin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (secs > 0) { delay(1000); secs-- }; onExit() }
    BackHandler { }  // back is blocked during focus
    Column(Modifier.fillMaxSize().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(Modifier.size(120.dp), CircleShape) {
            Icon(Icons.Filled.Lock, null, Modifier.align(Alignment.Center).size(46.dp), tint = Color.White)
        }
        Spacer(Modifier.height(36.dp))
        Text("وضع التركيز", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("ركّز على الدرس. التطبيق مقفل حتى نهاية الجلسة.", color = Color.White.copy(0.85f), fontSize = 16.sp)
        Spacer(Modifier.height(28.dp))
        GlassCard(Modifier.wrapContentSize(), RoundedCornerShape(36.dp)) {
            Text(String.format(Locale("ar"), "%02d:%02d", secs / 60, secs % 60), Modifier.padding(horizontal = 34.dp, vertical = 12.dp), color = Color.White, fontSize = 60.sp)
        }
        Spacer(Modifier.weight(1f))
        GlassCard(Modifier.wrapContentSize(), RoundedCornerShape(27.dp), onClick = { askPin = true }) {
            Text("حالة طارئة؟ أدخل رمز الخروج", Modifier.padding(horizontal = 28.dp, vertical = 16.dp), color = Color.White, fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
    if (askPin) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askPin = false },
            title = { Text("رمز الخروج") },
            text = { OutlinedTextField(pin, { pin = it.take(4) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)) },
            confirmButton = { TextButton({ if (pin == EMERGENCY_PIN) onExit() else pin = "" }) { Text("خروج") } },
            dismissButton = { TextButton({ askPin = false }) { Text("إلغاء") } }
        )
    }
}
