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

import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPINGS_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPINGS_SIMPLE_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_SIMPLE_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.VALUE_MAPPING_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.VALUE_MAPPING_SIMPLE_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.mapstruct.eclipse.internal.proposal.visitors.PropertyNameProposalCollector;

/**
 * Computes MapStruct specific content assist completion proposals for the <code>@Mapping</code> annotation.
 *
 * @author Lars Wetzer
 * @author Andreas Gudian
 */
public class MappingAnnotationCompletionProposalComputer extends AbstractAnnotationCompletionProposalComputer {
    private static final List<String> MAPPING_ANNOTATION_NAMES = Arrays.asList(
        MAPPING_FQ_NAME,
        MAPPING_SIMPLE_NAME,
        MAPPINGS_SIMPLE_NAME,
        MAPPINGS_FQ_NAME,
        VALUE_MAPPING_FQ_NAME,
        VALUE_MAPPING_SIMPLE_NAME );

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
}
