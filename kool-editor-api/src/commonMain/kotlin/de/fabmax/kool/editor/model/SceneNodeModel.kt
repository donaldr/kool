package de.fabmax.kool.editor.model

import de.fabmax.kool.KoolSystem
import de.fabmax.kool.editor.components.MeshComponent
import de.fabmax.kool.editor.components.ModelComponent
import de.fabmax.kool.editor.components.TransformComponent
import de.fabmax.kool.editor.data.SceneNodeData
import de.fabmax.kool.editor.data.TransformComponentData
import de.fabmax.kool.editor.data.TransformData
import de.fabmax.kool.scene.Model
import de.fabmax.kool.scene.Node

class SceneNodeModel(nodeData: SceneNodeData, val scene: SceneModel) : EditorNodeModel(nodeData) {

    override val node: Node
        get() = created ?: throw IllegalStateException("Node was not yet created")

    private var created: Node? = null
    override val isCreated: Boolean
        get() = created != null

    val transform = getOrPutComponent { TransformComponent(TransformComponentData(TransformData.IDENTITY)) }

    private val modelComponentModels = mutableMapOf<ModelComponent, Model>()

    init {
        nameState.onChange { created?.name = it }
    }

    override fun addChild(child: SceneNodeModel) {
        nodeData.childNodeIds += child.nodeId
        node.addNode(child.node)
    }

    override fun removeChild(child: SceneNodeModel) {
        nodeData.childNodeIds -= child.nodeId
        node.removeNode(child.node)
    }

    override suspend fun createComponents() {
        disposeCreatedNode()

        super.createComponents()

        val meshComponents = getComponents<MeshComponent>()
        val modelComponents = getComponents<ModelComponent>()
        val createdNode: Node = meshComponents.firstOrNull()?.mesh ?: modelComponents.firstOrNull()?.model ?: Node()

        for (meshComponent in meshComponents) {
            if (meshComponent.mesh !== createdNode) {
                createdNode.addNode(meshComponent.mesh)
            }
        }
        for (modelComponent in modelComponents) {
            if (modelComponent.model !== createdNode) {
                createdNode.addNode(modelComponent.model)
            }
        }

        createdNode.name = nodeData.name
        created = createdNode
    }

    fun disposeCreatedNode() {
        created?.dispose(KoolSystem.requireContext())
        created = null
    }

    fun replaceCreatedNode(newNode: Node) {
        created?.let {
            it.parent?.let {  parent ->
                val ndIdx = parent.children.indexOf(it)
                parent.removeNode(it)
                parent.addNode(newNode, ndIdx)
            }
            scene.nodesToNodeModels -= it
            it.dispose(KoolSystem.requireContext())

            newNode.onUpdate += it.onUpdate
            newNode.onDispose += it.onDispose
            it.onUpdate.clear()
            it.onDispose.clear()
        }
        transform.transformState.value.toTransform(newNode.transform)
        created = newNode
        scene.nodesToNodeModels[newNode] = this
    }
}