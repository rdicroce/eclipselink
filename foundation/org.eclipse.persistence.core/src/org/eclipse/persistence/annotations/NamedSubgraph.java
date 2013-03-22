/*******************************************************************************
 * Copyright (c) 2011, 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Gordon Yorke - Initial development
 ******************************************************************************/
package org.eclipse.persistence.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A subgraph is a member of an NamedEntityGraph that represents a managed
 * type. The NamedSubgraph is only referenced from within an NamedEntityGraph and
 * can not be referenced on it's own. It is referenced by name from components
 * of the NamedEntityGraph.
 * 
 * @see org.eclipse.persistence.jpa.NamedEntityGraph
 * @see org.eclipse.persistence.jpa.NamedSubgraph
 * @see org.eclipse.persistence.jpa.NamedAttributeNode
 * 
 * @author Gordon Yorke
 * @since EclipseLink 2.4.2
 */
@Target({})
@Retention(RUNTIME)
public @interface NamedSubgraph {
    /**
     * (Required) the name of the sub-graph.
     */
    String name();
    
    Class type() default void.class;

    /**
     * (Optional) if this component references a managed type then this list is
     * the attributes of that type that must be included.
     */
    NamedAttributeNode[] attributes() default {};
}