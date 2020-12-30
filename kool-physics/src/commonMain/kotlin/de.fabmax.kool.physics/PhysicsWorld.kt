package de.fabmax.kool.physics

import de.fabmax.kool.math.Vec3f

expect class PhysicsWorld() : CommonPhysicsWorld {

    var gravity: Vec3f

}

abstract class CommonPhysicsWorld {
    var physicsTime = 0.0

    private val mutBodies = mutableListOf<RigidBody>()
    val bodies: List<RigidBody>
        get() = mutBodies

    fun addRigidBody(rigidBody: RigidBody) {
        mutBodies += rigidBody
        addRigidBodyImpl(rigidBody)
    }

    fun removeRigidBody(rigidBody: RigidBody) {
        mutBodies -= rigidBody
        removeRigidBodyImpl(rigidBody)
    }

    fun stepPhysics(timeStep: Float, maxSubSteps: Int = 10, fixedStep: Float = 1f / 60f): Float {
        val stopTime = physicsTime + timeStep
        var steps = 0
        while (physicsTime < stopTime && ++steps < maxSubSteps) {
            singleStepPhysics(fixedStep)
            physicsTime += fixedStep
        }
        return steps * fixedStep
    }

    fun singleStepPhysics(timeStep: Float) {
        singleStepPhysicsImpl(timeStep)
        for (i in mutBodies.indices) {
            mutBodies[i].fixedUpdate(timeStep)
        }
    }

    protected abstract fun addRigidBodyImpl(rigidBody: RigidBody)
    protected abstract fun removeRigidBodyImpl(rigidBody: RigidBody)

    protected abstract fun singleStepPhysicsImpl(timeStep: Float)
}