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

    public abstract List<ICompletionProposal> computeCompletionProposals(JavaContentAssistInvocationContext javaContent,
                                                                         ICompilationUnit compilationUnit,
                                                                         int invocationOffset, IAnnotation annotation)
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
