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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventAdapterService;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class EvalInsertCoercionAvro implements SelectExprProcessor, SelectExprProcessorForge {

    private EventType resultEventType;
    private EventAdapterService eventAdapterService;

    public EvalInsertCoercionAvro(EventType resultEventType, EventAdapterService eventAdapterService) {
        this.resultEventType = resultEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        return eventAdapterService.adapterForTypedAvro(eventsPerStream[0].getUnderlying(), resultEventType);
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        return this;
    }

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        CodegenExpression bean = exprDotMethod(arrayAtIndex(params.passEPS(), constant(0)), "getUnderlying");
        return exprDotMethod(CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), "adapterForTypedAvro", bean, CodegenExpressionBuilder.member(memberResultEventType.getMemberId()));
    }
}
