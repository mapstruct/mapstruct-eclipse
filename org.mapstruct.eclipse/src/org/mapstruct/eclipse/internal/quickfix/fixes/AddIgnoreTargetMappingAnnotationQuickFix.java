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
package org.mapstruct.eclipse.internal.quickfix.fixes;

import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPINGS_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPINGS_SIMPLE_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_FQ_NAME;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_MEMBER_IGNORE;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_MEMBER_TARGET;
import static org.mapstruct.eclipse.internal.MapStructAPIConstants.MAPPING_SIMPLE_NAME;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.mapstruct.eclipse.internal.quickfix.MapStructQuickFix;
import org.mapstruct.eclipse.internal.quickfix.visitors.FindAnnotationByNameVisitor;

/**
 * Quick fix that adds {@code @Mapping( target = "<property>", ignore = true)} to the method.
 *
 * @author Andreas Gudian
 */
public class AddIgnoreTargetMappingAnnotationQuickFix extends MapStructQuickFix {

    private final List<String> properties;

    public AddIgnoreTargetMappingAnnotationQuickFix(List<String> properties) {
        this.properties = properties;
    }

    @Override
    public String getLabel() {
        String suffix = ( properties.size() > 1 ? "ies." : "y " + properties.get( 0 ) );

        return "Ignore unmapped target propert" + suffix;
    }

    @Override
    public String getDescription() {
        String suffix = ( properties.size() > 1 ? "ies." : "y " + properties.get( 0 ) );

        StringBuilder sb = new StringBuilder( "<html>Ignore target propert" );

        sb.append( suffix );
        sb.append( "<br><br>" );

        for ( String prop : properties ) {
            sb.append( "<b>@Mapping( target = \"" ).append( prop ).append( "\", ignore = true )</b><br>" );
        }

        sb.append( "</html>" );

        return sb.toString();
    }

    @Override
    protected ASTRewrite getASTRewrite(CompilationUnit unit, ASTNode nodeWithMarker, IMarker marker) {
        AST ast = unit.getAST();

        ASTRewrite rewrite = ASTRewrite.create( ast );
        MethodDeclaration method = (MethodDeclaration) nodeWithMarker;

        ListRewrite mappingList = getListForAddingMappingAnnotations( unit, properties, ast, rewrite, method );

        addMappingAnnotations( properties, ast, mappingList );

        addImportIfRequired( unit, rewrite, MAPPING_FQ_NAME );

        return rewrite;
    }

    private ListRewrite getListForAddingMappingAnnotations(CompilationUnit unit, Collection<String> properties,
                                                           AST ast, ASTRewrite rewrite, MethodDeclaration method) {

        // if there is already an @Mappings annotation, add the new @Mapping's there
        Annotation mappingsAnnotation = findAnnotation( method, MAPPINGS_FQ_NAME );
        if ( mappingsAnnotation != null ) {
            return rewrite.getListRewrite(
                ( (SingleMemberAnnotation) mappingsAnnotation ).getValue(),
                ArrayInitializer.EXPRESSIONS_PROPERTY );
        }

        // if repeatable @Mapping's are supported, then add the annotations directly to the method
        if ( supportsRepeatableMapping( unit ) ) {
            return rewrite.getListRewrite( method, MethodDeclaration.MODIFIERS2_PROPERTY );
        }

        // if we only need to add one @Mapping and there is none, yet, then add the single annotation directly
        Annotation singleMappingAnnotation = findAnnotation( method, MAPPING_FQ_NAME );
        if ( singleMappingAnnotation == null && properties.size() == 1 ) {
            return rewrite.getListRewrite( method, MethodDeclaration.MODIFIERS2_PROPERTY );
        }

        // create a new @Mappings annotation and add the @Mapping's there
        ListRewrite mappingList = addNewMappingsAnnotation( unit, rewrite, ast, method );

        if ( singleMappingAnnotation != null ) {
            rewrite.getListRewrite( method, MethodDeclaration.MODIFIERS2_PROPERTY )
                   .remove( singleMappingAnnotation, null );

            mappingList.insertFirst( singleMappingAnnotation, null );
        }
        return mappingList;
    }

    private ListRewrite addNewMappingsAnnotation(CompilationUnit unit, ASTRewrite rewrite, AST ast,
                                                 MethodDeclaration method) {
        SingleMemberAnnotation mappings = ast.newSingleMemberAnnotation();
        mappings.setTypeName( ast.newName( MAPPINGS_SIMPLE_NAME ) );

        ArrayInitializer mappingArray = ast.newArrayInitializer();
        mappings.setValue( mappingArray );

        ListRewrite annotations = rewrite.getListRewrite( method, MethodDeclaration.MODIFIERS2_PROPERTY );
        annotations.insertFirst( mappings, null );

        addImportIfRequired( unit, rewrite, MAPPINGS_FQ_NAME );

        return rewrite.getListRewrite( mappingArray, ArrayInitializer.EXPRESSIONS_PROPERTY );
    }

    private Annotation findAnnotation(MethodDeclaration method, String annotationName) {
        FindAnnotationByNameVisitor locatedAnnotation = new FindAnnotationByNameVisitor( annotationName );
        method.accept( locatedAnnotation );

        return locatedAnnotation.getLocatedNode();
    }

    private boolean supportsRepeatableMapping(CompilationUnit unit) {
        try {
            IJavaElement javaElement = unit.getJavaElement();
            if ( javaElement == null ) {
                return false;
            }

            IType mappingType = javaElement.getJavaProject().findType( MAPPING_FQ_NAME );
            if ( mappingType == null ) {
                return false;
            }

            for ( IAnnotation annotation : mappingType.getAnnotations() ) {
                if ( annotation.getElementName().equals( "Repeatable" )
                    || annotation.getElementName().equals( "java.lang.annotation.Repeatable" ) ) {
                    return true;
                }
            }
        }
        catch ( CoreException e ) {
            return false;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private void addMappingAnnotations(Collection<String> properties, AST ast, ListRewrite mappingList) {
        LinkedList<NormalAnnotation> toAdd = new LinkedList<NormalAnnotation>();
        for ( String property : properties ) {
            NormalAnnotation mapping = ast.newNormalAnnotation();
            mapping.setTypeName( ast.newName( MAPPING_SIMPLE_NAME ) );

            MemberValuePair valuePair = ast.newMemberValuePair();

            valuePair.setName( ast.newSimpleName( MAPPING_MEMBER_TARGET ) );
            StringLiteral literal = ast.newStringLiteral();
            literal.setLiteralValue( property );
            valuePair.setValue( literal );
            mapping.values().add( valuePair );

            valuePair = ast.newMemberValuePair();

            valuePair.setName( ast.newSimpleName( MAPPING_MEMBER_IGNORE ) );
            valuePair.setValue( ast.newBooleanLiteral( true ) );
            mapping.values().add( valuePair );

            toAdd.addFirst( mapping );
        }

        for ( NormalAnnotation mapping : toAdd ) {
            mappingList.insertFirst( mapping, null );
        }
    }
}
