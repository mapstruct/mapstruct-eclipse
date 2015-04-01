/**
 *  Copyright 2012-2015 Gunnar Morling (http://www.gunnarmorling.de/)
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
package org.mapstruct.eclipse.internal;

import java.util.List;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

abstract class AbstractAnnotationCompletionProposalComputer {

    abstract protected List<ICompletionProposal> computeCompletionProposals(final JavaContentAssistInvocationContext javaContent,
                                                                            final ICompilationUnit compilationUnit,
                                                                            final int invocationOffset,
                                                                            final IAnnotation annotation)
        throws JavaModelException;

    /**
     * Tests if the given offset is in range specified by the given start position and length.
     */
    protected boolean isInRange(int offset, int rangeStartPosition, int rangeLength) {
        if ( rangeStartPosition < offset && offset < rangeStartPosition + rangeLength ) {
            return true;
        }
        return false;
    }

    /**
     * Returns the qualified name of the annotation which is associated with the given {@link IMemberValuePairBinding}.
     */
    protected String getAnnotationQualifiedName(IMemberValuePairBinding binding) {
        IMethodBinding methodBinding = binding.getMethodBinding();
        if ( methodBinding == null ) {
            return null;
        }
        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if ( declaringClass == null ) {
            return null;
        }
        return declaringClass.getQualifiedName();
    }

}
