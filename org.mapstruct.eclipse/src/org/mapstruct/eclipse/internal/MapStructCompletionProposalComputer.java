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

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

/**
 * Computes MapStruct specific content assist completion proposals.
 * 
 * @author Lars Wetzer
 */
public class MapStructCompletionProposalComputer implements IJavaCompletionProposalComputer {

    private static final String MAPPING_ANNOTATION_QUALIFIED_NAME = "org.mapstruct.Mapping"; //$NON-NLS-1$
    private static final List<String> MAPPING_ANNOTATION_NAMES = Arrays.asList( "Mappings", "Mapping" ); //$NON-NLS-1$ //$NON-NLS-2$
    private static final String SOURCE_ANNOTATION_METHOD = "source"; //$NON-NLS-1$
    private static final String TARGET_ANNOTATION_METHOD = "target"; //$NON-NLS-1$
    private static final String GET_PREFIX = "get"; //$NON-NLS-1$
    private static final String SET_PREFIX = "set"; //$NON-NLS-1$
    private static final String IS_PREFIX = "is"; //$NON-NLS-1$

    private static final List<IContextInformation> EMPTY_CONTEXTS = Collections.emptyList();
    private static final List<ICompletionProposal> EMPTY_PROPOSALS = Collections.emptyList();

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
        return EMPTY_CONTEXTS;
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

                if ( MAPPING_ANNOTATION_NAMES.contains( annotation.getElementName() )
                    && isInRange(
                        invocationOffset,
                        annotation.getSourceRange().getOffset(),
                        annotation.getSourceRange().getLength() ) ) {

                    return parseCompilationUnit(
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

    /**
     * Parses the given {@link ICompilationUnit} and returns {@link ICompletionProposal}s for the given invocation
     * offset and token.
     */
    private List<ICompletionProposal> parseCompilationUnit(final ICompilationUnit compilationUnit,
                                                           final int invocationOffset, final String token) {

        List<ICompletionProposal> returnValue = new ArrayList<ICompletionProposal>();

        ASTParser parser = ASTParser.newParser( AST.JLS8 );
        parser.setKind( ASTParser.K_COMPILATION_UNIT );
        parser.setSource( compilationUnit );
        parser.setResolveBindings( true );

        AtomicBoolean isValidValue = new AtomicBoolean( false );
        AtomicBoolean isSource = new AtomicBoolean( false );

        Set<String> sourceProperties = new TreeSet<String>();
        Set<String> targetProperties = new TreeSet<String>();

        ASTNode astNode = parser.createAST( null );

        ASTVisitor astVisitor =
            createVisitor( invocationOffset, isValidValue, isSource, sourceProperties, targetProperties );

        astNode.accept( astVisitor );

        if ( isValidValue.get() ) {

            Set<String> propertiesToProcess;

            if ( isSource.get() ) {
                propertiesToProcess = sourceProperties;
            }
            else {
                propertiesToProcess = targetProperties;
            }

            for ( String property : propertiesToProcess ) {

                if ( property.startsWith( token ) ) {

                    String replacement = property.substring( token.length() );

                    CompletionProposal proposal =
                        new CompletionProposal(
                            replacement,
                            invocationOffset,
                            0,
                            replacement.length(),
                            JavaUI.getSharedImages().getImage( ISharedImages.IMG_OBJS_PUBLIC ),
                            property,
                            null,
                            null );

                    returnValue.add( proposal );

                }

            }

        }

        return returnValue;
    }

    /**
     * Creates an {@link ASTVisitor} that discovers all properties for a <code>Mapping</code>'s <code>source</code> and
     * <code>target</code> method for the given invocation offset.
     */
    private ASTVisitor createVisitor(final int invocationOffset, final AtomicBoolean isValidValue,
                                     final AtomicBoolean isSource, final Set<String> sourceProperties,
                                     final Set<String> targetProperties) {

        return new ASTVisitor( false ) {

            @Override
            public boolean visit(MemberValuePair node) {

                String annotationQualifiedName = getAnnotationQualifiedName( node.resolveMemberValuePairBinding() );

                if ( MAPPING_ANNOTATION_QUALIFIED_NAME.equals( annotationQualifiedName )
                    && isInRange( invocationOffset, node.getValue().getStartPosition(), node.getValue().getLength() )
                    && isMappingAnnotationMethod( node ) ) {

                    isValidValue.set( true );

                    if ( SOURCE_ANNOTATION_METHOD.equals( node.getName().toString() ) ) {
                        isSource.set( true );
                    }

                }

                return false;

            }

            @Override
            public boolean visit(MethodDeclaration node) {

                if ( isInRange( invocationOffset, node.getStartPosition(), node.getLength() ) ) {

                    IMethodBinding binding = node.resolveBinding();

                    ITypeBinding returnType = binding.getReturnType();
                    targetProperties.addAll( findProperties( returnType.getDeclaredMethods(), SET_PREFIX ) );

                    ITypeBinding[] parameterTypes = binding.getParameterTypes();
                    if ( parameterTypes.length == 1 ) {
                        IMethodBinding[] declaredMethods = parameterTypes[0].getDeclaredMethods();
                        sourceProperties.addAll( findProperties( declaredMethods, GET_PREFIX ) );
                        sourceProperties.addAll( findProperties( declaredMethods, IS_PREFIX ) );
                    }

                    return true;

                }

                return false;

            }

        };

    }

    /**
     * Finds {@link IMethodBinding}s starting with the given prefix and extracts the associated property name from it.
     */
    private Set<String> findProperties(IMethodBinding[] methodBindings, String methodPrefix) {
        Set<String> returnValue = new HashSet<String>();
        for ( IMethodBinding methodBinding : methodBindings ) {
            if ( methodBinding.getName().startsWith( methodPrefix ) ) {
                String propertyName = methodBinding.getName().substring( methodPrefix.length() );
                returnValue.add( Introspector.decapitalize( propertyName ) );
            }
        }
        return returnValue;
    }

    /**
     * Tests if the given offset is in range specified by the given start position and length.
     */
    private boolean isInRange(int offset, int rangeStartPosition, int rangeLength) {
        if ( rangeStartPosition < offset && offset < rangeStartPosition + rangeLength ) {
            return true;
        }
        return false;
    }

    /**
     * Decides whether the given {@link MemberValuePair} is a <code>Mapping</code> annotation method.
     */
    private boolean isMappingAnnotationMethod(MemberValuePair node) {
        if ( SOURCE_ANNOTATION_METHOD.equals( node.getName().toString() )
            || TARGET_ANNOTATION_METHOD.equals( node.getName().toString() ) ) {
            return true;
        }
        return false;
    }

    /**
     * Returns the qualified name of the annotation which is associated with the given {@link IMemberValuePairBinding}.
     */
    private String getAnnotationQualifiedName(IMemberValuePairBinding binding) {
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
