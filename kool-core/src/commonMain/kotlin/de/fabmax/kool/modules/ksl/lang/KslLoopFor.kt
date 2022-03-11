package de.fabmax.kool.modules.ksl.lang


class KslLoopFor<S>(
    val loopVar: KslVarScalar<S>,
    val whileExpression: KslScalarExpression<KslTypeBool1>,
    val incExpr: KslScalarExpression<S>,
    parentScope: KslScopeBuilder)
    : KslStatement("for", parentScope), KslLoop where S: KslNumericType, S: KslScalar {

    val body = KslScopeBuilder(this, parentScope, parentScope.parentStage)

    init {
        addExpressionDependencies(whileExpression)
        addExpressionDependencies(incExpr)
        childScopes += body
    }

    override fun toPseudoCode(): String {
        val str = StringBuilder("for (${loopVar.toPseudoCode()}; ${whileExpression.toPseudoCode()}; ${loopVar.stateName} += ${incExpr.toPseudoCode()}) // ${dependenciesAndMutationsToString()}\n")
        str.append(body.toPseudoCode())
        return str.toString()
    }
}
