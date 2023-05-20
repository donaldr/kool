package de.fabmax.kool.editor.actions

import de.fabmax.kool.editor.data.SceneBackgroundData
import de.fabmax.kool.editor.model.SceneModel
import de.fabmax.kool.editor.model.ecs.UpdateSceneBackgroundComponent

class SetBackgroundAction(
    val sceneModel: SceneModel,
    val oldBackground: SceneBackgroundData,
    val newBackground: SceneBackgroundData
) : EditorAction {

    override fun apply() {
        sceneModel.sceneBackground.backgroundState.set(newBackground)
        UpdateSceneBackgroundComponent.updateBackground(sceneModel)
    }

    override fun undo() {
        sceneModel.sceneBackground.backgroundState.set(oldBackground)
        UpdateSceneBackgroundComponent.updateBackground(sceneModel)
    }
}