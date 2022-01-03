package de.fabmax.kool.scene.ui

import de.fabmax.kool.InputManager
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.clamp
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MutableColor
import de.fabmax.kool.util.animation.Animator
import de.fabmax.kool.util.animation.CosAnimator
import de.fabmax.kool.util.animation.InterpolatedFloat
import de.fabmax.kool.util.animation.LinearAnimator
import kotlin.math.max
import kotlin.math.min

/**
 * @author fabmax
 */

class TextField(name: String, root: UiRoot) : Label(name, root) {

    var hasFocus = true

    val editText = EditableText()
    var maxLength: Int
        get() = editText.maxLength
        set(value) { editText.maxLength = value }

    val onKeyEvent = mutableListOf<(InputManager.KeyEvent) -> Unit>()

    val lineColor = ThemeOrCustomProp(Color.WHITE)
    val caretColor = ThemeOrCustomProp(Color.WHITE)
    val selectionColor = ThemeOrCustomProp(Color.WHITE)

    val charWidths = mutableListOf<Float>()

    private var lastClickTime = -1.0
    private val startDrag = MutableVec2f()
    private val mousePos = MutableVec2f()

    init {
        onUpdate += { evt ->
            if (isVisible && hasFocus && evt.ctx.inputMgr.keyEvents.isNotEmpty()) {
                for (e in evt.ctx.inputMgr.keyEvents) {
                    onKeyEvent.forEach { it(e) }
                    if (e.isCharTyped) {
                        editText.charTyped(e.typedChar)

                    } else if (e.isPressed) {
                        when (e.keyCode) {
                            InputManager.KEY_BACKSPACE -> editText.backspace()
                            InputManager.KEY_DEL -> editText.deleteSelection()
                            InputManager.KEY_CURSOR_LEFT -> {
                                if (e.isCtrlDown) {
                                    editText.moveCaret(EditableText.MOVE_WORD_LEFT, e.isShiftDown)
                                } else {
                                    editText.moveCaret(EditableText.MOVE_LEFT, e.isShiftDown)
                                }
                            }
                            InputManager.KEY_CURSOR_RIGHT -> {
                                if (e.isCtrlDown) {
                                    editText.moveCaret(EditableText.MOVE_WORD_RIGHT, e.isShiftDown)
                                } else {
                                    editText.moveCaret(EditableText.MOVE_RIGHT, e.isShiftDown)
                                }
                            }
                            InputManager.KEY_HOME -> editText.moveCaret(EditableText.MOVE_START, e.isShiftDown)
                            InputManager.KEY_END -> editText.moveCaret(EditableText.MOVE_END, e.isShiftDown)
                            else -> { }
                        }
                    }
                }
                text = editText.toString()
            }
        }

        onHover += { ptr, rt, ctx ->
            val ptX = rt.hitPositionLocal.x - componentBounds.min.x
            val ptY = rt.hitPositionLocal.y - componentBounds.min.y

            val txtUi = ui.prop as? TextFieldUi
            if (txtUi != null) {
                var isDoubleClick = false
                if (ptr.isLeftButtonClicked) {
                    isDoubleClick = ctx.time < lastClickTime + InputManager.DOUBLE_CLICK_INTERVAL_SECS
                    lastClickTime = ctx.time
                }

                if (isDoubleClick) {
                    editText.caretPosition = text.length
                    editText.selectionStart = 0
                } else if (ptr.isLeftButtonDown) {
                    mousePos.set(ptX, ptY)
                    if (ptr.isLeftButtonEvent) {
                        startDrag.set(mousePos)
                    }
                    dragSelectText(txtUi, startDrag, mousePos)
                }
            }
        }
    }

    private fun dragSelectText(ui: TextFieldUi, from: Vec2f, to: Vec2f) {
        val left = if (from.x < to.x) from else to
        val right = if (left === from) to else from

        var startI = 0

        var pos = ui.textStartX
        while (pos < left.x && startI < text.length) {
            pos += charWidths[startI++]
            if (pos > left.x) {
                pos -= charWidths[--startI]
                break
            }
        }

        var endI = startI
        while (pos < right.x && endI < text.length) {
            pos += charWidths[endI++]
            if (pos > right.x) {
                endI--
                break
            }
        }

        editText.selectionStart = startI
        editText.caretPosition = endI
    }

    override fun createThemeUi(ctx: KoolContext): ComponentUi {
        lineColor.setTheme(root.theme.accentColor)
        caretColor.setTheme(root.theme.accentColor)
        selectionColor.setTheme(root.theme.accentColor.withAlpha(0.4f))

        return root.theme.newTextFieldUi(this)
    }
}

open class TextFieldUi(val textField: TextField, baseUi: ComponentUi) : LabelUi(textField, baseUi) {

    private val caretAlphaAnimator = CosAnimator(InterpolatedFloat(0f, 1f))
    private val caretColor = MutableColor()

    private val caretDrawPos = InterpolatedFloat(0f, 0f)
    private val caretPosAnimator = LinearAnimator(caretDrawPos)

    init {
        caretAlphaAnimator.duration = 0.5f
        caretAlphaAnimator.repeating = Animator.REPEAT_TOGGLE_DIR

        caretPosAnimator.duration = 0.1f
    }

    override fun onRender(ctx: KoolContext) {
        textField.requestUiUpdate()
        super.onRender(ctx)
    }

    override fun renderText(dispText: String, ctx: KoolContext) {
        val x1 = label.padding.left.toUnits(label.width, label.dpi)
        val x2 = label.width - label.padding.right.toUnits(label.width, label.dpi)
        val y = textBaseline - (font?.charMap?.fontProps?.sizePts ?: 0f) * 0.2f

        textField.charWidths.clear()
        var textWidth = 0f
        dispText.forEach {
            val cw = font?.charWidth(it) ?: 0f
            textField.charWidths += cw
            textWidth += cw
        }

        // crop text in case it is too long
        var txt = dispText
        var cropWidth = 0f
        if (textWidth > textField.width) {
            var remaining = x2 - x1 - textField.charWidths.last()
            var startI = dispText.length - 1
            while (remaining > 0f && startI > 0) {
                remaining -= textField.charWidths[startI--]
            }
            txt = dispText.substring(startI)
            cropWidth = (0 until startI).sumOf { textField.charWidths[it].toDouble() }.toFloat()
        }

        var caretX = textStartX - cropWidth
        var selectionX = textStartX - cropWidth
        if (textField.editText.caretPosition > 0 || textField.editText.selectionStart > 0) {
            for (i in 0 until max(textField.editText.caretPosition, textField.editText.selectionStart)) {
                val w = textField.charWidths[i]
                if (i < textField.editText.caretPosition) {
                    caretX += w
                }
                if (i < textField.editText.selectionStart) {
                    selectionX += w
                }
            }
        }
        if (caretX != caretDrawPos.to) {
            caretDrawPos.from = caretDrawPos.value
            caretDrawPos.to = caretX
            caretPosAnimator.progress = 0f
            caretPosAnimator.speed = 1f
        }
        caretX = caretPosAnimator.tick(ctx)

        meshBuilder.withTransform {
            translate(0f, 0f, label.dp(0.1f))

            // draw selection
            if (textField.editText.selectionStart != textField.editText.caretPosition) {
                meshBuilder.color = textField.selectionColor.apply()
                meshBuilder.rect {
                    origin.set(caretX, y, 0f)
                    size.set(selectionX - caretX, (font?.charMap?.fontProps?.sizePts ?: 0f) * 1.2f)
                    zeroTexCoords()
                }
            }

            // draw underline
            meshBuilder.color = textField.lineColor.apply()
            meshBuilder.line(x1, y, x2, y, label.dp(1.5f))

            // draw caret
            caretColor.set(textField.caretColor.apply())
            caretColor.a = caretAlphaAnimator.tick(ctx)
            meshBuilder.color = caretColor
            meshBuilder.line(caretX, y, caretX, textBaseline + (font?.charMap?.fontProps?.sizePts ?: 0f), label.dp(1.5f))

            super.renderText(txt, ctx)
        }
    }
}

class EditableText(txt: String = "") {

    var text: String = txt
        set(value) {
            if (caretPosition > value.length) {
                caretPosition = value.length
            }
            if (selectionStart > value.length) {
                selectionStart = value.length
            }
            field = value
        }

    var maxLength = 100

    var caretPosition = 0
        set(value) {
            field = value.clamp(0, text.length)
        }

    var selectionStart = 0
        set(value) {
            field = value.clamp(0, text.length)
        }

    fun charTyped(c: Char) {
        replaceSelection("$c")
    }

    fun moveCaret(mode: Int, selection: Boolean) {
        when (mode) {
            MOVE_LEFT -> caretPosition--
            MOVE_RIGHT -> caretPosition++
            MOVE_START -> caretPosition = 0
            MOVE_END -> caretPosition = text.length
            MOVE_WORD_LEFT -> moveWordLeft()
            MOVE_WORD_RIGHT -> moveWordRight()
        }
        if (!selection) {
            selectionStart = caretPosition
        }
    }

    private fun moveWordLeft() {
        if (caretPosition > 0) {
            val idx = text.substring(0, caretPosition).lastIndexOf(' ')
            if (idx < 0) {
                caretPosition = 0
            } else {
                caretPosition = idx
            }
        }
    }

    private fun moveWordRight() {
        if (caretPosition < text.length) {
            val idx = text.indexOf(' ', caretPosition)
            if (idx < 0) {
                caretPosition = text.length
            } else {
                caretPosition = idx + 1
            }
        }
    }

    fun backspace() {
        if (selectionStart != caretPosition) {
            replaceSelection("")
        } else if (caretPosition > 0) {
            selectionStart = --caretPosition
            text = text.substring(0, caretPosition) + text.substring(caretPosition + 1)
        }
    }

    fun deleteSelection() {
        if (selectionStart != caretPosition) {
            replaceSelection("")
        } else if (caretPosition < text.length) {
            text = text.substring(0, caretPosition) + text.substring(caretPosition + 1)
        }
    }

    fun replaceSelection(string: String) {
        val start = min(selectionStart, caretPosition)
        val end = max(selectionStart, caretPosition)

        val newText = text.substring(0, start) + string + text.substring(end)
        if (maxLength <= 0 || newText.length < text.length || newText.length <= maxLength) {
            text = newText
            caretPosition = min(selectionStart, caretPosition) + string.length
            selectionStart = caretPosition
        }
    }

    operator fun get(index: Int): Char {
        return text[index]
    }

    override fun toString(): String {
        return text
    }

    companion object {
        const val MOVE_LEFT = 1
        const val MOVE_RIGHT = 2
        const val MOVE_WORD_LEFT = 3
        const val MOVE_WORD_RIGHT = 4
        const val MOVE_START = 5
        const val MOVE_END = 6
    }
}
