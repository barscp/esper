/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.JavaClassHelper;

public class SelectExprProcessorEnumerationSingleToCollForge implements ExprForge {
    protected final ExprEnumerationForge enumerationForge;
    private final EventType targetType;

    public SelectExprProcessorEnumerationSingleToCollForge(ExprEnumerationForge enumerationForge, EventType targetType) {
        this.enumerationForge = enumerationForge;
        this.targetType = targetType;
    }

    public ExprEvaluator getExprEvaluator() {
        return new SelectExprProcessorEnumerationSingleToCollEval(this, enumerationForge.getExprEvaluatorEnumeration());
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return SelectExprProcessorEnumerationSingleToCollEval.codegen(this, params, context);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
    }

    public ExprNodeRenderable getForgeRenderable() {
        return enumerationForge.getForgeRenderable();
    }
}
