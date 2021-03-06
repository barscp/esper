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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataInternalEventToPublic;
import com.espertech.esper.event.DecoratingEventBean;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.EventTypeSPI;

import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalSelectStreamWUnderlying extends EvalSelectStreamBaseMap implements SelectExprProcessor {

    private final List<SelectExprStreamDesc> unnamedStreams;
    private final boolean singleStreamWrapper;
    private final boolean underlyingIsFragmentEvent;
    private final int underlyingStreamNumber;
    private final EventPropertyGetterSPI underlyingPropertyEventGetter;
    private final ExprForge underlyingExprForge;
    private final TableMetadata tableMetadata;
    private final EventType[] eventTypes;

    public EvalSelectStreamWUnderlying(SelectExprForgeContext selectExprForgeContext,
                                       EventType resultEventType,
                                       List<SelectClauseStreamCompiledSpec> namedStreams,
                                       boolean usingWildcard,
                                       List<SelectExprStreamDesc> unnamedStreams,
                                       boolean singleStreamWrapper,
                                       boolean underlyingIsFragmentEvent,
                                       int underlyingStreamNumber,
                                       EventPropertyGetterSPI underlyingPropertyEventGetter,
                                       ExprForge underlyingExprForge,
                                       TableMetadata tableMetadata,
                                       EventType[] eventTypes) {
        super(selectExprForgeContext, resultEventType, namedStreams, usingWildcard);
        this.unnamedStreams = unnamedStreams;
        this.singleStreamWrapper = singleStreamWrapper;
        this.underlyingIsFragmentEvent = underlyingIsFragmentEvent;
        this.underlyingStreamNumber = underlyingStreamNumber;
        this.underlyingPropertyEventGetter = underlyingPropertyEventGetter;
        this.underlyingExprForge = underlyingExprForge;
        this.tableMetadata = tableMetadata;
        this.eventTypes = eventTypes;
    }

    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // In case of a wildcard and single stream that is itself a
        // wrapper bean, we also need to add the map properties
        if (singleStreamWrapper) {
            DecoratingEventBean wrapper = (DecoratingEventBean) eventsPerStream[0];
            if (wrapper != null) {
                Map<String, Object> map = wrapper.getDecoratingProperties();
                props.putAll(map);
            }
        }

        EventBean theEvent = null;
        if (underlyingIsFragmentEvent) {
            EventBean eventBean = eventsPerStream[underlyingStreamNumber];
            theEvent = (EventBean) eventBean.getFragment(unnamedStreams.get(0).getStreamSelected().getStreamName());
        } else if (underlyingPropertyEventGetter != null) {
            Object value = underlyingPropertyEventGetter.get(eventsPerStream[underlyingStreamNumber]);
            if (value != null) {
                theEvent = super.getContext().getEventAdapterService().adapterForBean(value);
            }
        } else if (underlyingExprForge != null) {
            Object value = underlyingExprForge.getExprEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (value != null) {
                theEvent = super.getContext().getEventAdapterService().adapterForBean(value);
            }
        } else {
            theEvent = eventsPerStream[underlyingStreamNumber];
            if (tableMetadata != null && theEvent != null) {
                theEvent = tableMetadata.getEventToPublic().convert(theEvent, eventsPerStream, isNewData, exprEvaluatorContext);
            }
        }

        // Using a wrapper bean since we cannot use the same event type else same-type filters match.
        // Wrapping it even when not adding properties is very inexpensive.
        return super.getContext().getEventAdapterService().adapterForTypedWrapper(theEvent, props, super.getResultEventType());
    }

    protected CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpression props, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(EventBean.class, EvalSelectStreamWUnderlying.class).add(Map.class, "props").add(params).begin();
        if (singleStreamWrapper) {
            block.declareVar(DecoratingEventBean.class, "wrapper", cast(DecoratingEventBean.class, arrayAtIndex(params.passEPS(), constant(0))))
                    .ifRefNotNull("wrapper")
                    .exprDotMethod(props, "putAll", exprDotMethod(ref("wrapper"), "getDecoratingProperties"))
                    .blockEnd();
        }

        if (underlyingIsFragmentEvent) {
            CodegenExpression fragment = ((EventTypeSPI) eventTypes[underlyingStreamNumber]).getGetterSPI(unnamedStreams.get(0).getStreamSelected().getStreamName()).eventBeanFragmentCodegen(ref("eventBean"), context);
            block.declareVar(EventBean.class, "eventBean", arrayAtIndex(params.passEPS(), constant(underlyingStreamNumber)))
                    .declareVar(EventBean.class, "theEvent", cast(EventBean.class, fragment));
        } else if (underlyingPropertyEventGetter != null) {
            block.declareVar(EventBean.class, "theEvent", constantNull())
                    .declareVar(Object.class, "value", underlyingPropertyEventGetter.eventBeanGetCodegen(arrayAtIndex(params.passEPS(), constant(underlyingStreamNumber)), context))
                    .ifRefNotNull("value")
                    .assignRef("theEvent", exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForBean", ref("value")))
                    .blockEnd();
        } else if (underlyingExprForge != null) {
            block.declareVar(EventBean.class, "theEvent", constantNull())
                    .declareVar(Object.class, "value", underlyingExprForge.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context))
                    .ifRefNotNull("value")
                    .assignRef("theEvent", exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForBean", ref("value")))
                    .blockEnd();
        } else {
            block.declareVar(EventBean.class, "theEvent", arrayAtIndex(params.passEPS(), constant(underlyingStreamNumber)));
            if (tableMetadata != null) {
                CodegenMember eventToPublic = context.makeAddMember(TableMetadataInternalEventToPublic.class, tableMetadata.getEventToPublic());
                block.ifRefNotNull("theEvent")
                        .assignRef("theEvent", exprDotMethod(member(eventToPublic.getMemberId()), "convert", ref("theEvent"), params.passEPS(), params.passIsNewData(), params.passEvalCtx()))
                        .blockEnd();
            }
        }
        CodegenMethodId method = block.methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedWrapper", ref("theEvent"), ref("props"), member(memberResultEventType.getMemberId())));
        return localMethodBuild(method).pass(props).passAll(params).call();
    }
}
