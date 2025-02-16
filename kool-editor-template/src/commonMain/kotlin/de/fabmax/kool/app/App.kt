package de.fabmax.kool.app

import de.fabmax.kool.KoolContext
import de.fabmax.kool.editor.api.AppState
import de.fabmax.kool.editor.api.EditorAwareApp
import de.fabmax.kool.editor.model.EditorProject
import de.fabmax.kool.scene.defaultOrbitCamera

class App : EditorAwareApp {
    override suspend fun startApp(projectModel: EditorProject, ctx: KoolContext) {
        projectModel.create()

        if (!AppState.isInEditor) {
            // fixme: camera not yet included in project model, add a default one
            projectModel.getCreatedScenes().forEach {
                it.node.defaultOrbitCamera()
            }
        }
    }
}