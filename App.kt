package com.forestglass

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection

enum class Scr { Home, Lesson, Focus }

@Composable
fun App(lock: () -> Unit, unlock: () -> Unit) {
    var scr by remember { mutableStateOf(Scr.Home) }
    fun go(s: Scr) { if (s == Scr.Focus) lock() else if (scr == Scr.Focus) unlock(); scr = s }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF16301F))) {
            CompositionLocalProvider(LocalRoot provides IntSize(constraints.maxWidth, constraints.maxHeight)) {
                Image(painterResource(R.drawable.forest), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                val wide = maxWidth >= 600.dp   // tablets and landscape
                Crossfade(scr, animationSpec = tween(600), label = "screen") { s ->
                    Box(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = 860.dp).fillMaxHeight()) {
                            when (s) {
                                Scr.Home -> Home(wide) { go(it) }
                                Scr.Lesson -> Lesson({ go(Scr.Home) }, { go(Scr.Focus) })
                                Scr.Focus -> Focus { go(Scr.Home) }
                            }
                        }
                    }
                }
            }
        }
    }
}
