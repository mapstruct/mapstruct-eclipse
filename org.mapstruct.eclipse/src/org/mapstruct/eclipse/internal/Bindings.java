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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Helper class to inspect various {@link IBinding}s.
 *
 * @author Andreas Gudian
 */
public class Bindings {

    /**
     * @param type the type
     * @return the method names declared in the class or a super type of it
     */
    public static Set<String> findAllMethodNames(ITypeBinding type) {
        Set<String> result = new HashSet<String>();

        collectMethodNames( type, new HashSet<ITypeBinding>(), result );

        return result;
    }

    private static void collectMethodNames(ITypeBinding curr, Set<ITypeBinding> visited, Set<String> methodNames) {
        if ( isNotJavaLangObject( curr ) && visited.add( curr ) ) {
            for ( IMethodBinding methodBinding : curr.getDeclaredMethods() ) {
                methodNames.add( methodBinding.getName() );
            }

            for ( ITypeBinding ifc : curr.getInterfaces() ) {
                collectMethodNames( ifc, visited, methodNames );
            }
            ITypeBinding superClass = curr.getSuperclass();
            if ( superClass != null ) {
                collectMethodNames( superClass, visited, methodNames );
            }
        }
    }

    private static boolean isNotJavaLangObject(ITypeBinding curr) {
        return !curr.getQualifiedName().equals( "java.lang.Object" );
    }
}
