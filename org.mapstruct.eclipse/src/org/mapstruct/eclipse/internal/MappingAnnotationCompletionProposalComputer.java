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
package org.mapstruct.eclipse.internal;

import static org.mapstruct.eclipse.internal.Bindings.containsAnnotation;
import static org.mapstruct.eclipse.internal.Bindings.findAllMethodNames;
import static org.mapstruct.eclipse.internal.Bindings.getAnnotationQualifiedName;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPINGS_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPINGS_SIMPLE_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_MEMBER_SOURCE;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_MEMBER_TARGET;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_SIMPLE_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_TARGET_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.TARGET_TYPE_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.VALUE_MAPPING_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.VALUE_MAPPING_SIMPLE_NAME;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * Computes MapStruct specific content assist completion proposals for the <code>@Mapping</code> annotation.
 *
 * @author Lars Wetzer
 * @author Andreas Gudian
 */
public class MappingAnnotationCompletionProposalComputer extends AbstractAnnotationCompletionProposalComputer {

    private final class PropertyNameProposalCollector extends ASTVisitor {
        private final int invocationOffset;

        private Collection<String> sourceProperties = new TreeSet<String>();
        private Collection<String> targetProperties = new TreeSet<String>();

        private boolean source = false;
        private boolean valid = false;
        private boolean inMethod = false;

        private ITypeBinding resultType;
        private Map<String, ITypeBinding> sourceNameToType = new HashMap<String, ITypeBinding>();

        private PropertyNameProposalCollector(int invocationOffset) {
            super( false );
            this.invocationOffset = invocationOffset;
        }

        @Override
        public boolean visit(MemberValuePair node) {
            String annotationQualifiedName = getAnnotationQualifiedName( node.resolveMemberValuePairBinding() );

            if ( isSupportedAnnotation( annotationQualifiedName )
                && isInRange( invocationOffset, node.getValue().getStartPosition(), node.getValue().getLength() )
                && ( isSourceNode( node ) || isTargetNode( node ) ) ) {

                valid = true;

                source = isSourceNode( node );
            }

            return false;
        }

        private boolean isSupportedAnnotation(String annotationQualifiedName) {
            return MAPPING_FQ_NAME.equals( annotationQualifiedName )
                || VALUE_MAPPING_FQ_NAME.equals( annotationQualifiedName );
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            if ( isInRange( invocationOffset, node.getStartPosition(), node.getLength() ) ) {
                inMethod = true;
                return true;
            }

            inMethod = false;
            return false;
        }

        @Override
        public boolean visit(SingleVariableDeclaration node) {
            IVariableBinding binding = node.resolveBinding();
            IAnnotationBinding[] annotations = binding.getAnnotations();

            if ( containsAnnotation( annotations, MAPPING_TARGET_FQ_NAME ) ) {
                resultType = binding.getType();
            }
            else if ( !containsAnnotation( annotations, TARGET_TYPE_FQ_NAME ) ) {
                sourceNameToType.put( node.getName().toString(), binding.getType() );
            }

            return false;
        }

        @Override
        public void endVisit(MethodDeclaration node) {
            if ( !inMethod ) {
                return;
            }

            if ( null == resultType ) {
                resultType = node.resolveBinding().getReturnType();
            }

            if ( isEnumMapping() ) {
                targetProperties.addAll( Bindings.findAllEnumConstants( resultType ) );
                sourceProperties.addAll( Bindings.findAllEnumConstants( sourceNameToType.values().iterator().next() ) );
            }
            else {
                targetProperties.addAll( findProperties( findAllMethodNames( resultType ), SET_PREFIX ) );
                if ( sourceNameToType.size() > 1 ) {
                    sourceProperties.addAll( sourceNameToType.keySet() );
                }
                else {
                    for ( ITypeBinding sourceType : sourceNameToType.values() ) {
                        Collection<String> methodNames = findAllMethodNames( sourceType );
                        sourceProperties.addAll( findProperties( methodNames, GET_PREFIX ) );
                        sourceProperties.addAll( findProperties( methodNames, IS_PREFIX ) );
                    }
                }
            }
        }

        private boolean isEnumMapping() {
            return ( ( sourceNameToType.size() == 1 && sourceNameToType.values().iterator().next().isEnum() )
                || resultType.isEnum() );
        }

        public Collection<String> getProperties() {
            if ( source ) {
                return sourceProperties;
            }
            else {
                return targetProperties;
            }
        }

        public boolean isValidValue() {
            return valid;
        }
    }

    private static final List<String> MAPPING_ANNOTATION_NAMES = Arrays.asList(
        MAPPING_FQ_NAME,
        MAPPING_SIMPLE_NAME,
        MAPPINGS_SIMPLE_NAME,
        MAPPINGS_FQ_NAME,
        VALUE_MAPPING_FQ_NAME,
        VALUE_MAPPING_SIMPLE_NAME );

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

        ASTNode astNode = parser.createAST( null );

        PropertyNameProposalCollector astVisitor = new PropertyNameProposalCollector( invocationOffset );

        astNode.accept( astVisitor );

        if ( astVisitor.isValidValue() ) {
            Collection<String> propertiesToProcess = astVisitor.getProperties();

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

    @Override
    protected List<String> getAnnotationNames() {
        return MAPPING_ANNOTATION_NAMES;
    }

    /**
     * Finds {@link IMethodBinding}s starting with the given prefix and extracts the associated property name from it.
     */
    private List<String> findProperties(Collection<String> methodNames, String methodPrefix) {
        List<String> returnValue = new ArrayList<String>();
        for ( String methodName : methodNames ) {
            if ( methodName.startsWith( methodPrefix ) ) {
                String propertyName = methodName.substring( methodPrefix.length() );
                returnValue.add( Introspector.decapitalize( propertyName ) );
            }
        }
        return returnValue;
    }

    private boolean isTargetNode(MemberValuePair node) {
        return MAPPING_MEMBER_TARGET.equals( node.getName().toString() );
    }

    private boolean isSourceNode(MemberValuePair node) {
        return MAPPING_MEMBER_SOURCE.equals( node.getName().toString() );
    }
}
