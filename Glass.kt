package com.forestglass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val LocalRoot = staticCompositionLocalOf { IntSize.Zero }

private const val AGSL = """
uniform shader bg;
uniform float2 org; uniform float2 size; uniform float2 tp;
uniform float depth; uniform float t; uniform float amp;
half4 main(float2 c) {
  float2 p = c - org; float2 q = min(p, size - p); float de = min(q.x, q.y);
  float b = 1.0 - smoothstep(0.0, 28.0, de); b *= b;
  float2 dir = normalize(c - (org + size * 0.5) + 0.001);
  float2 rv = c - tp; float r = length(rv) + 0.001; float2 rd = rv / r;
  float g = exp(-r * r / 7200.0);
  float slope = depth * g * r / 3600.0 + amp * exp(-t * 0.5) * 0.09 * exp(-r / 220.0) * cos(r * 0.09 - t * 6.0);
  float2 off = dir * b * 20.0 - rd * slope * 900.0;
  half4 c0 = bg.eval(c + off); half4 c1 = bg.eval(c + off * 1.07); half4 c2 = bg.eval(c + off * 1.14);
  half3 col = half3(c0.r, c1.g, c2.b);
  half3 f = bg.eval(c + off + float2(6.0, 0.0)).rgb + bg.eval(c + off + float2(-6.0, 0.0)).rgb
          + bg.eval(c + off + float2(0.0, 6.0)).rgb + bg.eval(c + off + float2(0.0, -6.0)).rgb;
  col = mix(col, f * 0.25, 0.4);
  col = mix(col, half3(0.55, 0.75, 0.68), 0.10);
  float3 N = normalize(float3(-rd * slope * 40.0 + dir * b * 0.8, 1.0));
  float3 L = normalize(float3(-0.35, -0.55, 0.75));
  float s = pow(max(dot(reflect(-L, N), float3(0.0, 0.0, 1.0)), 0.0), 70.0);
  float fr = pow(1.0 - N.z, 1.6);
  col += half3(s * 0.8 + fr * 0.35 + b * 0.08);
  col *= half(1.0 - depth * g * 0.18);
  return half4(col, 1.0);
}
"""

@RequiresApi(33)
private fun newShader(): Any = RuntimeShader(AGSL)

@RequiresApi(33)
private fun fx(sh: Any, o: Offset, w: Float, h: Float, tp: Offset, depth: Float, t: Float): androidx.compose.ui.graphics.RenderEffect {
    val s = sh as RuntimeShader
    s.setFloatUniform("org", o.x, o.y)
    s.setFloatUniform("size", w, h)
    s.setFloatUniform("tp", tp.x, tp.y)
    s.setFloatUniform("depth", depth)
    s.setFloatUniform("t", t)
    s.setFloatUniform("amp", 0.12f)
    return RenderEffect.createRuntimeShaderEffect(s, "bg").asComposeRenderEffect()
}

/** Glass surface: refracts the forest behind it; sinks under the finger while held; ripples after release. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val root = LocalRoot.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var org by remember { mutableStateOf(Offset.Zero) }
    var sz by remember { mutableStateOf(IntSize.Zero) }
    var tp by remember { mutableStateOf(Offset.Zero) }
    var down by remember { mutableStateOf(false) }
    val depth by animateFloatAsState(if (down) 1f else 0f, spring(0.55f, Spring.StiffnessLow), label = "depth")
    val time = remember { Animatable(10f) }
    LaunchedEffect(down) { if (down) { time.snapTo(0f); time.animateTo(10f, tween(9000, easing = LinearEasing)) } }
    val shader = remember { if (Build.VERSION.SDK_INT >= 33) newShader() else null }

    Box(
        modifier
            .onGloballyPositioned { org = it.positionInRoot(); sz = it.size }
            .graphicsLayer { scaleX = 1f - 0.045f * depth; scaleY = 1f - 0.075f * depth }
            .clip(shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val d = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    tp = d.position; down = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    do {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        e.changes.firstOrNull()?.let { tp = it.position }
                    } while (e.changes.any { it.pressed })
                    down = false
                }
            }
            .then(
                if (onClick != null) Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
                else Modifier
            )
    ) {
        Box(Modifier.matchParentSize().wrapContentSize(Alignment.TopStart, unbounded = true)) {
            Image(
                painterResource(R.drawable.forest), null,
                Modifier
                    .requiredSize(with(density) { root.width.toDp() }, with(density) { root.height.toDp() })
                    .absoluteOffset { IntOffset(-org.x.roundToInt(), -org.y.roundToInt()) }
                    .graphicsLayer {
                        val sh = shader
                        if (Build.VERSION.SDK_INT >= 33 && sh != null) {
                            renderEffect = fx(sh, org, sz.width.toFloat(), sz.height.toFloat(),
                                Offset(org.x + tp.x, org.y + tp.y), depth, time.value)
                        }
                    },
                contentScale = ContentScale.Crop
            )
        }
        Box(Modifier.matchParentSize().background(Color(0xFF0E2A22).copy(alpha = 0.20f)).border(1.dp, Color.White.copy(alpha = 0.5f), shape))
        content()
    }
}
