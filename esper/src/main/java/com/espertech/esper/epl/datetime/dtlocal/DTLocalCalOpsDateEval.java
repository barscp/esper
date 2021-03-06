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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsCalendarCodegen;

public class DTLocalCalOpsDateEval extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {

    private final TimeZone timeZone;

    public DTLocalCalOpsDateEval(List<CalendarOp> calendarOps, TimeZone timeZone) {
        super(calendarOps);
        this.timeZone = timeZone;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Date dateValue = (Date) target;
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(dateValue.getTime());

        DTLocalUtil.evaluateCalOpsCalendar(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);

        return cal.getTime();
    }

    public static CodegenExpression codegen(DTLocalCalOpsDateForge forge, CodegenExpression inner, Class innerType, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, forge.timeZone);
        CodegenBlock block = context.addMethod(Date.class, DTLocalCalOpsDateEval.class).add(innerType, "target").add(params).begin()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", member(tz.getMemberId())))
                .expression(exprDotMethod(ref("cal"), "setTimeInMillis", exprDotMethod(ref("target"), "getTime")));
        evaluateCalOpsCalendarCodegen(block, forge.calendarForges, ref("cal"), params, context);
        CodegenMethodId method = block.methodReturn(exprDotMethod(ref("cal"), "getTime"));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }
}
