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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the EXISTS(property) function in an expression tree.
 */
public class ExprPropertyExistsNode extends ExprNodeBase implements ExprEvaluator, ExprForge {
    private ExprIdentNode identNode;
    private static final long serialVersionUID = -6304444201237275628L;

    /**
     * Ctor.
     */
    public ExprPropertyExistsNode() {
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 1) {
            throw new ExprValidationException("Exists function node must have exactly 1 child node");
        }

        if (!(this.getChildNodes()[0] instanceof ExprIdentNode)) {
            throw new ExprValidationException("Exists function expects an property value expression as the child node");
        }

        identNode = (ExprIdentNode) this.getChildNodes()[0];
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprPropExists(this);
        }
        boolean exists = identNode.getExprEvaluatorIdent().evaluatePropertyExists(eventsPerStream, isNewData);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprPropExists(exists);
        }
        return exists;
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(Boolean.class, this.getClass()).add(params).begin()
                .declareVar(EventBean.class, "event", arrayAtIndex(params.passEPS(), constant(identNode.getStreamId())))
                .ifRefNullReturnNull("event")
                .methodReturn(identNode.getExprEvaluatorIdent().getGetter().eventBeanExistsCodegen(ref("event"), context));
        return localMethodBuild(method).passAll(params).call();
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("exists(");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprPropertyExistsNode)) {
            return false;
        }

        return true;
    }
}
