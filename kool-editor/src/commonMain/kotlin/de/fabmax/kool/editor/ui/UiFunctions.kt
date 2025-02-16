package de.fabmax.kool.editor.ui

import de.fabmax.kool.editor.AppAssetType
import de.fabmax.kool.editor.AssetItem
import de.fabmax.kool.editor.CachedAppAssets
import de.fabmax.kool.editor.KoolEditor
import de.fabmax.kool.editor.actions.EditorAction
import de.fabmax.kool.editor.actions.EditorActions
import de.fabmax.kool.input.CursorShape
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.math.*
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.toString
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MdColor
import kotlin.math.*

fun UiScope.editorTitleBar(
    windowDockable: UiDockable,
    title: String = windowDockable.name,
    roundedTop: Boolean = false,
    onClose: ((PointerEvent) -> Unit)? = null
) {
    Row(Grow.Std, height = sizes.lineHeightTitle) {
        modifier
            .padding(horizontal = sizes.gap - sizes.borderWidth)

        if (roundedTop) {
            modifier.background(TitleBarBackground(UiColors.titleBg, sizes.gap.px, false))
        } else {
            modifier.backgroundColor(UiColors.titleBg)
        }

        if (!windowDockable.isDocked.use()) {
            modifier.margin(sizes.borderWidth)
        }

        with(windowDockable) {
            registerDragCallbacks()
        }

        Text(title) {
            modifier
                .width(Grow.Std)
                .textColor(UiColors.titleText)
                .font(sizes.boldText)
                .alignY(AlignmentY.Center)
        }

        onClose?.let { action ->
            Button {
                modifier
                    .size(sizes.largeGap, sizes.largeGap)
                    .padding(Dp.ZERO)
                    .alignY(AlignmentY.Center)
                    .isClickFeedback(false)
                    .background(remember { CloseButtonBackground() })
                    .onClick(action)
            }
        }
    }
}

class CloseButtonBackground : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) = node.run {
        val r = innerWidthPx * 0.5f
        val fgColor = colors.onBackground
        val bgColor =
        if ((node as ButtonNode).isHovered) {
            MdColor.RED
        } else {
            colors.componentBg
        }
        getUiPrimitives().localCircle(widthPx * 0.5f, heightPx * 0.5f, r, bgColor)

        getPlainBuilder().configured(fgColor) {
            translate(widthPx * 0.5f, heightPx * 0.5f, 0f)
            rotate(45f, Vec3f.Z_AXIS)
            rect {
                size.set(r * 1.3f, 1.dp.px)
                origin.set(size.x * -0.5f, size.y * -0.5f, 0f)
            }
            rect {
                size.set(1.dp.px, r * 1.3f)
                origin.set(size.x * -0.5f, size.y * -0.5f, 0f)
            }
        }
    }
}

fun UiScope.doubleTextField(
    value: Double,
    precision: Int = precisionForValue(value),
    width: Dimension = FitContent,
    dragChangeSpeed: Double = 0.0,
    minValue: Double = Double.NEGATIVE_INFINITY,
    maxValue: Double = Double.POSITIVE_INFINITY,
    editHandler: ValueEditHandler<Double>
) = TextField {
    var text by remember(value.toString(precision))
    var wasFocuesd by remember(false)
    var dragStartValue by remember(value)

    if (!isFocused.use()) {
        if (wasFocuesd) {
            // focus lost, apply edited value
            text.parseDouble(minValue, maxValue)?.let { editHandler.onEditEnd(dragStartValue, it) }
        } else {
            text = value.toString(precision)
        }
    } else if (!wasFocuesd) {
        // gained focus
        dragStartValue = value
        editHandler.onEditStart(dragStartValue)
    }
    wasFocuesd = isFocused.value

    defaultTextfieldStyle()
    modifier
        .text(text)
        .width(width)
        .padding(horizontal = sizes.smallTextFieldPadding)
        .alignY(AlignmentY.Center)
        .textAlignX(AlignmentX.End)
        .onChange { text = it }
        .onEnterPressed {
            // unfocus text field, onSet() is called in focus lost handler
            surface.requestFocus(null)
        }

    if (!wasFocuesd && dragChangeSpeed != 0.0) {
        modifier.clearDragCallbacks()
        modifier.clearHoverCallbacks()
        modifier
            .onHover { PointerInput.cursorShape = CursorShape.H_RESIZE }
            .onDragStart {
                dragStartValue = value
                editHandler.onEditStart(dragStartValue)
            }
            .onDrag {
                val dragVal = dragStartValue + dragChangeSpeed * Dp.fromPx(it.pointer.dragDeltaX.toFloat()).value
                editHandler.onEdit(dragVal.clamp(minValue, maxValue))
            }
            .onDragEnd {
                editHandler.onEditEnd(dragStartValue, value)
            }
    }
}

fun String.parseDouble(min: Double = Double.NEGATIVE_INFINITY, max: Double = Double.POSITIVE_INFINITY): Double? {
    var d = replace(',', '.').toDoubleOrNull()
    if (d != null && d.isFinite()) {
        d = d.clamp(min, max)
    }
    return d
}

fun UiScope.intTextField(
    value: Int,
    width: Dimension = FitContent,
    dragChangeSpeed: Double = 0.0,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    editHandler: ValueEditHandler<Int>
) = TextField {
    var text by remember(value.toString())
    var wasFocuesd by remember(false)
    var dragStartValue by remember(value)

    if (!isFocused.use()) {
        if (wasFocuesd) {
            // focus lost, apply edited value
            text.parseInt(minValue, maxValue)?.let { editHandler.onEditEnd(dragStartValue, it) }
        } else {
            text = value.toString()
        }
    } else if (!wasFocuesd) {
        // gained focus
        dragStartValue = value
        editHandler.onEditStart(dragStartValue)
    }
    wasFocuesd = isFocused.value

    defaultTextfieldStyle()
    modifier
        .text(text)
        .width(width)
        .padding(horizontal = sizes.smallTextFieldPadding)
        .alignY(AlignmentY.Center)
        .textAlignX(AlignmentX.End)
        .onChange { text = it }
        .onEnterPressed {
            // unfocus text field, onSet() is called in focus lost handler
            surface.requestFocus(null)
        }

    if (!wasFocuesd && dragChangeSpeed != 0.0) {
        modifier.clearDragCallbacks()
        modifier.clearHoverCallbacks()
        modifier
            .onHover { PointerInput.cursorShape = CursorShape.H_RESIZE }
            .onDragStart {
                dragStartValue = value
                editHandler.onEditStart(dragStartValue)
            }
            .onDrag {
                val dragVal = (dragStartValue + dragChangeSpeed * Dp.fromPx(it.pointer.dragDeltaX.toFloat()).value).roundToInt()
                editHandler.onEdit(dragVal.clamp(minValue, maxValue))
            }
            .onDragEnd {
                editHandler.onEditEnd(dragStartValue, value)
            }
    }
}

fun String.parseInt(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int? {
    var i = toIntOrNull()
    if (i != null) {
        i = i.clamp(min, max)
    }
    return i
}

fun UiScope.xyRow(
    label: String,
    xy: Vec2d,
    precision: Vec2i = Vec2i(precisionForValue(xy.x), precisionForValue(xy.y)),
    minValues: Vec2d? = null,
    maxValues: Vec2d? = null,
    dragChangeSpeed: Vec2d = Vec2d.ZERO,
    editHandler: ValueEditHandler<Vec2d>
) = Column(width = Grow.Std) {
    menuRow {
        Text(label) {
            modifier.alignY(AlignmentY.Center)
        }
    }
    menuRow(Dp.ZERO) {
        val xEditHandler = object : ValueEditHandler<Double> {
            override fun onEditStart(startValue: Double) = editHandler.onEditStart(Vec2d(startValue, xy.y))
            override fun onEdit(value: Double) = editHandler.onEdit(Vec2d(value, xy.y))
            override fun onEditEnd(startValue: Double, endValue: Double) = editHandler.onEditEnd(Vec2d(startValue, xy.y), Vec2d(endValue, xy.y))
        }
        val yEditHandler = object : ValueEditHandler<Double> {
            override fun onEditStart(startValue: Double) = editHandler.onEditStart(Vec2d(xy.x, startValue))
            override fun onEdit(value: Double) = editHandler.onEdit(Vec2d(xy.x, value))
            override fun onEditEnd(startValue: Double, endValue: Double) = editHandler.onEditEnd(Vec2d(xy.x, startValue), Vec2d(xy.x, endValue))
        }

        Text("X") {
            modifier
                .width(sizes.largeGap * 0.75f)
                .margin(end = sizes.smallGap)
                .textAlignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .textColor(MdColor.RED tone 300)
        }
        val minX = minValues?.x ?: Double.NEGATIVE_INFINITY
        val maxX = maxValues?.x ?: Double.POSITIVE_INFINITY
        doubleTextField(xy.x, precision.x, Grow.Std, dragChangeSpeed.x, minX, maxX, editHandler = xEditHandler)

        Text("Y") {
            modifier
                .width(sizes.largeGap)
                .margin(end = sizes.smallGap)
                .textAlignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .textColor(MdColor.GREEN tone 300)
        }
        val minY = minValues?.y ?: Double.NEGATIVE_INFINITY
        val maxY = maxValues?.y ?: Double.POSITIVE_INFINITY
        doubleTextField(xy.y, precision.y, Grow.Std, dragChangeSpeed.y, minY, maxY, editHandler = yEditHandler)
    }
}

fun UiScope.xyzRow(
    label: String,
    xyz: Vec3d,
    precision: Vec3i = Vec3i(precisionForValue(xyz.x), precisionForValue(xyz.y), precisionForValue(xyz.z)),
    minValues: Vec3d? = null,
    maxValues: Vec3d? = null,
    dragChangeSpeed: Vec3d = Vec3d.ZERO,
    editHandler: ValueEditHandler<Vec3d>
) = Column(width = Grow.Std) {
    menuRow {
        Text(label) {
            modifier.alignY(AlignmentY.Center)
        }
    }
    menuRow(Dp.ZERO) {
        val xEditHandler = object : ValueEditHandler<Double> {
            override fun onEditStart(startValue: Double) = editHandler.onEditStart(Vec3d(startValue, xyz.y, xyz.z))
            override fun onEdit(value: Double) = editHandler.onEdit(Vec3d(value, xyz.y, xyz.z))
            override fun onEditEnd(startValue: Double, endValue: Double) = editHandler.onEditEnd(Vec3d(startValue, xyz.y, xyz.z), Vec3d(endValue, xyz.y, xyz.z))
        }
        val yEditHandler = object : ValueEditHandler<Double> {
            override fun onEditStart(startValue: Double) = editHandler.onEditStart(Vec3d(xyz.x, startValue, xyz.z))
            override fun onEdit(value: Double) = editHandler.onEdit(Vec3d(xyz.x, value, xyz.z))
            override fun onEditEnd(startValue: Double, endValue: Double) = editHandler.onEditEnd(Vec3d(xyz.x, startValue, xyz.z), Vec3d(xyz.x, endValue, xyz.z))
        }
        val zEditHandler = object : ValueEditHandler<Double> {
            override fun onEditStart(startValue: Double) = editHandler.onEditStart(Vec3d(xyz.x, xyz.y, startValue))
            override fun onEdit(value: Double) = editHandler.onEdit(Vec3d(xyz.x, xyz.y, value))
            override fun onEditEnd(startValue: Double, endValue: Double) = editHandler.onEditEnd(Vec3d(xyz.x, xyz.y, startValue), Vec3d(xyz.x, xyz.y, endValue))
        }

        Text("X") {
            modifier
                .width(sizes.largeGap * 0.75f)
                .margin(end = sizes.smallGap)
                .textAlignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .textColor(MdColor.RED tone 300)
        }
        val minX = minValues?.x ?: Double.NEGATIVE_INFINITY
        val maxX = maxValues?.x ?: Double.POSITIVE_INFINITY
        doubleTextField(xyz.x, precision.x, Grow.Std, dragChangeSpeed.x, minX, maxX, editHandler = xEditHandler)

        Text("Y") {
            modifier
                .width(sizes.largeGap)
                .margin(end = sizes.smallGap)
                .textAlignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .textColor(MdColor.GREEN tone 300)
        }
        val minY = minValues?.y ?: Double.NEGATIVE_INFINITY
        val maxY = maxValues?.y ?: Double.POSITIVE_INFINITY
        doubleTextField(xyz.y, precision.y, Grow.Std, dragChangeSpeed.y, minY, maxY, editHandler = yEditHandler)

        Text("Z") {
            modifier
                .width(sizes.largeGap)
                .margin(end = sizes.smallGap)
                .textAlignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .textColor(MdColor.BLUE tone 300)
        }
        val minZ = minValues?.z ?: Double.NEGATIVE_INFINITY
        val maxZ = maxValues?.z ?: Double.POSITIVE_INFINITY
        doubleTextField(xyz.z, precision.z, Grow.Std, dragChangeSpeed.z, minZ, maxZ, editHandler = zEditHandler)
    }
}

fun UiScope.xyzwRow(
    label: String,
    xyzw: Vec4d,
    precision: Vec4i = Vec4i(precisionForValue(xyzw.x), precisionForValue(xyzw.y), precisionForValue(xyzw.z), precisionForValue(xyzw.w)),
    minValues: Vec4d? = null,
    maxValues: Vec4d? = null,
    dragChangeSpeed: Vec4d = Vec4d.ZERO,
    editHandler: ValueEditHandler<Vec4d>
) = Column(width = Grow.Std) {
    menuRow {
        Text(label) {
            modifier.alignY(AlignmentY.Center)
        }
    }
    menuRow(Dp.ZERO) {
        val xEditHandler = object : ValueEditHandler<Double> {
            override fun onEditStart(startValue: Double) = editHandler.onEditStart(Vec4d(startValue, xyzw.y, xyzw.z, xyzw.w))
            override fun onEdit(value: Double) = editHandler.onEdit(Vec4d(value, xyzw.y, xyzw.z, xyzw.w))
            override fun onEditEnd(startValue: Double, endValue: Double) = editHandler.onEditEnd(Vec4d(startValue, xyzw.y, xyzw.z, xyzw.w), Vec4d(endValue, xyzw.y, xyzw.z, xyzw.w))
        }
        val yEditHandler = object : ValueEditHandler<Double> {
            override fun onEditStart(startValue: Double) = editHandler.onEditStart(Vec4d(xyzw.x, startValue, xyzw.z, xyzw.w))
            override fun onEdit(value: Double) = editHandler.onEdit(Vec4d(xyzw.x, value, xyzw.z, xyzw.w))
            override fun onEditEnd(startValue: Double, endValue: Double) = editHandler.onEditEnd(Vec4d(xyzw.x, startValue, xyzw.z, xyzw.w), Vec4d(xyzw.x, endValue, xyzw.z, xyzw.w))
        }
        val zEditHandler = object : ValueEditHandler<Double> {
            override fun onEditStart(startValue: Double) = editHandler.onEditStart(Vec4d(xyzw.x, xyzw.y, startValue, xyzw.w))
            override fun onEdit(value: Double) = editHandler.onEdit(Vec4d(xyzw.x, xyzw.y, value, xyzw.w))
            override fun onEditEnd(startValue: Double, endValue: Double) = editHandler.onEditEnd(Vec4d(xyzw.x, xyzw.y, startValue, xyzw.w), Vec4d(xyzw.x, xyzw.y, endValue, xyzw.w))
        }
        val wEditHandler = object : ValueEditHandler<Double> {
            override fun onEditStart(startValue: Double) = editHandler.onEditStart(Vec4d(xyzw.x, xyzw.y, xyzw.z, startValue))
            override fun onEdit(value: Double) = editHandler.onEdit(Vec4d(xyzw.x, xyzw.y, xyzw.z, value))
            override fun onEditEnd(startValue: Double, endValue: Double) = editHandler.onEditEnd(Vec4d(xyzw.x, xyzw.y, xyzw.z, startValue), Vec4d(xyzw.x, xyzw.y, xyzw.z, endValue))
        }

        Text("X") {
            modifier
                .width(sizes.largeGap * 0.75f)
                .margin(end = sizes.smallGap)
                .textAlignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .textColor(MdColor.RED tone 300)
        }
        val minX = minValues?.x ?: Double.NEGATIVE_INFINITY
        val maxX = maxValues?.x ?: Double.POSITIVE_INFINITY
        doubleTextField(xyzw.x, precision.x, Grow.Std, dragChangeSpeed.x, minX, maxX, editHandler = xEditHandler)

        Text("Y") {
            modifier
                .width(sizes.largeGap)
                .margin(end = sizes.smallGap)
                .textAlignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .textColor(MdColor.GREEN tone 300)
        }
        val minY = minValues?.y ?: Double.NEGATIVE_INFINITY
        val maxY = maxValues?.y ?: Double.POSITIVE_INFINITY
        doubleTextField(xyzw.y, precision.y, Grow.Std, dragChangeSpeed.y, minY, maxY, editHandler = yEditHandler)

        Text("Z") {
            modifier
                .width(sizes.largeGap)
                .margin(end = sizes.smallGap)
                .textAlignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .textColor(MdColor.BLUE tone 300)
        }
        val minZ = minValues?.z ?: Double.NEGATIVE_INFINITY
        val maxZ = maxValues?.z ?: Double.POSITIVE_INFINITY
        doubleTextField(xyzw.z, precision.z, Grow.Std, dragChangeSpeed.z, minZ, maxZ, editHandler = zEditHandler)

        Text("W") {
            modifier
                .width(sizes.largeGap)
                .margin(end = sizes.smallGap)
                .textAlignX(AlignmentX.End)
                .alignY(AlignmentY.Center)
                .textColor(MdColor.AMBER tone 300)
        }
        val minW = minValues?.w ?: Double.NEGATIVE_INFINITY
        val maxW = maxValues?.w ?: Double.POSITIVE_INFINITY
        doubleTextField(xyzw.w, precision.w, Grow.Std, dragChangeSpeed.w, minW, maxW, editHandler = wEditHandler)
    }
}

fun <T: Any> UiScope.labeledCombobox(
    label: String,
    items: List<T>,
    selectedIndex: Int,
    boxWidth: Dimension = Grow.Std,
    onItemSelected: (T) -> Unit
) = menuRow {
    Text(label) {
        modifier
            .width(sizes.baseSize * 3f)
            .alignY(AlignmentY.Center)
    }

    ComboBox {
        defaultComboBoxStyle()
        modifier
            .size(boxWidth, sizes.lineHeight)
            .items(items)
            .selectedIndex(selectedIndex)
            .onItemSelected {
                onItemSelected(items[it])
            }
    }
}

fun precisionForValue(value: Double): Int {
    if (value == 0.0) {
        return 3
    } else if (!value.isFinite()) {
        return 2
    }
    val log = 3.5 - log10(abs(value))
    val digits = if (log.isFinite()) log.roundToInt() else 3
    return min(3, max(1, digits))
}

fun UiScope.labeledColorPicker(
    label: String,
    pickerColor: Color,
    isWithAlpha: Boolean = false,
    boxWidth: Dimension = sizes.baseSize * 2,
    editHandler: ValueEditHandler<Color>
) = menuRow {
    Text(label) {
        modifier
            .width(Grow.Std)
            .alignY(AlignmentY.Center)
    }
    colorPicker(pickerColor, isWithAlpha, boxWidth, editHandler)
}

fun UiScope.colorPicker(
    pickerColor: Color,
    isWithAlpha: Boolean = false,
    boxWidth: Dimension = sizes.baseSize * 2,
    editHandler: ValueEditHandler<Color>
): UiScope {
    val currentColor = remember(pickerColor)
    var editColor by remember(pickerColor)
    var editStartColor by remember(pickerColor)

    // proxy current picker color in currentColor so that following remember block always uses the up-to-date value
    // instead of the initial value, captured when remember was first called
    currentColor.set(pickerColor)

    val colorPickerPopup = remember {
        val popup = AutoPopup()
        popup.popupContent = Composable {
            modifier
                .background(RoundRectBackground(colors.background, sizes.smallGap))
                .border(RoundRectBorder(colors.secondaryVariant, sizes.smallGap, sizes.borderWidth))
                .padding(sizes.smallGap)

            val hsv = currentColor.use().toHsv()
            val hue = remember(hsv.x)
            val sat = remember(hsv.y)
            val bri = remember(hsv.z)
            val alpha = if (isWithAlpha) remember(currentColor.value.a) else null
            val hexString = remember(currentColor.value.toHexString(isWithAlpha))

            ColorChooserV(hue, sat, bri, alpha, hexString) { color ->
                editColor = color
                editHandler.onEdit(color)
            }
        }
        popup.onShow = {
            editHandler.onEditStart(currentColor.value)
            editStartColor = currentColor.value
        }
        popup.onHide = { editHandler.onEditEnd(editStartColor, editColor) }
        popup
    }
    colorPickerPopup()

    return Box(width = boxWidth, height = Grow.Std) {
        var isHovered by remember(false)
        val borderColor = if (isHovered) colors.elevatedComponentBgHovered else colors.elevatedComponentBg
        modifier
            .background(RoundRectBackground(pickerColor, sizes.smallGap))
            .border(RoundRectBorder(borderColor, sizes.smallGap, sizes.borderWidth))
            .onEnter { isHovered = true }
            .onExit { isHovered = false }
            .onClick { colorPickerPopup.show(it) }
    }
}

fun UiScope.labeledDoubleTextField(
    label: String,
    value: Double,
    precision: Int = precisionForValue(value),
    valueWidth: Dimension = sizes.baseSize * 2,
    dragChangeSpeed: Double = 0.0,
    minValue: Double = Double.NEGATIVE_INFINITY,
    maxValue: Double = Double.POSITIVE_INFINITY,
    editHandler: ValueEditHandler<Double>
) = menuRow {
    Text(label) {
        modifier
            .width(Grow.Std)
            .alignY(AlignmentY.Center)
    }
    doubleTextField(value, precision, valueWidth, dragChangeSpeed, minValue, maxValue, editHandler)
}

fun UiScope.labeledIntTextField(
    label: String,
    value: Int,
    valueWidth: Dimension = sizes.baseSize * 2,
    dragChangeSpeed: Double = 0.0,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    editHandler: ValueEditHandler<Int>
) = menuRow {
    Text(label) {
        modifier
            .width(Grow.Std)
            .alignY(AlignmentY.Center)
    }
    intTextField(value, valueWidth, dragChangeSpeed, minValue, maxValue, editHandler)
}

fun UiScope.labeledTextField(
    label: String,
    text: String,
    valueWidth: Dimension = sizes.baseSize * 4,
    onEdited: (String) -> Unit
) = menuRow {
    Text(label) {
        modifier
            .width(Grow.Std)
            .alignY(AlignmentY.Center)
    }

    var editText by remember(text)
    TextField(editText) {
        if (!isFocused.use()) {
            editText = text
        }

        defaultTextfieldStyle()
        modifier
            .width(valueWidth)
            .onChange { editText = it }
            .onEnterPressed {
                onEdited(editText)
                surface.unfocus(this)
            }
    }
}

fun UiScope.labeledSlider(
    label: String,
    value: Double,
    min: Double = 0.0,
    max: Double = 1.0,
    precision: Int = precisionForValue(max - min),
    valueWidth: Dimension = sizes.baseSize * 2,
    editHandler: ValueEditHandler<Double>,
) = Column(Grow.Std, scopeName = label) {
    menuRow {
        Text(label) {
            modifier
                .width(Grow.Std)
                .alignY(AlignmentY.Center)
        }
        doubleTextField(value, precision, valueWidth, 0.0, min, max, editHandler)
    }
    menuRow {
        var dragStartValue by remember(value)
        Slider(value.toFloat(), min.toFloat(), max.toFloat()) {
            defaultSliderStyle()
            modifier
                .width(Grow.Std)
                .onChange { editHandler.onEdit(it.toDouble()) }
                .onDragEnd { editHandler.onEditEnd(dragStartValue, value) }
                .onDragStart {
                    editHandler.onEditStart(value)
                    dragStartValue = value
                }
        }
    }
}

fun UiScope.labeledCheckbox(
    label: String,
    state: Boolean,
    onToggle: (Boolean) -> Unit
) = menuRow {
    Text(label) {
        modifier
            .width(Grow.Std)
            .alignY(AlignmentY.Center)
            .onClick {
                onToggle(!state)
            }
    }

    Checkbox(state) {
        modifier
            .size(FitContent, sizes.lineHeight)
            .onToggle {
                onToggle(it)
            }
    }
}

fun UiScope.labeledSwitch(
    label: String,
    state: Boolean,
    onToggle: (Boolean) -> Unit
) = menuRow {
    Text(label) {
        modifier
            .width(Grow.Std)
            .alignY(AlignmentY.Center)
            .onClick {
                onToggle(!state)
            }
    }

    Switch(state) {
        modifier
            .size(FitContent, sizes.lineHeight)
            .onToggle {
                onToggle(it)
            }
    }
}

fun UiScope.okButton(xAlign: AlignmentX = AlignmentX.End, onClick: (PointerEvent) -> Unit) = menuRow {
    if (xAlign != AlignmentX.Start) {
        Box(width = Grow.Std) { }
    }
    Button("OK") {
        modifier
            .size(sizes.baseSize * 2, sizes.lineHeight)
            .alignY(AlignmentY.Center)
            .onClick(onClick)
    }
    if (xAlign == AlignmentX.Center) {
        Box(width = Grow.Std) { }
    }
}

inline fun UiScope.menuRow(marginTop: Dp = sizes.smallGap, block: RowScope.() -> Unit) = Row(width = Grow.Std, height = sizes.lineHeight) {
    modifier.margin(top = marginTop)
    block()
}

fun UiScope.textureSelector(selectedTexPath: String, withNoneOption: Boolean, onSelect: (AssetItem) -> Unit) = Column {
    val textures = mutableListOf<AssetItem>()
    if (withNoneOption) {
        textures += AssetItem("None", "", AppAssetType.Texture)
    }
    textures += KoolEditor.instance.availableAssets.textureAssets

    ComboBox {
        defaultComboBoxStyle()
        modifier
            .size(sizes.baseSize * 6, sizes.lineHeight)
            .items(textures)
            .selectedIndex(textures.indexOfFirst { it.path == selectedTexPath })
            .onItemSelected {
                onSelect(textures[it])
            }
    }

    if (selectedTexPath.isNotEmpty()) {
        val tex = CachedAppAssets.getTextureWhenLoaded(selectedTexPath).use()
        Image(tex) {
            modifier
                .margin(top = sizes.gap)
                .size(sizes.baseSize * 6, sizes.baseSize * 6)
                .imageZ(UiSurface.LAYER_BACKGROUND)
        }
    }
}

fun ColumnScope.menuDivider(marginTop: Dp = sizes.smallGap, marginBottom: Dp = Dp.ZERO) {
    divider(colors.secondaryVariantAlpha(0.75f), marginTop = marginTop, marginBottom = marginBottom)
}

fun ButtonScope.defaultButtonStyle() {
    modifier.colors(buttonColor = colors.elevatedComponentBg, buttonHoverColor = colors.elevatedComponentBgHovered)
}

fun ComboBoxScope.defaultComboBoxStyle() {
    modifier
        .clearWheelCallbacks()
        .colors(
            textBackgroundColor = colors.componentBg,
            textBackgroundHoverColor = colors.componentBgHovered,
            expanderColor = colors.elevatedComponentBg,
            expanderHoverColor = colors.elevatedComponentBgHovered
        )
        .popupColors(
            popupBackgroundColor = UiColors.bgMid,
            popupHoverColor = colors.componentBgHovered,
            popupHoverTextColor = colors.onBackground,
            popupBorderColor = colors.secondaryVariant
        )
}

fun SliderScope.defaultSliderStyle() {
    modifier.colors(trackColor = colors.elevatedComponentBg, trackColorActive = colors.elevatedComponentBgHovered)
}

fun TextFieldScope.defaultTextfieldStyle() {
    val bgColor = if (isFocused.use()) colors.componentBgHovered else colors.componentBg
    modifier
        .colors(lineColor = null, lineColorFocused = null)
        .background(RoundRectBackground(bgColor, sizes.smallTextFieldPadding))
        .padding(horizontal = sizes.gap, vertical = sizes.smallTextFieldPadding)
}

fun UiScope.defaultPopupStyle(layout: Layout = ColumnLayout) {
    modifier
        .background(RoundRectBackground(UiColors.bgMid, sizes.smallGap))
        .border(RoundRectBorder(colors.secondaryVariant, sizes.smallGap, sizes.borderWidth))
        .layout(layout)
        .padding(sizes.gap)
}

fun interface ValueEditHandler<T> {
    fun onEditStart(startValue: T) { }
    fun onEdit(value: T)
    fun onEditEnd(startValue: T, endValue: T) = onEdit(endValue)
}

fun interface ActionValueEditHandler<T> : ValueEditHandler<T> {
    override fun onEdit(value: T) = makeEditAction(value, value).apply()
    override fun onEditEnd(startValue: T, endValue: T) = EditorActions.applyAction(makeEditAction(startValue, endValue))

    fun makeEditAction(undoValue: T, applyValue: T) : EditorAction
}
