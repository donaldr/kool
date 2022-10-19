package de.fabmax.kool.demo.uidemo

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color

class BasicUiWindow(val uiDemo: UiDemo) : UiDemo.DemoWindow {

    private val windowState = WindowState().apply { setWindowSize(Dp(500f), Dp(700f)) }

    override val windowSurface: UiSurface = Window(windowState, name = "Demo Window") {
        surface.sizes = uiDemo.selectedUiSize.use()
        surface.colors = uiDemo.selectedColors.use()

        TitleBar(onCloseAction = { uiDemo.closeWindow(this@BasicUiWindow, it.ctx) })
        WindowContent()
    }
    override val windowScope: WindowScope = windowSurface.windowScope!!

    fun UiScope.WindowContent() = Column(Grow.Std, Grow.Std) {
        modifier
            .padding(horizontal = sizes.gap, vertical = sizes.largeGap)

        Row {
            modifier.margin(bottom = sizes.smallGap)
            val clickCnt = weakRememberState(0)
            Button("A regular button: clicked ${clickCnt.use()} times") {
                modifier
                    .onClick { clickCnt.value += 1 }
            }
        }

        Row {
            modifier.margin(bottom = sizes.smallGap)

            val checkboxState = weakRememberState(false)
            val radioButtonState = weakRememberState(false)
            val switchState = weakRememberState(false)

            Text("Checkbox") { modifier.alignY(AlignmentY.Center) }
            Checkbox(checkboxState.use()) {
                modifier.margin(sizes.gap).onToggle { checkboxState.set(it) }
            }

            Text("Radio Button") { modifier.alignY(AlignmentY.Center).margin(start = sizes.largeGap) }
            RadioButton(radioButtonState.use()) {
                modifier.margin(sizes.gap).onToggle { radioButtonState.set(it) }
            }

            Text("Switch") { modifier.alignY(AlignmentY.Center).margin(start = sizes.largeGap) }
            Switch(switchState.use()) {
                modifier.margin(sizes.gap).onToggle { switchState.set(it) }
            }
        }

        Row {
            modifier.margin(bottom = sizes.smallGap)
            Text("Text fields") { modifier.alignY(AlignmentY.Center).margin(end = sizes.gap) }
            val text1 = weakRememberState("")
            val text2 = weakRememberState("")
            TextField(text1.use()) {
                modifier
                    .width(150.dp)
                    .hint("Ctrl+C to copy")
                    .onChange { text1.set(it) }
            }
            TextField(text2.use()) {
                modifier
                    .width(150.dp)
                    .hint("Ctrl+V to paste")
                    .margin(start = sizes.largeGap)
                    .onChange { text2.set(it) }
            }
        }

        Row {
            modifier.margin(bottom = sizes.smallGap)
            Text("Slider") { modifier.alignY(AlignmentY.Center) }
            val sliderValue = weakRememberState(50f)
            Slider(sliderValue.use(), 0f, 100f) {
                modifier
                    .margin(sizes.gap)
                    .width(sizes.largeGap * 6f)
                    .onChange { sliderValue.set(it) }
            }
            Text("${sliderValue.use().toInt()}") { modifier.alignY(AlignmentY.Center).margin(start = sizes.gap) }
        }

        Row {
            Text("Combo-box") { modifier.alignY(AlignmentY.Center) }

            val items = weakRemember { List(8) { "Item ${it + 1}" } }
            val selectedIndex = weakRememberState(0)
            ComboBox {
                modifier
                    .margin(start = sizes.gap)
                    .width(sizes.largeGap * 6f)
                    .items(items)
                    .selectedIndex(selectedIndex.use())
                    .onItemSelected { selectedIndex.set(it) }
            }
        }

        divider(colors.secondaryVariant.withAlpha(0.5f), marginTop = sizes.largeGap, marginBottom = sizes.gap)
        Text("A scroll area:") {
            modifier.margin(bottom = sizes.gap)
        }
        ScrollArea(
            vScrollbarModifier = {
                it.colors(
                    trackColor = colors.secondaryVariant.withAlpha(0.1f),
                    trackHoverColor = colors.secondaryVariant.withAlpha(0.2f)
                )
            },
            hScrollbarModifier = {
                it.colors(
                    trackColor = colors.secondaryVariant.withAlpha(0.1f),
                    trackHoverColor = colors.secondaryVariant.withAlpha(0.2f)
                )
            }
        ) {
            Column {
                Text("Text with two lines in a slightly larger font:\nThe second line is a tiny bit longer than the first one.") {
                    modifier
                        .margin(sizes.smallGap)
                        .font(sizes.largeText)
                }
                Row {
                    fun ImageScope.imageLabel(text: String) {
                        Text(text) {
                            modifier
                                .textColor(Color.WHITE)
                                .align(AlignmentX.Start, AlignmentY.Bottom)
                                .zLayer(UiSurface.LAYER_FLOATING)
                                .backgroundColor(Color.BLACK.withAlpha(0.5f))
                        }
                    }
                    Image {
                        modifier
                            .margin(sizes.smallGap)
                            .image(uiDemo.exampleImage)
                            .imageZ(UiSurface.LAYER_BACKGROUND)
                            .imageSize(ImageSize.FixedScale(0.3f))
                            .border(RectBorder(Color.RED, 1.dp))
                            .size(FitContent, FitContent)
                        imageLabel("sz: fit, img: fixedScale(0.3)")
                    }
                    Image {
                        modifier
                            .margin(sizes.smallGap)
                            .image(uiDemo.exampleImage)
                            .imageZ(UiSurface.LAYER_BACKGROUND)
                            .size(400.dp, 300.dp)
                            .imageSize(ImageSize.FitContent)
                            .border(RectBorder(Color.RED, 1.dp))
                        imageLabel("sz: 400x300, img: fit")
                    }
                    Image {
                        modifier
                            .margin(sizes.smallGap)
                            .image(uiDemo.exampleImage)
                            .imageZ(UiSurface.LAYER_BACKGROUND)
                            .size(400.dp, 300.dp)
                            .imageSize(ImageSize.ZoomContent)
                            .border(RectBorder(Color.RED, 1.dp))
                        imageLabel("sz: 400x300, img: zoom")
                    }
                    Image {
                        modifier
                            .margin(sizes.smallGap)
                            .image(uiDemo.exampleImage)
                            .imageZ(UiSurface.LAYER_BACKGROUND)
                            .size(400.dp, 300.dp)
                            .imageSize(ImageSize.Stretch)
                            .border(RectBorder(Color.RED, 1.dp))
                        imageLabel("sz: 400x300, img: stretch")
                    }
                }
                Row {
                    for (r in 0..270 step 90) {
                        Text("Another text with rotation: $r") {
                            modifier
                                .margin(sizes.smallGap)
                                .padding(sizes.largeGap)
                                .textRotation(r.toFloat())
                                .border(RoundRectBorder(colors.primaryVariant, sizes.gap, 2.dp, 6.dp))
                        }
                    }
                }
            }
        }

        divider(colors.secondaryVariant.withAlpha(0.5f), marginTop = sizes.largeGap, marginBottom = sizes.gap)
        Text("A longer list, click items to delete them:") {
            modifier.margin(bottom = sizes.gap)
        }
        val listItems = weakRemember { mutableStateListOf<String>().apply { for (i in 1..500) { add("Item $i") } } }
        val hoveredItemIndex = weakRememberState(-1)
        LazyList(
            vScrollbarModifier = {
                it.colors(
                    trackColor = colors.secondaryVariant.withAlpha(0.1f),
                    trackHoverColor = colors.secondaryVariant.withAlpha(0.2f)
                )
            }
        ) {
            itemsIndexed(listItems) { i, item ->
                val isHovered = i == hoveredItemIndex.use()
                val bgColor = if (isHovered) {
                    colors.secondary.withAlpha(0.5f)
                } else if (i % 2 == 0) {
                    val bg = if (colors.isLight) Color.BLACK else colors.secondary
                    bg.withAlpha(0.05f)
                } else {
                    null
                }
                val textColor = if (isHovered) colors.onPrimary else colors.onBackground
                val isLarge = (i / 10) % 2 != 0
                val txt = if (isLarge) "$item [large]" else item

                Text(txt) {
                    modifier
                        .textColor(textColor)
                        .textAlignY(AlignmentY.Center)
                        .padding(sizes.smallGap)
                        .width(Grow.Std)
                        .height(if (isLarge) 64.dp else FitContent)
                        .backgroundColor(bgColor)
                        .onHover { hoveredItemIndex.set(i) }
                        .onExit { hoveredItemIndex.set(-1) }
                        .onClick {
                            listItems.remove(item)
                        }
                }
            }
        }
    }
}