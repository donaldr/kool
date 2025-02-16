package de.fabmax.kool.editor.actions

import de.fabmax.kool.editor.components.ScriptComponent
import de.fabmax.kool.editor.data.PropertyValue

class SetScriptPropertyAction(
    val scriptComponent: ScriptComponent,
    val propName: String,
    val undoValue: PropertyValue,
    val newValue: PropertyValue,
    val setPropertyBlock: (PropertyValue) -> Unit
) : EditorAction {

    override fun apply() {
        setPropertyBlock(newValue)
    }

    override fun undo() {
        setPropertyBlock(undoValue)
    }

}