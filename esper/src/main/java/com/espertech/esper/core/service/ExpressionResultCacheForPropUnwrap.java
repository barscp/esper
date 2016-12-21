/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;

import java.util.Collection;
import java.util.Deque;

/**
 * On the level of indexed event properties: Properties that are contained in EventBean instances, such as for Enumeration Methods, get wrapped only once for the same event.
 * The cache is keyed by property-name and EventBean reference and maintains a Collection&lt;EventBean&gt;.
 *
 * NOTE: ExpressionResultCacheEntry should not be held onto since the instance returned can be reused.
 */
public interface ExpressionResultCacheForPropUnwrap {

    ExpressionResultCacheEntry<EventBean, Collection<EventBean>> getPropertyColl(String propertyNameFullyQualified, EventBean reference);
    void savePropertyColl(String propertyNameFullyQualified, EventBean reference, Collection<EventBean> events);
}