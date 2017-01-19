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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.mapstruct.eclipse.internal.MapStructAPIConstants;

/**
 * Computes MapStruct specific content assist completion proposals for the {@code @Mapper} annotation.
 *
 * @author Stefan Rademacher
 */
public class MapperAnnotationCompletionProposalComputer extends AbstractAnnotationCompletionProposalComputer {

    private static final List<String> MAPPER_ANNOTATION_NAMES = Arrays.asList(
        MapStructAPIConstants.MAPPER_SIMPLE_NAME,
        MapStructAPIConstants.MAPPER_FQ_NAME );

    private static final List<String> COMPONENT_MODEL_TYPES = Collections.unmodifiableList( Arrays.asList(
        "default",
        "cdi",
        "spring",
        "jsr330" ) );

    @Override
    protected List<String> getAnnotationNames() {
        return MAPPER_ANNOTATION_NAMES;
    }

    @Override
    protected List<ICompletionProposal> getProposals(ICompilationUnit compilationUnit, int invocationOffset,
                                                     String token) {
        final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

        for ( final String property : COMPONENT_MODEL_TYPES ) {

            if ( property.startsWith( token ) ) {

                final String replacement = property.substring( token.length() );

                final CompletionProposal proposal =
                    new CompletionProposal(
                        replacement,
                        invocationOffset,
                        0,
                        replacement.length(),
                        JavaUI.getSharedImages().getImage( ISharedImages.IMG_OBJS_PUBLIC ),
                        property,
                        null,
                        null );

                proposals.add( proposal );

            }

        }

        return proposals;
    }

}
