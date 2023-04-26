package de.fabmax.kool.modules.ui2

import de.fabmax.kool.util.Color
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun UiScope.ScrollArea(
    width: Dimension = Grow.Std,
    height: Dimension = Grow.Std,
    withVerticalScrollbar: Boolean = true,
    withHorizontalScrollbar: Boolean = true,
    scrollbarColor: Color? = null,
    containerModifier: ((UiModifier) -> Unit)? = null,
    vScrollbarModifier: ((ScrollbarModifier) -> Unit)? = null,
    hScrollbarModifier: ((ScrollbarModifier) -> Unit)? = null,
    state: ScrollState = rememberScrollState(),
    scopeName: String? = null,
    block: ScrollPaneScope.() -> Unit
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    Box(scopeName = scopeName) {
        modifier
            .width(width)
            .height(height)
            .backgroundColor(colors.backgroundVariant)
            .onWheelX { state.scrollDpX(it.pointer.deltaScrollX.toFloat() * -20f) }
            .onWheelY { state.scrollDpY(it.pointer.deltaScrollY.toFloat() * -50f) }

        containerModifier?.invoke(modifier)

        ScrollPane(state) {
            block()
        }
        if (withVerticalScrollbar) {
            VerticalScrollbar {
                modifier
                    .relativeBarPos(state.relativeBarPosY)
                    .relativeBarLen(state.relativeBarLenY)
                    .onChange { state.scrollRelativeY(it) }
                scrollbarColor?.let { modifier.colors(it) }
                vScrollbarModifier?.invoke(modifier)
            }
        }
        if (withHorizontalScrollbar) {
            HorizontalScrollbar {
                modifier
                    .relativeBarPos(state.relativeBarPosX)
                    .relativeBarLen(state.relativeBarLenX)
                    .onChange { state.scrollRelativeX(it) }
                scrollbarColor?.let { modifier.colors(it) }
                hScrollbarModifier?.invoke(modifier)
            }
        }
    }
}
