import com.intellij.lang.javascript.psi.e4x.impl.JSXmlLiteralExpressionImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlAttribute

class PsiFileWriter(private val project: Project, private val psiFile: PsiFile) {
    fun renameHtmlTagAndDeleteClassTag(newTag: String, classNameTag: XmlAttribute?, jsXmlElement: JSXmlLiteralExpressionImpl) {
        val runnable = Runnable {
            classNameTag?.delete()
            jsXmlElement.name = newTag
        }

        WriteCommandAction.runWriteCommandAction(project, runnable)
    }

    fun addStyledCompDefinition(styledComponentDefinition: String) {
        val styledComponentDefinitionPsi = PsiFileFactory.getInstance(project).createFileFromText(styledComponentDefinition, psiFile)!!

        val runnable = Runnable {
            val lastElement = psiFile.node.lastChildNode.psi
            psiFile.addAfter(styledComponentDefinitionPsi, lastElement)
        }

        WriteCommandAction.runWriteCommandAction(project, runnable)
    }
}