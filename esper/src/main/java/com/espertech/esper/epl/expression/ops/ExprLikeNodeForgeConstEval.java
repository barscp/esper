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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.LikeUtil;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprLikeNodeForgeConstEval implements ExprEvaluator {
    private final ExprLikeNodeForgeConst forge;
    private final ExprEvaluator lhsEval;

    ExprLikeNodeForgeConstEval(ExprLikeNodeForgeConst forge, ExprEvaluator lhsEval) {
        this.forge = forge;
        this.lhsEval = lhsEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprLike(forge.getForgeRenderable());
        }

        Object value = lhsEval.evaluate(eventsPerStream, isNewData, context);

        if (value == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprLike(null);
            }
            return null;
        }

        if (forge.isNumericValue()) {
            value = value.toString();
        }

        boolean result = forge.getForgeRenderable().isNot() ^ forge.getLikeUtil().compare((String) value);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprLike(result);
        }
        return result;
    }

    public static CodegenMethodId codegen(ExprLikeNodeForgeConst forge, ExprNode lhs, CodegenContext context, CodegenParamSetExprPremade params) {
        CodegenMember mLikeUtil = context.makeAddMember(LikeUtil.class, forge.getLikeUtil());

        if (!forge.isNumericValue()) {
            return context.addMethod(Boolean.class, ExprLikeNodeForgeConstEval.class).add(params).begin()
                    .declareVar(String.class, "value", lhs.getForge().evaluateCodegen(params, context))
                    .ifRefNullReturnNull("value")
                    .methodReturn(getLikeCode(forge, member(mLikeUtil.getMemberId()), ref("value")));
        }
        return context.addMethod(Boolean.class, ExprLikeNodeForgeConstEval.class).add(params).begin()
                .declareVar(Object.class, "value", lhs.getForge().evaluateCodegen(params, context))
                .ifRefNullReturnNull("value")
                .methodReturn(getLikeCode(forge, member(mLikeUtil.getMemberId()), exprDotMethod(ref("value"), "toString")));
    }

    static CodegenExpression getLikeCode(ExprLikeNodeForge forge, CodegenExpression refLike, CodegenExpression stringExpr) {
        CodegenExpression eval = exprDotMethod(refLike, "compare", stringExpr);
        return !forge.getForgeRenderable().isNot() ? eval : not(eval);
    }
}
