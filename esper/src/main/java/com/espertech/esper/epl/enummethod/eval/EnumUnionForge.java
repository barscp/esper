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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.epl.expression.core.ExprEnumerationForge;

public class EnumUnionForge implements EnumForge {

    private final int numStreams;
    protected final ExprEnumerationForge evaluatorForge;
    protected final boolean scalar;

    public EnumUnionForge(int numStreams, ExprEnumerationForge evaluatorForge, boolean scalar) {
        this.numStreams = numStreams;
        this.evaluatorForge = evaluatorForge;
        this.scalar = scalar;
    }

    public int getStreamNumSize() {
        return numStreams;
    }

    public EnumEval getEnumEvaluator() {
        return new EnumUnionForgeEval(this, evaluatorForge.getExprEvaluatorEnumeration());
    }

    public CodegenExpression codegen(CodegenParamSetEnumMethodNonPremade premade, CodegenContext context) {
        return EnumUnionForgeEval.codegen(this, premade, context);
    }
}
