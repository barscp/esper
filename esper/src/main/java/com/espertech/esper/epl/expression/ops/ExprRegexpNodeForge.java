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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;

public abstract class ExprRegexpNodeForge implements ExprForge {
    private final ExprRegexpNode parent;
    private final boolean isNumericValue;

    public abstract ExprEvaluator getExprEvaluator();
    public abstract CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context);

    public ExprRegexpNodeForge(ExprRegexpNode parent, boolean isNumericValue) {
        this.parent = parent;
        this.isNumericValue = isNumericValue;
    }

    public ExprRegexpNode getForgeRenderable() {
        return parent;
    }

    public boolean isNumericValue() {
        return isNumericValue;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }
}
