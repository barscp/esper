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
package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.epl.core.*;
import com.espertech.esper.epl.core.eval.SelectExprForgeContext;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.TypeWidener;
import com.espertech.esper.util.TypeWidenerCustomizer;
import com.espertech.esper.util.TypeWidenerFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.nio.ByteBuffer;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalSelectNoWildcardAvro implements SelectExprProcessor, SelectExprProcessorForge {

    private final SelectExprForgeContext selectExprForgeContext;
    private final AvroEventType resultEventType;
    private final ExprForge[] forges;
    private ExprEvaluator[] evaluators;

    public EvalSelectNoWildcardAvro(SelectExprForgeContext selectExprForgeContext, ExprForge[] exprForges, EventType resultEventType, String statementName, String engineURI) throws ExprValidationException {
        this.selectExprForgeContext = selectExprForgeContext;
        this.resultEventType = (AvroEventType) resultEventType;

        this.forges = new ExprForge[selectExprForgeContext.getExprForges().length];
        TypeWidenerCustomizer typeWidenerCustomizer = selectExprForgeContext.getEventAdapterService().getTypeWidenerCustomizer(resultEventType);
        for (int i = 0; i < forges.length; i++) {
            forges[i] = selectExprForgeContext.getExprForges()[i];
            ExprForge forge = exprForges[i];
            Class forgeEvaluationType = forge.getEvaluationType();

            if (forge instanceof SelectExprProcessorEvalByGetterFragment) {
                forges[i] = handleFragment((SelectExprProcessorEvalByGetterFragment) forge);
            } else if (forge instanceof SelectExprProcessorEvalStreamInsertUnd) {
                SelectExprProcessorEvalStreamInsertUnd und = (SelectExprProcessorEvalStreamInsertUnd) forge;
                forges[i] = new SelectExprInsertEventBeanFactory.ExprForgeStreamUnderlying(und.getStreamNum(), Object.class);
            } else if (forge instanceof SelectExprProcessorTypableMapForge) {
                SelectExprProcessorTypableMapForge typableMap = (SelectExprProcessorTypableMapForge) forge;
                forges[i] = new SelectExprProcessorEvalAvroMapToAvro(typableMap.getInnerForge(), ((AvroEventType) resultEventType).getSchemaAvro(), selectExprForgeContext.getColumnNames()[i]);
            } else if (forge instanceof SelectExprProcessorEvalStreamInsertNamedWindow) {
                SelectExprProcessorEvalStreamInsertNamedWindow nw = (SelectExprProcessorEvalStreamInsertNamedWindow) forge;
                forges[i] = new SelectExprInsertEventBeanFactory.ExprForgeStreamUnderlying(nw.getStreamNum(), Object.class);
            } else if (forgeEvaluationType != null && forgeEvaluationType.isArray()) {
                TypeWidener widener = TypeWidenerFactory.getArrayToCollectionCoercer(forgeEvaluationType.getComponentType());
                Class resultType = Collection.class;
                if (forgeEvaluationType == byte[].class) {
                    widener = TypeWidenerFactory.BYTE_ARRAY_TO_BYTE_BUFFER_COERCER;
                    resultType = ByteBuffer.class;
                }
                forges[i] = new SelectExprProcessorEvalAvroArrayCoercer(forge, widener, resultType);
            } else {
                String propertyName = selectExprForgeContext.getColumnNames()[i];
                Class propertyType = resultEventType.getPropertyType(propertyName);
                TypeWidener widener = TypeWidenerFactory.getCheckPropertyAssignType(propertyName, forgeEvaluationType, propertyType, propertyName, true, typeWidenerCustomizer, statementName, engineURI);
                if (widener != null) {
                    forges[i] = new SelectExprProcessorEvalAvroArrayCoercer(forge, widener, propertyType);
                }
            }
        }
    }
    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        if (evaluators == null) {
            evaluators = ExprNodeUtility.getEvaluatorsMayCompile(forges, engineImportService, EvalSelectNoWildcardAvro.class, isFireAndForget, statementName);
        }
        return this;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        String[] columnNames = selectExprForgeContext.getColumnNames();
        GenericData.Record record = new GenericData.Record(resultEventType.getSchemaAvro());

        // Evaluate all expressions and build a map of name-value pairs
        for (int i = 0; i < evaluators.length; i++) {
            Object evalResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            record.put(columnNames[i], evalResult);
        }

        return selectExprForgeContext.getEventAdapterService().adapterForTypedAvro(record, resultEventType);
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public CodegenExpression processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenParamSetSelectPremade params, CodegenContext context) {
        CodegenMember avroSchema = context.makeAddMember(Schema.class, resultEventType.getSchemaAvro());
        CodegenBlock block = context.addMethod(EventBean.class, this.getClass()).add(params).begin()
                .declareVar(GenericData.Record.class, "record", newInstance(GenericData.Record.class, member(avroSchema.getMemberId())));
        for (int i = 0; i < selectExprForgeContext.getColumnNames().length; i++) {
            CodegenExpression expression = forges[i].evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context);
            block.expression(exprDotMethod(ref("record"), "put", constant(selectExprForgeContext.getColumnNames()[i]), expression));
        }
        CodegenMethodId method = block.methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedAvro", ref("record"), member(memberResultEventType.getMemberId())));
        return localMethodBuild(method).passAll(params).call();
    }

    private ExprForge handleFragment(SelectExprProcessorEvalByGetterFragment eval) {
        if (eval.getEvaluationType() == GenericData.Record[].class) {
            return new SelectExprProcessorEvalByGetterFragmentAvroArray(eval.getStreamNum(), eval.getGetter(), Collection.class);
        }
        if (eval.getEvaluationType() == GenericData.Record.class) {
            return new SelectExprProcessorEvalByGetterFragmentAvro(eval.getStreamNum(), eval.getGetter(), GenericData.Record.class);
        }
        throw new EPException("Unrecognized return type " + eval.getEvaluationType() + " for use with Avro");
    }
}