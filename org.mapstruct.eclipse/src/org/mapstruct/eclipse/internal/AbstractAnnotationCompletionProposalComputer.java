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

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

abstract class AbstractAnnotationCompletionProposalComputer implements IJavaCompletionProposalComputer {

    static final List<ICompletionProposal> EMPTY_PROPOSALS = Collections.emptyList();

    @Override
    public void sessionStarted() {
    }

    @Override
    public void sessionEnded() {
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context,
                                                               IProgressMonitor monitor) {
        return Collections.emptyList();
    }

    @Override
    public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context,
                                                                IProgressMonitor monitor) {

        try {

            if ( !( context instanceof JavaContentAssistInvocationContext ) ) {
                return EMPTY_PROPOSALS;
            }

            JavaContentAssistInvocationContext javaContent = (JavaContentAssistInvocationContext) context;

            ICompilationUnit compilationUnit = javaContent.getCompilationUnit();

            if ( compilationUnit == null || !compilationUnit.isStructureKnown() ) {
                return EMPTY_PROPOSALS;
            }

            int invocationOffset = javaContent.getInvocationOffset();

            IJavaElement javaElement = compilationUnit.getElementAt( invocationOffset );

            if ( !( javaElement instanceof IMethod ) ) {
                return EMPTY_PROPOSALS;
            }

            IMethod method = (IMethod) javaElement;
            for ( IAnnotation annotation : method.getAnnotations() ) {

                if ( getAnnotationNames().contains( annotation.getElementName() )
                    && isInRange(
                        invocationOffset,
                        annotation.getSourceRange().getOffset(),
                        annotation.getSourceRange().getLength() ) ) {

                    return getProposals(
                        compilationUnit,
                        invocationOffset,
                        String.valueOf( javaContent.getCoreContext().getToken() ) );
                }

            }
        }
        catch ( Exception e ) {
            return EMPTY_PROPOSALS;
        }

        return EMPTY_PROPOSALS;

    }

    protected abstract List<ICompletionProposal> getProposals(ICompilationUnit compilationUnit, int invocationOffset,
                                                              String valueOf);

    protected abstract List<String> getAnnotationNames();

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
