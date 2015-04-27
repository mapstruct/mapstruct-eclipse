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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * Computes MapStruct specific content assist completion proposals for the <code>@Mapping</code> annotation.
 *
 * @author Lars Wetzer
 */
public class MappingAnnotationCompletionProposalComputer extends AbstractAnnotationCompletionProposalComputer {

    private static final List<String> MAPPING_ANNOTATION_NAMES = Arrays.asList(
        MapStructAPIConstants.MAPPING_FQ_NAME,
        MapStructAPIConstants.MAPPING_SIMPLE_NAME,
        MapStructAPIConstants.MAPPINGS_SIMPLE_NAME,
        MapStructAPIConstants.MAPPINGS_FQ_NAME );

    private static final String GET_PREFIX = "get"; //$NON-NLS-1$
    private static final String SET_PREFIX = "set"; //$NON-NLS-1$
    private static final String IS_PREFIX = "is"; //$NON-NLS-1$

    /**
     * Parses the given {@link ICompilationUnit} and returns {@link ICompletionProposal}s for the given invocation
     * offset and token.
     */
    @Override
    protected List<ICompletionProposal> getProposals(final ICompilationUnit compilationUnit,
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

                if ( MapStructAPIConstants.MAPPING_FQ_NAME.equals( annotationQualifiedName )
                    && isInRange( invocationOffset, node.getValue().getStartPosition(), node.getValue().getLength() )
                    && isMappingAnnotationMethod( node ) ) {

                    isValidValue.set( true );

                    if ( MapStructAPIConstants.MAPPING_MEMBER_SOURCE.equals( node.getName().toString() ) ) {
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
                    targetProperties.addAll( findProperties( Bindings.findAllMethodNames( returnType ), SET_PREFIX ) );

                    ITypeBinding[] parameterTypes = binding.getParameterTypes();
                    if ( parameterTypes.length == 1 ) {
                        Set<String> methodNames = Bindings.findAllMethodNames( parameterTypes[0] );
                        sourceProperties.addAll( findProperties( methodNames, GET_PREFIX ) );
                        sourceProperties.addAll( findProperties( methodNames, IS_PREFIX ) );
                    }

                    return true;
                }

                return false;
            }

        };

    }

    @Override
    protected List<String> getAnnotationNames() {
        return MAPPING_ANNOTATION_NAMES;
    }

    /**
     * Finds {@link IMethodBinding}s starting with the given prefix and extracts the associated property name from it.
     */
    private Set<String> findProperties(Set<String> methodNames, String methodPrefix) {
        Set<String> returnValue = new HashSet<String>();
        for ( String methodName : methodNames ) {
            if ( methodName.startsWith( methodPrefix ) ) {
                String propertyName = methodName.substring( methodPrefix.length() );
                returnValue.add( Introspector.decapitalize( propertyName ) );
            }
        }
        return returnValue;
    }

    /**
     * Decides whether the given {@link MemberValuePair} is a <code>Mapping</code> annotation method.
     */
    private boolean isMappingAnnotationMethod(MemberValuePair node) {
        if ( MapStructAPIConstants.MAPPING_MEMBER_SOURCE.equals( node.getName().toString() )
            || MapStructAPIConstants.MAPPING_MEMBER_TARGET.equals( node.getName().toString() ) ) {
            return true;
        }
        return false;
    }

}
