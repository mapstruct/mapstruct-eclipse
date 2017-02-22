/**
 *  Copyright 2012-2017 Gunnar Morling (http://www.gunnarmorling.de/)
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
package org.mapstruct.eclipse.internal.proposal;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.mapstruct.eclipse.internal.util.Ranges;

/**
 * Base class for completion proposal computers.
 *
 * @author Lars Wetzer
 * @author Stefan Rademacher
 */
abstract class AbstractAnnotationCompletionProposalComputer implements IJavaCompletionProposalComputer {

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
                return Collections.emptyList();
            }

            JavaContentAssistInvocationContext javaContent = (JavaContentAssistInvocationContext) context;

            ICompilationUnit compilationUnit = javaContent.getCompilationUnit();

            if ( compilationUnit == null || !compilationUnit.isStructureKnown() ) {
                return Collections.emptyList();
            }

            int invocationOffset = javaContent.getInvocationOffset();

            IJavaElement javaElement = compilationUnit.getElementAt( invocationOffset );

            if ( !( javaElement instanceof IAnnotatable ) ) {
                return Collections.emptyList();
            }

            final IAnnotatable annotatable = (IAnnotatable) javaElement;
            for ( final IAnnotation annotation : annotatable.getAnnotations() ) {

                if ( getAnnotationNames().contains( annotation.getElementName() )
                    && Ranges.isInRange(
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
            return Collections.emptyList();
        }

        return Collections.emptyList();

    }

    protected abstract List<ICompletionProposal> getProposals(ICompilationUnit compilationUnit, int invocationOffset,
                                                              String valueOf);

    protected abstract List<String> getAnnotationNames();
}
