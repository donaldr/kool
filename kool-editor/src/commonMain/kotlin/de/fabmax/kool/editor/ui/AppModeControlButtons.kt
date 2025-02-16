package de.fabmax.kool.editor.ui

import de.fabmax.kool.editor.api.AppMode
import de.fabmax.kool.editor.api.AppState
import de.fabmax.kool.math.Easing
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.toRad
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.ColorGradient
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.MutableColor
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

fun UiScope.appModeControlButtons() {
    val btnSzCenter = sizes.baseSize * 1.3f
    val btnSzOuter = sizes.baseSize * 1.1f
    val btnExtent = btnSzOuter * 0.85f

    Box(width = btnExtent * 2 + btnSzOuter, height = btnSzCenter) {
        val playStopAnimator = remember {
            AnimatedFloatBidir(0.2f).apply {
                AppState.appModeState.onChange {
                    if (it == AppMode.EDIT) {
                        start(0f)
                    } else if (it == AppMode.PLAY) {
                        start(1f)
                    }
                }
            }
        }
        val playPauseAnimator = remember {
            AnimatedFloatBidir(0.2f).apply {
                AppState.appModeState.onChange {
                    if (it == AppMode.PLAY) {
                        start(0f)
                    } else if (it == AppMode.PAUSE) {
                        start(1f)
                    }
                }
            }
        }

        val playButtonBg = remember { PlayButtonBg(playStopAnimator) }
        val resetButtonBg = remember { ResetButtonBg(playStopAnimator) }
        val pauseButtonBg = remember { PauseButtonBg(playStopAnimator, playPauseAnimator) }

        modifier
            .margin(top = sizes.smallGap * -0.5f)
            .alignX(AlignmentX.Center)

        val p = 1f - Easing.quadRev(playStopAnimator.progressAndUse())
        Button {
            modifier
                .size(btnSzOuter, btnSzOuter)
                .alignY(AlignmentY.Center)
                .margin(start = btnExtent * p)
                .isClickFeedback(false)
                .background(resetButtonBg)
                .onEnter { resetButtonBg.hoverAnimator.start(1f) }
                .onExit { resetButtonBg.hoverAnimator.start(0f) }
                .onClick {
                    resetButtonBg.clickAnimator.start()
                }
                .border(CircularBorder(colors.background, 2.dp))
        }
        Button {
            modifier
                .size(btnSzOuter, btnSzOuter)
                .alignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .margin(end = btnExtent * p)
                .isClickFeedback(false)
                .background(pauseButtonBg)
                .onEnter { pauseButtonBg.hoverAnimator.start(1f) }
                .onExit { pauseButtonBg.hoverAnimator.start(0f) }
                .onClick {
                    pauseButtonBg.clickAnimator.start()
                    if (AppState.appMode == AppMode.PLAY) {
                        AppState.appModeState.set(AppMode.PAUSE)
                    } else if (AppState.appMode == AppMode.PAUSE) {
                        AppState.appModeState.set(AppMode.PLAY)
                    }
                }
                .border(CircularBorder(colors.background, 2.dp))
        }
        Button {
            modifier
                .size(btnSzCenter, btnSzCenter)
                .align(AlignmentX.Center, AlignmentY.Center)
                .isClickFeedback(false)
                .background(playButtonBg)
                .onEnter { playButtonBg.hoverAnimator.start(1f) }
                .onExit { playButtonBg.hoverAnimator.start(0f) }
                .onClick {
                    playButtonBg.clickAnimator.start()
                    if (AppState.appMode == AppMode.EDIT) {
                        AppState.appModeState.set(AppMode.PLAY)
                    } else {
                        AppState.appModeState.set(AppMode.EDIT)
                    }
                }
                .border(CircularBorder(colors.background, 2.dp))
        }
    }
}

private open class ModeButtonBg(val playStopAnimator: AnimatedFloatBidir, val accent: Color) : UiRenderer<UiNode> {
    var fgColor = ColorGradient(
        EditorUi.EDITOR_THEME_COLORS.onBackgroundAlpha(0.5f),
        EditorUi.EDITOR_THEME_COLORS.onBackground.mix(accent, 0.9f)
    )
    var bgColor = ColorGradient(
        UiColors.titleBg.mix(EditorUi.EDITOR_THEME_COLORS.secondaryVariant, 0.5f),
        EditorUi.EDITOR_THEME_COLORS.secondaryVariant.mix(accent, 0.1f)
    )
    val hoverAnimator = AnimatedFloatBidir(0.15f, 0.3f)
    val clickAnimator = AnimatedFloat(0.15f)

    open fun getBgColor(pHover: Float) = bgColor.getColor(pHover)

    override fun renderUi(node: UiNode) = node.run {
        val r = widthPx * 0.5f
        val pHover = Easing.smooth(hoverAnimator.progressAndUse())
        val pClick = clickAnimator.progressAndUse()

        getUiPrimitives().localCircle(widthPx * 0.5f, heightPx * 0.5f, r, getBgColor(pHover))
        buttonFg(pHover)
        if (clickAnimator.isActive) {
            getUiPrimitives().localCircle(
                widthPx * 0.5f, heightPx * 0.5f, r, Color.WHITE.withAlpha(0.7f - pClick * 0.5f)
            )
        }
    }

    open fun UiNode.buttonFg(pHover: Float) { }
}

private class PlayButtonBg(playStopAnimator: AnimatedFloatBidir) : ModeButtonBg(playStopAnimator, MdColor.GREEN) {
    private val playAccent = MdColor.PINK
    var bgColorPlay = ColorGradient(UiColors.titleBg.mix(playAccent, 0.3f), playAccent)

    private val pPlay = listOf(
        Vec3f(1f, 0f, 0f),
        Vec3f(1f, 0f, 0f),
        Vec3f(cos(120f.toRad()), sin(120f.toRad()), 0f),
        Vec3f(cos(240f.toRad()), sin(240f.toRad()), 0f)
    )
    private val pStopSz = 0.8f
    private val pStop = listOf(
        Vec3f(1f * pStopSz, -1f * pStopSz, 0f),
        Vec3f(1f * pStopSz, 1f * pStopSz, 0f),
        Vec3f(-1f * pStopSz, 1f * pStopSz, 0f),
        Vec3f(-1f * pStopSz, -1f * pStopSz, 0f)
    )

    init {
        fgColor = ColorGradient(
            EditorUi.EDITOR_THEME_COLORS.onBackgroundAlpha(0.5f),
            EditorUi.EDITOR_THEME_COLORS.onBackground
        )
        bgColor = ColorGradient(UiColors.titleBg.mix(accent, 0.3f), accent)
    }

    override fun getBgColor(pHover: Float): Color {
        return when (playStopAnimator.value) {
            0f -> {
                // stopped
                bgColor.getColor(pHover)
            }
            1f -> {
                // playing
                bgColorPlay.getColor(pHover)
            }
            else -> {
                // animating
                val bg1 = bgColor.getColor(pHover)
                val bg2 = bgColorPlay.getColor(pHover)
                bg1.mix(bg2, playStopAnimator.value)
            }
        }
    }

    override fun UiNode.buttonFg(pHover: Float) {
        val r = innerWidthPx * 0.35f
        val pAnim = playStopAnimator.value

        getPlainBuilder().configured(fgColor.getColor(pHover)) {
            translate(round(widthPx * 0.5f), round(heightPx * 0.5f), 0f)
            if (playStopAnimator.isForward) {
                rotate(90f * pAnim, 0f, 0f, 1f)
            } else {
                rotate(-90f * pAnim, 0f, 0f, 1f)
            }

            val vi = IntArray(4)
            val pa = MutableVec3f()
            val pb = MutableVec3f()
            for (i in 0..3) {
                pa.set(pPlay[i]).scale(1f - pAnim)
                pb.set(pStop[i]).scale(pAnim)
                pa.add(pb)
                vi[i] = vertex { set(round(pa.x * r), round(pa.y * r), round(pa.z * r)) }
            }
            addTriIndices(vi[0], vi[1], vi[2])
            addTriIndices(vi[0], vi[2], vi[3])
        }
    }
}

private class PauseButtonBg(playStopAnimator: AnimatedFloatBidir, val playPauseAnimator: AnimatedFloatBidir)
    : ModeButtonBg(playStopAnimator, EditorUi.EDITOR_THEME_COLORS.primary)
{
    private val pPlay = listOf(
        Vec3f(1f, 0f, 0f),
        Vec3f(1f, 0f, 0f),
        Vec3f(cos(120f.toRad()), sin(120f.toRad()), 0f),
        Vec3f(cos(240f.toRad()), 0f, 0f),
    )
    private val pPause = listOf(
        Vec3f(1f, 0.25f, 0f),
        Vec3f(1f, 0.65f, 0f),
        Vec3f(-1f, 0.65f, 0f),
        Vec3f(-1f, 0.25f, 0f)
    )

    override fun UiNode.buttonFg(pHover: Float) {
        val r = innerWidthPx * 0.35f
        val pAnim = playPauseAnimator.progressAndUse()
        val fg = fgColor.getColor(pHover)

        getPlainBuilder().configured(MutableColor(fg).apply { a *= Easing.quadRev(playStopAnimator.value) }) {
            translate(round(widthPx * 0.5f), round(heightPx * 0.5f), 0f)
            rotate(90f * (1f - pAnim), 0f, 0f, 1f)

            val vi = IntArray(4)
            val pa = MutableVec3f()
            val pb = MutableVec3f()
            for (i in 0..3) {
                pa.set(pPlay[i]).scale(pAnim)
                pb.set(pPause[i]).scale(1f - pAnim)
                pa.add(pb)
                vi[i] = vertex { set(round(pa.x * r), round(pa.y * r), round(pa.z * r)) }
            }
            addTriIndices(vi[0], vi[1], vi[2])
            addTriIndices(vi[0], vi[2], vi[3])

            for (i in 0..3) {
                pa.set(pPlay[i]).scale(pAnim)
                pb.set(pPause[i]).scale(1f - pAnim)
                pa.add(pb)
                vi[i] = vertex { set(round(pa.x * r), round(pa.y * -r), round(pa.z * r)) }
            }
            addTriIndices(vi[0], vi[1], vi[2])
            addTriIndices(vi[0], vi[2], vi[3])
        }
    }
}

private class ResetButtonBg(playStopAnimator: AnimatedFloatBidir)
    : ModeButtonBg(playStopAnimator, EditorUi.EDITOR_THEME_COLORS.primary)
{
    override fun UiNode.buttonFg(pHover: Float) {
        val r = innerWidthPx * 0.35f
        val fg = fgColor.getColor(pHover)
        val x = round(widthPx * 0.5f - r * 1.1f)
        val y = round(heightPx * 0.5f - r)
        val w = round(r * 0.4f)
        val h = round(r * 2f)
        getUiPrimitives().localRect(x, y, w, h, fg)
        getPlainBuilder().configured(MutableColor(fg).apply { a *= Easing.quadRev(playStopAnimator.value) }) {
            translate(round(widthPx * 0.5f + r * 0.3f), round(heightPx * 0.5f), 0f)
            val i0 = vertex { set(-r, 0f, 0f) }
            val i1 = vertex { set(-cos(120f.toRad()) * r, sin(120f.toRad()) * r, 0f) }
            val i2 = vertex { set(-cos(240f.toRad()) * r, sin(240f.toRad()) * r, 0f) }
            addTriIndices(i0, i1, i2)
        }
    }
}