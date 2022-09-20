package de.fabmax.kool.modules.ui2

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.clamp
import de.fabmax.kool.util.Color

interface SliderScope : UiScope {
    override val modifier: SliderModifier
}

open class SliderModifier(surface: UiSurface) : TextModifier(surface) {
    var value: Float by property(0.5f)
    var minValue: Float by property(0f)
    var maxValue: Float by property(1f)
    var onChange: ((Float) -> Unit)? by property(null)
    var onChangeEnd: ((Float) -> Unit)? by property(null)

    var orientation: SliderOrientation by property(SliderOrientation.Horizontal)

    var knobColor: Color by property { it.colors.secondary }
    var trackColor: Color by property { it.colors.secondaryVariant.withAlpha(0.5f) }
    var trackColorActive: Color? by property { it.colors.secondaryVariant.withAlpha(0.8f) }
}

fun <T: SliderModifier> T.value(value: Float): T { this.value = value; return this }
fun <T: SliderModifier> T.minValue(min: Float): T { this.minValue = min; return this }
fun <T: SliderModifier> T.maxValue(max: Float): T { this.maxValue = max; return this }
fun <T: SliderModifier> T.onChange(block: ((Float) -> Unit)?): T { onChange = block; return this }
fun <T: SliderModifier> T.onChangeEnd(block: ((Float) -> Unit)?): T { onChangeEnd = block; return this }

fun <T: SliderModifier> T.orientation(orientation: SliderOrientation): T { this.orientation = orientation; return this }

fun <T: SliderModifier> T.colors(
    knobColor: Color = this.knobColor,
    trackColor: Color = this.trackColor,
    trackColorActive: Color? = this.trackColorActive
): T {
    this.knobColor = knobColor
    this.trackColor = trackColor
    this.trackColorActive = trackColorActive
    return this
}

enum class SliderOrientation {
    Horizontal,
    Vertical
}

inline fun UiScope.Slider(value: Float = 0.5f, min: Float = 0f, max: Float = 1f, block: SliderScope.() -> Unit): SliderScope {
    val slider = uiNode.createChild(SliderNode::class, SliderNode.factory)
    slider.modifier
        .value(value)
        .minValue(min)
        .maxValue(max)
        .margin(8.dp)
        .dragListener(slider)
    slider.block()
    return slider
}

class SliderNode(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface), SliderScope, Draggable {
    override val modifier = SliderModifier(surface)

    private val knobCenter = MutableVec2f()
    private val dragStart = MutableVec2f()

    private val knobDiameter: Dp get() = sizes.sliderHeight
    private val trackLenPx: Float get() = knobDiameter.px * 6
    private val trackHeightPx: Float get() = knobDiameter.px * 0.4f

    override fun measureContentSize(ctx: KoolContext) {
        val w: Float
        val h: Float
        if (modifier.orientation == SliderOrientation.Horizontal) {
            w = trackLenPx
            h = knobDiameter.px
        } else {
            w = knobDiameter.px
            h = trackLenPx
        }
        val measuredWidth = w + paddingStartPx + paddingEndPx
        val measuredHeight = h + paddingTopPx + paddingBottomPx
        setContentSize(measuredWidth, measuredHeight)
    }

    override fun render(ctx: KoolContext) {
        super.render(ctx)
        val draw = getUiPrimitives()
        val knobPos = ((modifier.value - modifier.minValue) / (modifier.maxValue - modifier.minValue)).clamp()
        val kD = knobDiameter.px
        val kR = kD * 0.5f
        val tH = trackHeightPx
        val tR = tH * 0.5f

        if (modifier.orientation == SliderOrientation.Horizontal) {
            val c = heightPx * 0.5f
            val xMin = paddingStartPx + kR
            val xMax = widthPx - paddingEndPx - kR
            val moveW = xMax - xMin
            val knobX = xMin + moveW * knobPos

            if (modifier.trackColorActive != null) {
                val colorAct = modifier.trackColorActive ?: modifier.trackColor
                draw.localRoundRect(
                    xMin - tR, c - tR,
                    knobX - xMin + tH, tH, tR, colorAct)
                draw.localRoundRect(
                    knobX - tR, c - tR,
                    xMax - knobX + tH, tH, tR, modifier.trackColor)
            } else {
                draw.localRoundRect(
                    xMin - tR, c - tR,
                    xMax - xMin + tH, tH, tR, modifier.trackColor)
            }
            knobCenter.set(knobX, c)
            draw.localCircle(knobX, c, knobDiameter.px * 0.5f, modifier.knobColor)

        } else {
            val c = widthPx * 0.5f
            val yMin = paddingTopPx + kR
            val yMax = heightPx - paddingBottomPx - kR
            val moveH = yMax - yMin
            val knobY = yMin + moveH * (1f - knobPos)

            if (modifier.trackColorActive != null) {
                val colorAct = modifier.trackColorActive ?: modifier.trackColor
                draw.localRoundRect(
                    c - tR, yMin - tR,
                    tH, knobY - yMin + tH, tR, modifier.trackColor)
                draw.localRoundRect(
                    c - tR, knobY - tR,
                    tH, yMax - knobY + tH, tR, colorAct)
            } else {
                draw.localRoundRect(
                    c - tR, yMin - tR,
                    tH, yMax - yMin + tH, tR, modifier.trackColor)
            }
            knobCenter.set(c, knobY)
            draw.localCircle(c, knobY, knobDiameter.px * 0.5f, modifier.knobColor)
        }
    }

    private fun computeValue(ev: PointerEvent): Float {
        val kD = knobDiameter.px
        return if (modifier.orientation == SliderOrientation.Horizontal) {
            val innerW = innerWidthPx - kD
            val dragKnobPos = dragStart.x + ev.pointer.dragDeltaX.toFloat() - paddingStartPx - kD * 0.5f
            val f = (dragKnobPos / innerW).clamp()
            f * (modifier.maxValue - modifier.minValue) + modifier.minValue
        } else {
            val innerH = innerHeightPx - kD
            val dragKnobPos = dragStart.y + ev.pointer.dragDeltaY.toFloat() - paddingTopPx - kD * 0.5f
            val f = 1f - (dragKnobPos / innerH).clamp()
            f * (modifier.maxValue - modifier.minValue) + modifier.minValue
        }
    }

    override fun onDragStart(ev: PointerEvent) {
        if (ev.position.distance(knobCenter) < knobDiameter.px) {
            dragStart.set(knobCenter)
        } else {
            ev.reject()
        }
    }

    override fun onDrag(ev: PointerEvent) {
        modifier.onChange?.invoke(computeValue(ev))
    }

    override fun onDragEnd(ev: PointerEvent) {
        modifier.onChangeEnd?.invoke(computeValue(ev))
    }

    companion object {
        val factory: (UiNode, UiSurface) -> SliderNode = { parent, surface -> SliderNode(parent, surface) }
    }
}