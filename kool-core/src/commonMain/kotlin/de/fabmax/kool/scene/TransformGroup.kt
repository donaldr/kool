package de.fabmax.kool.scene

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.*
import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.util.BoundingBox

/**
 * @author fabmax
 */

fun transformGroup(name: String? = null, block: TransformGroup.() -> Unit): TransformGroup {
    val tg = TransformGroup(name)
    tg.block()
    return tg
}

open class TransformGroup(name: String? = null) : Group(name) {
    val transform = Mat4d()

    protected val invTransform = Mat4d()
    protected var isIdentity = false
    protected var isDirty = false

    private val tmpTransformVec = MutableVec3f()
    private val tmpBounds = BoundingBox()

    init {
        setIdentity()
    }

    protected fun checkInverse() {
        if (isDirty) {
            transform.invert(invTransform)
            isDirty = false
        }
    }

    fun setDirty() {
        isDirty = true
        isIdentity = false
    }

    override fun update(ctx: KoolContext) {
        // apply transformation
        val wasIdentity = isIdentity
        if (!wasIdentity) {
            ctx.mvpState.modelMatrix.push()
            ctx.mvpState.modelMatrix.mul(transform)
            ctx.mvpState.update(ctx)
        }

        // compute global position and size based on group bounds and current model transform
        super.update(ctx)

        // Something to think about: In case transform changes during preRender (e.g. because a onPreRender listener
        // is animating this transform group) the computed bounds will not match the actual bounds during
        // render (because of the changed transform matrix). This could be solved by caching the transform used now
        // and reuse it in render, but that would also introduce an delay of one frame before changed transform
        // becomes visible. In most cases mismatch between bounds shouldn't be harmful.

        // transform group bounds
        if (!bounds.isEmpty && !wasIdentity) {
            tmpBounds.clear()
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.min.x, bounds.min.y, bounds.min.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.min.x, bounds.min.y, bounds.max.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.min.x, bounds.max.y, bounds.min.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.min.x, bounds.max.y, bounds.max.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.max.x, bounds.min.y, bounds.min.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.max.x, bounds.min.y, bounds.max.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.max.x, bounds.max.y, bounds.min.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.max.x, bounds.max.y, bounds.max.z), 1f))
            bounds.set(tmpBounds)
        }

        // clear transformation
        if (!wasIdentity) {
            ctx.mvpState.modelMatrix.pop()
            ctx.mvpState.update(ctx)
        }
    }

    override fun collectDrawCommands(renderPass: RenderPass, ctx: KoolContext) {
        if (isVisible) {
            // apply transformation
            val wasIdentity = isIdentity
            if (!wasIdentity) {
                ctx.mvpState.modelMatrix.push()
                ctx.mvpState.modelMatrix.mul(transform)
                ctx.mvpState.update(ctx)
            }

            // draw all child nodes
            super.collectDrawCommands(renderPass, ctx)

            // clear transformation
            if (!wasIdentity) {
                ctx.mvpState.modelMatrix.pop()
                ctx.mvpState.update(ctx)
            }
        }
    }

    override fun rayTest(test: RayTest) {
        if (!isIdentity) {
            // transform ray to local coordinates
            checkInverse()
            test.transformBy(invTransform)
        }
        super.rayTest(test)
        if (!isIdentity) {
            // transform ray back to previous coordinates
            test.transformBy(transform)
        }
    }

    fun getTransform(result: Mat4f): Mat4f = result.set(transform)

    fun getInverseTransform(result: Mat4f): Mat4f {
        checkInverse()
        return result.set(invTransform)
    }

    fun translate(t: Vec3f) = translate(t.x, t.y, t.z)

    fun translate(tx: Float, ty: Float, tz: Float) = translate(tx.toDouble(), ty.toDouble(), tz.toDouble())

    fun translate(t: Vec3d) = translate(t.x, t.y, t.z)

    fun translate(tx: Double, ty: Double, tz: Double): TransformGroup {
        transform.translate(tx, ty, tz)
        setDirty()
        return this
    }

    fun rotate(angleDeg: Float, axis: Vec3f) = rotate(angleDeg, axis.x, axis.y, axis.z)

    fun rotate(angleDeg: Float, axX: Float, axY: Float, axZ: Float) =
            rotate(angleDeg.toDouble(), axX.toDouble(), axY.toDouble(), axZ.toDouble())

    fun rotate(angleDeg: Double, axis: Vec3d) = rotate(angleDeg, axis.x, axis.y, axis.z)

    fun rotate(angleDeg: Double, axX: Double, axY: Double, axZ: Double): TransformGroup {
        transform.rotate(angleDeg, axX, axY, axZ)
        setDirty()
        return this
    }

    fun scale(sx: Float, sy: Float, sz: Float) = scale(sx.toDouble(), sy.toDouble(), sz.toDouble())

    fun scale(sx: Double, sy: Double, sz: Double): TransformGroup {
        transform.scale(sx, sy, sz)
        setDirty()
        return this
    }

    fun mul(mat: Mat4d): TransformGroup {
        transform.mul(mat)
        setDirty()
        return this
    }

    fun set(mat: Mat4f): TransformGroup {
        transform.set(mat)
        setDirty()
        return this
    }

    fun set(mat: Mat4d): TransformGroup {
        transform.set(mat)
        setDirty()
        return this
    }

    fun setIdentity(): TransformGroup {
        transform.setIdentity()
        invTransform.setIdentity()
        isDirty = false
        isIdentity = true
        return this
    }
}