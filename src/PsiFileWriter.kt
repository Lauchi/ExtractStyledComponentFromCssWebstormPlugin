import com.intellij.lang.javascript.psi.e4x.impl.JSXmlLiteralExpressionImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlAttribute

class PsiFileWriter(private val psiFile: PsiFile) {
    fun renameHtmlTagAndDeleteClassTag(jsxElement: JSXmlLiteralExpressionImpl, newJsxTagName: String, classNameTag: XmlAttribute?) {
        val runnable = Runnable {
            classNameTag?.delete()
            jsxElement.name = newJsxTagName
        }

        WriteCommandAction.runWriteCommandAction(psiFile.project, runnable)
    }

    fun addStyledCompDefinition(styledComponentDefinition: String) {
        val styledComponentDefinitionPsi = PsiFileFactory.getInstance(psiFile.project).createFileFromText(styledComponentDefinition, psiFile)!!

        val runnable = Runnable {
            val lastElement = psiFile.node.lastChildNode.psi
            psiFile.addAfter(styledComponentDefinitionPsi, lastElement)
        }

        WriteCommandAction.runWriteCommandAction(psiFile.project, runnable)
    }
}