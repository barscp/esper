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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;

public class ExprMinMaxRowNodeForge implements ExprForge {
    private final ExprMinMaxRowNode parent;
    private final Class resultType;

    public ExprMinMaxRowNodeForge(ExprMinMaxRowNode parent, Class resultType) {
        this.parent = parent;
        this.resultType = resultType;
    }

    public ExprMinMaxRowNode getForgeRenderable() {
        return parent;
    }

    public ExprEvaluator getExprEvaluator() {
        ExprEvaluator[] evaluators = ExprNodeUtility.getEvaluatorsNoCompile(parent.getChildNodes());
        return new ExprMinMaxRowNodeForgeEval(this, evaluators, ExprNodeUtility.getForges(parent.getChildNodes()));
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        return ExprMinMaxRowNodeForgeEval.codegen(this, context, params);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return resultType;
    }
}
