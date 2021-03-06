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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.interval.IntervalForge;

public class DTLocalZDTIntervalForge extends DTLocalForgeIntervalBase {

    public DTLocalZDTIntervalForge(IntervalForge intervalForge) {
        super(intervalForge);
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalZDTIntervalEval(intervalForge.getOp());
    }

    public DTLocalEvaluatorIntervalComp makeEvaluatorComp() {
        return new DTLocalZDTIntervalEval(intervalForge.getOp());
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenParamSetExprPremade params, CodegenContext context) {
        return DTLocalZDTIntervalEval.codegen(this, inner, innerType, params, context);
    }

    public CodegenExpression codegen(CodegenExpressionRef start, CodegenExpressionRef end, CodegenParamSetExprPremade params, CodegenContext context) {
        return DTLocalZDTIntervalEval.codegen(this, start, end, params, context);
    }
}
