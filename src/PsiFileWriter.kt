import com.intellij.lang.javascript.psi.e4x.impl.JSXmlLiteralExpressionImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlAttribute

class PsiFileWriter(private val project: Project) {
    fun renameHtmlTagAndDeleteClassTag(newTag: String, classNameTag: XmlAttribute?, jsXmlElement: JSXmlLiteralExpressionImpl) {
        val runnable = Runnable {
            classNameTag?.delete()
            jsXmlElement.name = newTag
        }

        WriteCommandAction.runWriteCommandAction(project, runnable)

    }

}