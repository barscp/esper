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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterAndIndexed;
import com.espertech.esper.event.vaevent.PropertyUtility;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

/**
 * Getter for an array property backed by a field, identified by a given index, using vanilla reflection.
 */
public class ArrayFieldPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndIndexed {
    private final Field field;
    private final int index;

    /**
     * Constructor.
     *
     * @param field               is the field to use to retrieve a value from the object
     * @param index               is tge index within the array to get the property from
     * @param eventAdapterService factory for event beans and event types
     */
    public ArrayFieldPropertyGetter(Field field, int index, EventAdapterService eventAdapterService) {
        super(eventAdapterService, field.getType().getComponentType(), null);
        this.index = index;
        this.field = field;

        if (index < 0) {
            throw new IllegalArgumentException("Invalid negative index value");
        }
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return getBeanPropInternal(object, index);
    }

    private Object getBeanPropInternal(Object object, int index) throws PropertyAccessException {
        try {
            Object value = field.get(object);
            if (Array.getLength(value) <= index) {
                return null;
            }
            return Array.get(value, index);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(field, object, e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(field, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(field, e);
        }
    }

    private CodegenMethodId getBeanPropInternalCodegen(CodegenContext context) {
        return context.addMethod(getBeanPropType(), this.getClass()).add(getTargetType(), "object").add(int.class, "index").begin()
                .declareVar(Object.class, "value", exprDotName(ref("object"), field.getName()))
                .ifConditionReturnConst(relational(staticMethod(Array.class, "getLength", ref("value")), LE, ref("index")), null)
                .methodReturn(cast(getBeanPropType(), staticMethod(Array.class, "get", ref("value"), ref("index"))));
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        return getBeanProp(obj.getUnderlying());
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), index);
    }

    public Class getBeanPropType() {
        return field.getType().getComponentType();
    }

    public String toString() {
        return "ArrayFieldPropertyGetter " +
                " field=" + field.toString() +
                " index=" + index;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Class getTargetType() {
        return field.getDeclaringClass();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(getTargetType(), beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getBeanPropInternalCodegen(context), underlyingExpression, constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenContext context, CodegenExpression beanExpression, CodegenExpression key) {
        return localMethod(getBeanPropInternalCodegen(context), castUnderlying(getTargetType(), beanExpression), key);
    }
}
