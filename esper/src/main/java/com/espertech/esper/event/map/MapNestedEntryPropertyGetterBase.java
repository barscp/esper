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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public abstract class MapNestedEntryPropertyGetterBase implements MapEventPropertyGetter {

    protected final String propertyMap;
    protected final EventType fragmentType;
    protected final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param propertyMap         the property to look at
     * @param eventAdapterService factory for event beans and event types
     * @param fragmentType        type of the entry returned
     */
    public MapNestedEntryPropertyGetterBase(String propertyMap, EventType fragmentType, EventAdapterService eventAdapterService) {
        this.propertyMap = propertyMap;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
    }

    public abstract Object handleNestedValue(Object value);
    public abstract Object handleNestedValueFragment(Object value);
    public abstract CodegenExpression handleNestedValueCodegen(CodegenExpression name, CodegenContext context);
    public abstract CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression name, CodegenContext context);

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object value = map.get(propertyMap);
        if (value == null) {
            return null;
        }
        return handleNestedValue(value);
    }

    private CodegenMethodId getMapCodegen(CodegenContext context) {
        return context.addMethod(Object.class, this.getClass()).add(Map.class, "map").begin()
                .declareVar(Object.class, "value", exprDotMethod(ref("map"), "get", constant(propertyMap)))
                .ifRefNullReturnNull("value")
                .methodReturn(handleNestedValueCodegen(ref("value"), context));
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        return getMap(BaseNestableEventUtil.checkedCastUnderlyingMap(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean obj) {
        Map<String, Object> map = BaseNestableEventUtil.checkedCastUnderlyingMap(obj);
        Object value = map.get(propertyMap);
        if (value == null) {
            return null;
        }
        return handleNestedValueFragment(value);
    }

    private CodegenMethodId getFragmentCodegen(CodegenContext context) {
        return context.addMethod(Object.class, this.getClass()).add(Map.class, "map").begin()
                .declareVar(Object.class, "value", exprDotMethod(ref("map"), "get", constant(propertyMap)))
                .ifRefNullReturnNull("value")
                .methodReturn(handleNestedValueFragmentCodegen(ref("value"), context));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingFragmentCodegen(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getMapCodegen(context), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }
}
