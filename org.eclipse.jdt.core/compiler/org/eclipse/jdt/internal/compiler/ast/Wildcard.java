/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

/**
 * @author oliviert
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Wildcard extends SingleTypeReference {

	public TypeReference type;
	boolean isSuper;
	/**
	 * @param source
	 * @param pos
	 */
	public Wildcard(boolean isSuper) {
		super(null, 0);
		this.isSuper = isSuper;
	}
	
	public StringBuffer printExpression(int indent, StringBuffer output){
		output.append('?');
		if (this.type != null) {
			if (this.isSuper) {
				output.append(" super "); //$NON-NLS-1$
			} else {
				output.append(" extends "); //$NON-NLS-1$
			}
			this.type.printExpression(0, output);
		}
		return output;
	}	

	public void traverse(IAbstractSyntaxTreeVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		if (this.type != null) {
			this.type.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}

	public void traverse(IAbstractSyntaxTreeVisitor visitor, ClassScope scope) {
		visitor.visit(this, scope);
		if (this.type != null) {
			this.type.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
}
