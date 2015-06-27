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
package org.mapstruct.eclipse.internal.quickfix;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.mapstruct.eclipse.internal.quickfix.visitors.FindMethodByPositionVisitor;

/**
 * Base class for quick fixes
 *
 * @author Andreas Gudian
 */
@SuppressWarnings("restriction")
public abstract class MapStructQuickFix implements IMarkerResolution2 {

    @Override
    public void run(IMarker marker) {
        ICompilationUnit compilationUnit = null;
        try {
            IResource resource = marker.getResource();
            IJavaElement javaElement = JavaCore.create( resource );

            compilationUnit = javaElement.getAdapter( ICompilationUnit.class );
            IEditorInput input = EditorUtility.getEditorInput( compilationUnit );
            if ( input != null ) {
                CompilationUnit astCompilationUnit = toAST( compilationUnit );

                ASTNode locatedNode =
                    locateASTNodeForSartingOffset( findProblemStart( input, marker ), astCompilationUnit );
                if ( locatedNode != null ) {
                    ASTRewrite rewrite = getASTRewrite( astCompilationUnit, locatedNode, marker );

                    if ( rewrite != null ) {
                        compilationUnit.applyTextEdit( rewrite.rewriteAST(), null );
                        compilationUnit.becomeWorkingCopy( null );
                        compilationUnit.commitWorkingCopy( true, null );
                        compilationUnit.discardWorkingCopy();
                        marker.delete();
                    }
                }
            }
        }
        catch ( CoreException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param input editor input
     * @param marker the marker
     * @return the offset of the problem, either as currently computed in an open (dirty) editor, or as originally
     *         stated in the marker
     * @throws CoreException
     */
    @SuppressWarnings("unchecked")
    private static int findProblemStart(IEditorInput input, IMarker marker) throws CoreException {
        IAnnotationModel model =
            JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getAnnotationModel( input );
        if ( model != null ) {
            Iterator<Annotation> iter = model.getAnnotationIterator();

            while ( iter.hasNext() ) {
                Annotation curr = iter.next();
                if ( curr instanceof MarkerAnnotation ) {
                    MarkerAnnotation annot = (MarkerAnnotation) curr;
                    if ( marker.equals( annot.getMarker() ) ) {
                        Position pos = model.getPosition( annot );
                        if ( pos != null ) {
                            return pos.getOffset();
                        }
                    }
                }
            }
        }

        Integer charStart = (Integer) marker.getAttribute( IMarker.CHAR_START );

        return charStart != null ? charStart : -1;
    }

    private ASTNode locateASTNodeForSartingOffset(int problemOffset, CompilationUnit astCompilationUnit)
        throws CoreException {
        FindMethodByPositionVisitor visitor = new FindMethodByPositionVisitor( problemOffset );
        astCompilationUnit.accept( visitor );
        return visitor.getLocatedNode();
    }

    /**
     * @param unit the compilation unit
     * @param nodeWithMarker the ASTNode that is located at the start position of the marker
     * @param marker the marker
     * @return the rewrite to be performed, or <code>null</code> in case no changes are to be performed
     */
    protected abstract ASTRewrite getASTRewrite(CompilationUnit unit, ASTNode nodeWithMarker, IMarker marker);

    /**
     * Add an import statement for the fullyQualifiedName if it is not yet imported
     *
     * @param compilationUnit the compilation unit
     * @param rewrite the rewrite to modify
     * @param fullyQualifiedName the fully qualified name of the type to add an import for
     */
    protected void addImportIfRequired(CompilationUnit compilationUnit, ASTRewrite rewrite, String fullyQualifiedName) {
        if ( !hasImport( compilationUnit, fullyQualifiedName ) ) {
            AST ast = compilationUnit.getAST();
            ImportDeclaration declaration = ast.newImportDeclaration();
            declaration.setName( ast.newName( fullyQualifiedName ) );

            rewrite.getListRewrite( compilationUnit, CompilationUnit.IMPORTS_PROPERTY ).insertLast( declaration, null );
        }
    }

    private static boolean hasImport(CompilationUnit compilationUnit, String fullyQualifiedName) {
        for ( Object obj : compilationUnit.imports() ) {
            ImportDeclaration importDec = (ImportDeclaration) obj;
            if ( importDec.getName().getFullyQualifiedName().equals( fullyQualifiedName ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Image getImage() {
        return JavaPluginImages.get( JavaPluginImages.IMG_CORRECTION_CHANGE );
    }

    private static CompilationUnit toAST(ICompilationUnit unit) {
        CompilationUnit astRoot = SharedASTProvider.getAST( unit, SharedASTProvider.WAIT_YES, null );
        if ( astRoot == null ) {
            astRoot = ASTResolving.createQuickFixAST( unit, null );
        }
        return astRoot;
    }
}
