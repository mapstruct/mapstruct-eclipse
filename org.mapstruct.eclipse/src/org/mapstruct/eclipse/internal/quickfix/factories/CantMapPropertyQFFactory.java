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
package org.mapstruct.eclipse.internal.quickfix.factories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.mapstruct.eclipse.internal.quickfix.MapStructQuickFix;
import org.mapstruct.eclipse.internal.quickfix.QuickFixFactory;
import org.mapstruct.eclipse.internal.quickfix.fixes.AddIgnoreTargetMappingAnnotationQuickFix;
import org.mapstruct.eclipse.internal.quickfix.fixes.AddMethodQuickFix;

/**
 * Quick-Fix factory for error "Can't map property ..."
 *
 * @author Andreas Gudian
 */
public class CantMapPropertyQFFactory extends QuickFixFactory {
    private static final Pattern PATTERN =
        Pattern.compile( "Can't map property \"([^\"]+) ([^\"]+)\" to \"([^\"]+) ([^\"]+)\"\\..*" );

    @Override
    public List<? extends MapStructQuickFix> createQuickFix(IMarker marker) {
        List<MapStructQuickFix> result = new ArrayList<MapStructQuickFix>( 2 );
        Matcher matcher = PATTERN.matcher( getMessage( marker ) );

        if ( matcher.matches() ) {
            String srcType = matcher.group( 1 );
            String targetType = matcher.group( 3 );
            String targetProp = matcher.group( 4 );

            if ( !isGenericType( srcType ) && !isGenericType( targetType ) ) {
                result.add( new AddMethodQuickFix( targetType, srcType ) );
            }

            result.add( new AddIgnoreTargetMappingAnnotationQuickFix( Arrays.asList( targetProp ) ) );
        }

        return result;
    }

    private boolean isGenericType(String srcType) {
        return srcType.contains( "<" );
    }
}
