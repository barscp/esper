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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.bean.BeanEventType;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * A getter for use with Map-based events simply returns the value for the key.
 */
public class ObjectArrayEntryPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final EventAdapterService eventAdapterService;
    private final BeanEventType eventType;

    /**
     * Ctor.
     *
     * @param propertyIndex       index
     * @param eventType           type of the entry returned
     * @param eventAdapterService factory for event beans and event types
     */
    public ObjectArrayEntryPropertyGetter(int propertyIndex, BeanEventType eventType, EventAdapterService eventAdapterService) {
        this.propertyIndex = propertyIndex;
        this.eventAdapterService = eventAdapterService;
        this.eventType = eventType;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        return array[propertyIndex];
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        Object[] arr = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return getObjectArray(arr);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean eventBean) {
        if (eventType == null) {
            return null;
        }
        Object result = get(eventBean);
        return BaseNestableEventUtil.getBNFragmentPojo(result, eventType, eventAdapterService);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context) {
        if (eventType == null) {
            return constantNull();
        }
        return underlyingFragmentCodegen(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return arrayAtIndex(underlyingExpression, constant(propertyIndex));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        if (eventType == null) {
            return constantNull();
        }
        CodegenMember mSvc = context.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mType = context.makeAddMember(BeanEventType.class, eventType);
        return staticMethod(BaseNestableEventUtil.class, "getBNFragmentPojo", underlyingGetCodegen(underlyingExpression, context), member(mType.getMemberId()), member(mSvc.getMemberId()));
    }
}
