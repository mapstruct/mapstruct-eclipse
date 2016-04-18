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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 * Helper class to inspect various {@link IBinding}s.
 *
 * @author Andreas Gudian
 */
public class Bindings {

    private Bindings() {
    }

    /**
     * @param type the type
     * @return the method names declared in the class or a super type of it
     */
    public static Set<String> findAllMethodNames(ITypeBinding type) {
        Set<String> result = new HashSet<String>();

        collectMethodNames( type, new HashSet<ITypeBinding>(), result );

        return result;
    }

    private static void collectMethodNames(ITypeBinding type, Set<ITypeBinding> visited,
                                           Collection<String> methodNames) {
        if ( !isJavaLangObject( type ) && visited.add( type ) ) {
            for ( IMethodBinding methodBinding : type.getDeclaredMethods() ) {
                methodNames.add( methodBinding.getName() );
            }

            for ( ITypeBinding ifc : type.getInterfaces() ) {
                collectMethodNames( ifc, visited, methodNames );
            }

            ITypeBinding superClass = type.getSuperclass();
            if ( superClass != null ) {
                collectMethodNames( superClass, visited, methodNames );
            }
        }
    }

    private static boolean isJavaLangObject(ITypeBinding curr) {
        return curr.getQualifiedName().equals( "java.lang.Object" );
    }

    /**
     * @param annotations the annotations
     * @param annotationName the fully qualified name of the annotation to look for
     * @return {@code true}, iff the given array of annotations contains an annotation with the given name
     */
    public static boolean containsAnnotation(IAnnotationBinding[] annotations, String annotationName) {
        for ( IAnnotationBinding annotation : annotations ) {
            if ( annotation.getAnnotationType().getQualifiedName().equals( annotationName ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param type the enum type
     * @return the enum constant names of the given type
     */
    public static List<String> findAllEnumConstants(ITypeBinding type) {
        if ( !type.isEnum() ) {
            return Collections.emptyList();
        }

        IVariableBinding[] declaredFields = type.getDeclaredFields();

        List<String> result = new ArrayList<String>( declaredFields.length );

        for ( IVariableBinding field : declaredFields ) {
            result.add( field.getName() );
        }

        return result;
    }

    /**
     * Returns the qualified name of the annotation which is associated with the given {@link IMemberValuePairBinding}.
     */
    public static String getAnnotationQualifiedName(IMemberValuePairBinding binding) {
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
