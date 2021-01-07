package de.fabmax.kool.physics.shapes

import de.fabmax.kool.math.MutableVec4f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.util.BoundingBox
import physx.*

actual class SphereShape actual constructor(radius: Float) : CommonSphereShape(radius), CollisionShape {

    override fun getAabb(result: BoundingBox): BoundingBox {
        return result.set(-radius, -radius, -radius, radius, radius, radius)
    }

    override fun getBoundingSphere(result: MutableVec4f): MutableVec4f = result.set(Vec3f.ZERO, radius)

    override fun attachTo(actor: PxRigidActor, material: PxMaterial, flags: PxShapeFlags, collisionFilter: PxFilterData): PxShape {
        val geometry = PhysX.PxSphereGeometry(radius)
        val shape = PhysX.physics.createShape(geometry, material, true, flags)
        shape.setSimulationFilterData(collisionFilter)
        actor.attachShape(shape)
        return shape
    }
}