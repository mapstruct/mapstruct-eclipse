/**
 *  Copyright 2012-2016 Gunnar Morling (http://www.gunnarmorling.de/)
 *  and/or other contributors as indicated by the @authors tag. See the
 *  copyright.txt file in the distribution for a full listing of all
 *  contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.mapstruct.eclipse.internal.quickfix.visitors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

/**
 * Finds an Annotation ASTNode based on the annotation's FQN.
 *
 * @author Andreas Gudian
 */
public class FindAnnotationByNameVisitor extends ASTVisitor {

    private final String fullyQualifiedName;
    private Annotation locatedNode;

    public FindAnnotationByNameVisitor(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        return visitAnnotation( node );
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
        return visitAnnotation( node );
    }

    @Override
    public boolean visit(MarkerAnnotation node) {
        return visitAnnotation( node );
    }

    public Annotation getLocatedNode() {
        return locatedNode;
    }

    private boolean visitAnnotation(Annotation node) {
        Name typeName = node.getTypeName();
        if ( typeName.isQualifiedName() ) {
            if ( typeName.getFullyQualifiedName().equals( fullyQualifiedName ) ) {
                locatedNode = node;
            }
        }
        else {
            ITypeBinding typeBinding = typeName.resolveTypeBinding();
            if ( typeBinding != null && fullyQualifiedName.equals( typeBinding.getBinaryName() ) ) {
                locatedNode = node;
            }
        }

        return false;
    }
}
