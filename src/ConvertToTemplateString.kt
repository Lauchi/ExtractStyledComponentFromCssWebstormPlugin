import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import com.intellij.psi.util.PsiTreeUtil

/**
 * Created by Juan on 22/06/2016
 */
internal class ConvertToTemplateString : AnAction("Convert to template string") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(PlatformDataKeys.PROJECT)
        val caret = event.getData(PlatformDataKeys.CARET)
        val editor = event.getData(PlatformDataKeys.EDITOR)
        var document: Document? = null
        if (editor != null) {
            document = editor.document
        }
        var psiFile: PsiFile? = null
        if (project != null && document != null) {
            psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        }

        var jsxElement: PsiElement? = null
        if (psiFile != null && caret != null) {
            val psiElement = psiFile.findElementAt(caret.offset)
            jsxElement = psiElement?.parent
        }

        if (jsxElement != null) {
            replaceHtmlElementWithStyledTag(project, psiFile, jsxElement)
            addStyledDefinitionAtEnd(jsxElement, project, psiFile)
        }
    }

    private fun replaceHtmlElementWithStyledTag(project: Project?, psiFile: PsiFile?, psiElement: PsiElement) {
        val newStyledComponentTagOpening = "fkt"
        val newStyledComponentTagClosing = "fkt"

        val styledComponentOpening = PsiFileFactory.getInstance(project!!).createFileFromText(newStyledComponentTagOpening, psiFile!!)
        val styledComponentClosing = PsiFileFactory.getInstance(project!!).createFileFromText(newStyledComponentTagClosing, psiFile!!)

        var runnable: Runnable? = null
        if (styledComponentOpening != null && styledComponentClosing != null) {
            runnable = Runnable {
                val firstChild = psiElement.firstChild.nextSibling
                firstChild.replace(styledComponentOpening)
                val lastChild = psiElement.lastChild.prevSibling
                lastChild.replace(styledComponentClosing)
            }
        }
        if (runnable != null) {
            WriteCommandAction.runWriteCommandAction(project, runnable)
        }
    }

    private fun addStyledDefinitionAtEnd(psiElement: PsiElement, project: Project?, psiFile: PsiFile?) {
        val htmlElement = psiElement.firstChild.nextSibling.text
        val styledComponentDefinition = "\n\nconst fkt = styled.$htmlElement`\n\n`;"

        val styledComponentDefinitionPsi = PsiFileFactory.getInstance(project).createFileFromText(styledComponentDefinition, psiFile!!)

        var runnable: Runnable? = null
        if (styledComponentDefinitionPsi != null) {
            runnable = Runnable {
                val lastElement = psiFile.node.lastChildNode.psi
                psiFile.addAfter(styledComponentDefinitionPsi, lastElement)
            }
        }
        if (runnable != null) {
            WriteCommandAction.runWriteCommandAction(project, runnable)
        }
    }

}
