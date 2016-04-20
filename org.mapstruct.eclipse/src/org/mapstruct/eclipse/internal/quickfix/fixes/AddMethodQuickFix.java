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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.mapstruct.eclipse.internal.quickfix.MapStructQuickFix;

/**
 * Quick fix that adds a method declaration.
 *
 * @author Andreas Gudian
 */
public class AddMethodQuickFix extends MapStructQuickFix {

    private final String resultType;
    private final String sourceType;
    private final String resultTypeSimpleName;
    private final String sourceTypeSimpleName;

    public AddMethodQuickFix(String resultType, String sourceType) {
        this.resultType = resultType;
        this.sourceType = sourceType;

        this.resultTypeSimpleName = toSimpleName( resultType );
        this.sourceTypeSimpleName = toSimpleName( sourceType );
    }

    @Override
    public String getLabel() {
        return "Add method: " + getMethodDeclarationString();
    }

    private String getMethodDeclarationString() {
        return resultTypeSimpleName + " to" + capitalize( resultTypeSimpleName ) + "( "
            + sourceTypeSimpleName + " " + uncapitalize( sourceTypeSimpleName ) + " )";
    }

    @Override
    public String getDescription() {
        return "<html><b>" + getMethodDeclarationString() + "</b></html>";
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ASTRewrite getASTRewrite(CompilationUnit unit, ASTNode nodeWithMarker, IMarker marker) {
        AST ast = unit.getAST();

        ASTRewrite rewrite = ASTRewrite.create( ast );
        MethodDeclaration method = (MethodDeclaration) nodeWithMarker;

        MethodDeclaration newMethod = ast.newMethodDeclaration();
        newMethod.modifiers().addAll( ast.newModifiers( method.getModifiers() ) );
        newMethod.setName( ast.newSimpleName( "to" + capitalize( resultTypeSimpleName ) ) );

        newMethod.setReturnType2( createType( ast, resultTypeSimpleName ) );

        SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();
        parameter.setType( createType( ast, sourceTypeSimpleName ) );
        parameter.setName( ast.newSimpleName( uncapitalize( sourceTypeSimpleName ) ) );
        newMethod.parameters().add( parameter );

        ListRewrite listRewrite =
            rewrite.getListRewrite( method.getParent(), TypeDeclaration.BODY_DECLARATIONS_PROPERTY );
        listRewrite.insertAfter( newMethod, method, null );

        addImportIfRequired( unit, rewrite, resultType );
        addImportIfRequired( unit, rewrite, sourceType );

        return rewrite;
    }

    private Type createType(AST ast, String type) {
        if ( isPrimitive( type ) ) {
            return ast.newPrimitiveType( PrimitiveType.toCode( type ) );
        }

        return ast.newSimpleType( ast.newSimpleName( type ) );
    }
}
