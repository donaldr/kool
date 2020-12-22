package de.fabmax.kool.util

import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec3f

interface ShapeContainer {
    val shapes: MutableList<Shape>
}

inline fun ShapeContainer.multiShape(block: MultiShape.() -> Unit): MultiShape {
    val shape = MultiShape()
    shapes += shape
    shape.block()
    return shape
}

inline fun ShapeContainer.simpleShape(isClosed: Boolean, block: SimpleShape.() -> Unit): SimpleShape {
    val shape = SimpleShape(isClosed)
    shapes += shape
    shape.block()
    return shape
}

class Profile : ShapeContainer {
    override val shapes = mutableListOf<Shape>()

    fun sample(meshBuilder: MeshBuilder, connect: Boolean, inverseOrientation: Boolean) {
        shapes.forEach { it.sample(meshBuilder, connect, inverseOrientation) }
    }

    fun fillTop(meshBuilder: MeshBuilder) {
        shapes.forEach { it.fillTop(meshBuilder) }
    }

    fun fillBottom(meshBuilder: MeshBuilder) {
        shapes.forEach { it.fillBottom(meshBuilder) }
    }
}

abstract class Shape {
    abstract val positions: List<Vec3f>
    abstract val sampledVertIndices: List<Int>

    abstract fun sample(meshBuilder: MeshBuilder, connect: Boolean, inverseOrientation: Boolean)

    abstract fun fillTop(meshBuilder: MeshBuilder)
    abstract fun fillBottom(meshBuilder: MeshBuilder)
}

class SimpleShape(val isClosed: Boolean) : Shape() {
    override val positions = mutableListOf<MutableVec3f>()
    val normals = mutableListOf<MutableVec3f>()
    val texCoords = mutableListOf<MutableVec2f>()

    var color: Color? = null

    private val prevIndices = mutableListOf<Int>()
    private val vertIndices = mutableListOf<Int>()
    override val sampledVertIndices: List<Int>
        get() = vertIndices

    val nVerts: Int
        get() = positions.size

    fun getNormal(i: Int): Vec3f {
        return if (i < normals.size) normals[i] else Vec3f.ZERO
    }

    fun getTexCoord(i: Int): Vec2f {
        return if (i < texCoords.size) texCoords[i] else Vec2f.ZERO
    }

    fun xy(x: Float, y: Float) {
        positions += MutableVec3f(x, y, 0f)
    }

    fun xz(x: Float, z: Float) {
        positions += MutableVec3f(x, 0f, z)
    }

    fun uv(x: Float, y: Float) {
        texCoords += MutableVec2f(x, y)
    }

    fun normal(x: Float, y: Float, z: Float) {
        normals += MutableVec3f(x, y, z)
    }

    fun setTexCoordsX(x: Float) {
        texCoords.forEach { it.x = x }
    }

    fun setTexCoordsY(y: Float) {
        texCoords.forEach { it.y = y }
    }

    override fun sample(meshBuilder: MeshBuilder, connect: Boolean, inverseOrientation: Boolean) {
        prevIndices.clear()
        prevIndices.addAll(vertIndices)
        vertIndices.clear()

        color?.let { meshBuilder.color = it }

        positions.forEachIndexed { i, pos ->
            vertIndices += meshBuilder.vertex(pos, getNormal(i), getTexCoord(i))
        }

        if (connect) {
            connect(meshBuilder, prevIndices, inverseOrientation)
        }
    }

    fun connect(meshBuilder: MeshBuilder, otherVertIndices: List<Int>, inverseOrientation: Boolean) {
        if (otherVertIndices.isNotEmpty()) {
            for (i in 1 until otherVertIndices.size) {
                if (inverseOrientation) {
                    meshBuilder.geometry.addTriIndices(otherVertIndices[i - 1], vertIndices[i], otherVertIndices[i])
                    meshBuilder.geometry.addTriIndices(otherVertIndices[i - 1], vertIndices[i - 1], vertIndices[i])
                } else {
                    meshBuilder.geometry.addTriIndices(otherVertIndices[i - 1], otherVertIndices[i], vertIndices[i])
                    meshBuilder.geometry.addTriIndices(otherVertIndices[i - 1], vertIndices[i], vertIndices[i - 1])
                }
            }
            if (isClosed && prevIndices.isNotEmpty()) {
                if (inverseOrientation) {
                    meshBuilder.geometry.addTriIndices(otherVertIndices.last(), vertIndices.first(), otherVertIndices.first())
                    meshBuilder.geometry.addTriIndices(otherVertIndices.last(), vertIndices.last(), vertIndices.first())
                } else {
                    meshBuilder.geometry.addTriIndices(otherVertIndices.last(), otherVertIndices.first(), vertIndices.first())
                    meshBuilder.geometry.addTriIndices(otherVertIndices.last(), vertIndices.first(), vertIndices.last())
                }
            }
        }
    }

    override fun fillTop(meshBuilder: MeshBuilder) {
        val triangulated = PolyUtil.fillPolygon(positions)
        for (i in triangulated.indices.indices step 3) {
            val i1 = triangulated.indices[i]
            val i2 = triangulated.indices[i+1]
            val i3 = triangulated.indices[i+2]
            meshBuilder.geometry.addTriIndices(vertIndices[i1], vertIndices[i2], vertIndices[i3])
        }
    }

    override fun fillBottom(meshBuilder: MeshBuilder) {
        val triangulated = PolyUtil.fillPolygon(positions)
        for (i in triangulated.indices.indices step 3) {
            val i1 = triangulated.indices[i]
            val i2 = triangulated.indices[i+1]
            val i3 = triangulated.indices[i+2]
            meshBuilder.geometry.addTriIndices(vertIndices[i1], vertIndices[i3], vertIndices[i2])
        }
    }
}

class MultiShape : Shape(), ShapeContainer {
    override val shapes = mutableListOf<Shape>()
    override val positions: List<Vec3f>
        get() = shapes.flatMap { it.positions }
    override val sampledVertIndices: List<Int>
        get() = shapes.flatMap { it.sampledVertIndices }

    override fun sample(meshBuilder: MeshBuilder, connect: Boolean, inverseOrientation: Boolean) {
        shapes.forEach { it.sample(meshBuilder, connect, inverseOrientation) }
    }

    override fun fillTop(meshBuilder: MeshBuilder) {
        val joinedInds = sampledVertIndices
        val triangulated = PolyUtil.fillPolygon(positions)
        for (i in triangulated.indices.indices step 3) {
            val i1 = triangulated.indices[i]
            val i2 = triangulated.indices[i+1]
            val i3 = triangulated.indices[i+2]
            meshBuilder.geometry.addTriIndices(joinedInds[i1], joinedInds[i2], joinedInds[i3])
        }
    }

    override fun fillBottom(meshBuilder: MeshBuilder) {
        val joinedInds = sampledVertIndices
        val triangulated = PolyUtil.fillPolygon(positions)
        for (i in triangulated.indices.indices step 3) {
            val i1 = triangulated.indices[i]
            val i2 = triangulated.indices[i+1]
            val i3 = triangulated.indices[i+2]
            meshBuilder.geometry.addTriIndices(joinedInds[i1], joinedInds[i3], joinedInds[i2])
        }
    }
}
