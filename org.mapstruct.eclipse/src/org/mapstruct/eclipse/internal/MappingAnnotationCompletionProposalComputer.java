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
package org.mapstruct.eclipse.internal;

import static org.mapstruct.eclipse.internal.Bindings.containsAnnotation;
import static org.mapstruct.eclipse.internal.Bindings.getAnnotationQualifiedName;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.CONTEXT_FQ_NAME;
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
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
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
        private final String givenPrefix;

        private Collection<String> proposedProperties = new TreeSet<String>();

        private boolean source = false;
        private boolean valid = false;
        private boolean inMethod = false;

        private ITypeBinding resultType;
        private Map<String, ITypeBinding> sourceNameToType = new HashMap<String, ITypeBinding>();
        private String proposalPrefix;

        private PropertyNameProposalCollector(int invocationOffset, String givenPrefix) {
            super( false );
            this.invocationOffset = invocationOffset;
            this.givenPrefix = givenPrefix;
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
            else if ( !containsAnnotation( annotations, TARGET_TYPE_FQ_NAME )
                && !containsAnnotation( annotations, CONTEXT_FQ_NAME ) ) {
                sourceNameToType.put( node.getName().toString(), binding.getType() );
            }

            return false;
        }

        @Override
        public void endVisit(MethodDeclaration node) {
            if ( !inMethod ) {
                return;
            }

            String pathWithoutLastElement = getPathWithoutLastElement( givenPrefix );
            Deque<String> pathToProposedType;
            if ( pathWithoutLastElement.isEmpty() ) {
                pathToProposedType = new LinkedList<String>();
                proposalPrefix = "";
            }
            else {
                pathToProposedType = new LinkedList<String>( Arrays.asList( pathWithoutLastElement.split( "\\." ) ) );
                proposalPrefix = pathWithoutLastElement + ".";
            }

            String propertyPrefix = getLastPathElement( givenPrefix );

            ITypeBinding proposalType = getTypeForPropertyProposals( node, pathToProposedType, propertyPrefix );

            if ( proposalType != null ) {
                proposePropertiesIfPrefixMatches(
                    propertyPrefix,
                    proposalType );
            }
        }

        private ITypeBinding getTypeForPropertyProposals(MethodDeclaration node, Deque<String> pathToProposedType,
                                                         String propertyPrefix) {
            ITypeBinding proposalType;
            if ( source ) {
                if ( sourceNameToType.size() > 1 ) {
                    if ( pathToProposedType.isEmpty() ) {
                        proposeIfPrefixMatches( propertyPrefix, sourceNameToType.keySet() );

                        return null;
                    }
                    else {
                        // for multiple source params, the first element would be expected to be the parameter name
                        String first = pathToProposedType.removeFirst();

                        proposalType = sourceNameToType.get( first );
                    }
                }
                else {
                    proposalType = sourceNameToType.values().iterator().next();
                }
            }
            else {
                if ( resultType == null ) {
                    proposalType = node.resolveBinding().getReturnType();
                }
                else {
                    proposalType = resultType;
                }
            }

            return retrieveProposalTypeFromPath( proposalType, pathToProposedType );
        }

        private ITypeBinding retrieveProposalTypeFromPath(ITypeBinding proposalType, Deque<String> pathToProposedType) {
            if ( proposalType != null && !pathToProposedType.isEmpty() ) {
                if ( proposalType.isEnum() ) {
                    return null;
                }

                String propertyName = pathToProposedType.removeFirst();
                Collection<IMethodBinding> methodNames = Bindings.findAllMethods( proposalType );

                String[] prefixes = source ? READ_ACCESSOR_PREFIXES : WRITE_ACCESSOR_PREFIXES;
                Map<String, List<IMethodBinding>> propertyMethods =
                    findPropertyMethods( propertyName, methodNames, prefixes );
                if ( !propertyMethods.isEmpty() ) {
                    IMethodBinding firstMethod = propertyMethods.values().iterator().next().iterator().next();
                    ITypeBinding nextType;
                    if ( source ) {
                        nextType = firstMethod.getReturnType();
                    }
                    else {
                        nextType = ( firstMethod.getParameterTypes().length > 0 ? firstMethod.getParameterTypes()[0]
                                        : firstMethod.getReturnType() );
                    }

                    proposalType = retrieveProposalTypeFromPath( nextType, pathToProposedType );
                }
                else {
                    return null;
                }
            }

            return proposalType;
        }

        private void proposePropertiesIfPrefixMatches(String propertyPrefix, ITypeBinding type) {
            if ( type.isEnum() ) {
                proposeIfPrefixMatches( propertyPrefix, Bindings.findAllEnumConstants( type ) );
            }
            else {
                Collection<IMethodBinding> methodNames = Bindings.findAllMethods( type );
                if ( source ) {
                    proposeIfPrefixMatches(
                        propertyPrefix,
                        findPropertyMethods( methodNames, READ_ACCESSOR_PREFIXES ).keySet() );
                }
                else {
                    proposeIfPrefixMatches(
                        propertyPrefix,
                        findPropertyMethods( methodNames, WRITE_ACCESSOR_PREFIXES ).keySet() );
                }
            }
        }

        private void proposeIfPrefixMatches(String propertyPrefix, Collection<String> keySet) {
            for ( String value : keySet ) {
                proposeIfPrefixMatches( propertyPrefix, value );
            }
        }

        private void proposeIfPrefixMatches(String prefix, String value) {
            if ( prefix.isEmpty() || value.startsWith( prefix ) ) {
                proposedProperties.add( proposalPrefix + value );
            }
        }


        public Collection<String> getProperties() {
            return proposedProperties;
        }

        public boolean isValidValue() {
            return valid;
        }
    }

    private static String getLastPathElement(String path) {
        int lastDot = path.lastIndexOf( '.' );
        if ( lastDot >= 0 ) {
            return path.substring( lastDot + 1, path.length() );
        }
        return path;
    }

    private static String getPathWithoutLastElement(String path) {
        int lastDot = path.lastIndexOf( '.' );
        if ( lastDot >= 0 ) {
            return path.substring( 0, lastDot );
        }
        return "";
    }

    private static final List<String> MAPPING_ANNOTATION_NAMES = Arrays.asList(
        MAPPING_FQ_NAME,
        MAPPING_SIMPLE_NAME,
        MAPPINGS_SIMPLE_NAME,
        MAPPINGS_FQ_NAME,
        VALUE_MAPPING_FQ_NAME,
        VALUE_MAPPING_SIMPLE_NAME );

    private static final String[] READ_ACCESSOR_PREFIXES = { "get", "is" }; //$NON-NLS-1$
    private static final String[] WRITE_ACCESSOR_PREFIXES = { "set" }; //$NON-NLS-1$

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

        PropertyNameProposalCollector astVisitor = new PropertyNameProposalCollector( invocationOffset, token );

        astNode.accept( astVisitor );

        if ( astVisitor.isValidValue() ) {
            Collection<String> propertiesToProcess = astVisitor.getProperties();

            for ( String property : propertiesToProcess ) {
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

        return returnValue;
    }

    @Override
    protected List<String> getAnnotationNames() {
        return MAPPING_ANNOTATION_NAMES;
    }

    /**
     * Finds {@link IMethodBinding}s starting with any of the given prefixes for the given property.
     */
    private Map<String, List<IMethodBinding>> findPropertyMethods(String propertyName,
                                                                  Collection<IMethodBinding> methods,
                                                                  String... candidatePrefixes) {
        Map<String, List<IMethodBinding>> returnValue = new HashMap<String, List<IMethodBinding>>();
        for ( IMethodBinding method : methods ) {
            String methodName = method.getName();
            String matchingPrefix = getMatchingPrefix( methodName, candidatePrefixes );
            if ( matchingPrefix != null ) {
                String methodPropertyName =
                    Introspector.decapitalize( methodName.substring( matchingPrefix.length() ) );

                if ( propertyName == null || methodPropertyName.equals( propertyName ) ) {
                    List<IMethodBinding> accessorMethods = returnValue.get( methodPropertyName );
                    if ( accessorMethods == null ) {
                        accessorMethods = new ArrayList<IMethodBinding>( 2 );
                        returnValue.put( methodPropertyName, accessorMethods );
                    }

                    accessorMethods.add( method );
                }
            }
        }

        return returnValue;
    }

    /**
     * Finds {@link IMethodBinding}s starting with any of the given prefix and extracts the associated property name
     * from it.
     */
    private Map<String, List<IMethodBinding>> findPropertyMethods(Collection<IMethodBinding> methods,
                                                                  String[] candidatePrefixes) {
        return findPropertyMethods( null, methods, candidatePrefixes );
    }

    private String getMatchingPrefix(String methodName, String[] candidatePrefixes) {
        for ( String prefix : candidatePrefixes ) {
            if ( methodName.startsWith( prefix ) ) {
                return prefix;
            }
        }
        return null;
    }

    private boolean isTargetNode(MemberValuePair node) {
        return MAPPING_MEMBER_TARGET.equals( node.getName().toString() );
    }

    private boolean isSourceNode(MemberValuePair node) {
        return MAPPING_MEMBER_SOURCE.equals( node.getName().toString() );
    }
}
