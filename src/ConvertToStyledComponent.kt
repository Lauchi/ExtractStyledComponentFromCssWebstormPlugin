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
import com.intellij.psi.css.CssClass
import com.intellij.psi.css.CssFile
import com.intellij.psi.css.CssRuleset
import com.intellij.psi.css.CssStylesheet

internal class ConvertToStyledComponent : AnAction("Convert to a styled component") {

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
            val styledComponentClassName = getClassName(jsxElement)
            addStyledDefinitionAtEnd(project!!, psiFile!!, jsxElement, styledComponentClassName)
            replaceHtmlElementWithStyledTag(project, psiFile, jsxElement, styledComponentClassName)
        }
    }

    private fun getClassName(jsxElement: PsiElement): String {
        val classNameValue = getClassNameNameElement(jsxElement)
        val resultRaw = classNameValue?.text!!
        return resultRaw.replace("\'", "").replace("\"", "")
    }

    private fun getClassNameNameElement(classNameTag: PsiElement?): PsiElement? {
        val classNameTag = getClassNameTag(classNameTag!!)
        val firstChild = classNameTag?.firstChild
        val firstChild1 = firstChild?.nextSibling
        val nextSibling2 = firstChild1?.nextSibling
        val nextSibling3 = nextSibling2?.firstChild
        return nextSibling3?.nextSibling
    }

    private fun replaceHtmlElementWithStyledTag(project: Project?, psiFile: PsiFile?, psiElement: PsiElement, newTag: String) {
        val styledComponentTag = PsiFileFactory.getInstance(project!!).createFileFromText(newTag, psiFile!!)

        var runnable: Runnable? = null
        if (styledComponentTag != null) {
            runnable = Runnable {
                val classNameTag = getClassNameTag(psiElement)
                classNameTag?.delete()
                val startTag = psiElement.firstChild.nextSibling
                startTag.replace(styledComponentTag)
                val endTag = psiElement.lastChild.prevSibling
                endTag.replace(styledComponentTag)
            }
        }
        if (runnable != null) {
            WriteCommandAction.runWriteCommandAction(project, runnable)
        }
    }

    private fun getClassNameTag(psiElement: PsiElement): PsiElement? {

        var result: PsiElement? = null
        psiElement.children.forEach{child ->
            if (child.firstChild?.text == "className") {
                result = child
            }
        }
        return result
    }

    private fun addStyledDefinitionAtEnd(project: Project, psiFile: PsiFile, jsxElement: PsiElement, newTag: String) {
        val htmlElement = jsxElement.firstChild.nextSibling.text
        val extractedCss = getCssFrom(jsxElement)
        val styledComponentDefinition = "\n\nconst $newTag = styled.$htmlElement`\n$extractedCss\n`;"

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

    private fun getCssFrom(jsxElement: PsiElement): String {
        val classNameReference = getClassNameReference(jsxElement)
        val className = getClassNameNameElement(jsxElement)?.text!!
        val reference = classNameReference?.reference

        val resolve = reference?.resolve()

        if (resolve is CssStylesheet) {
            System.out.println("2" + resolve)
        }
        if (resolve is CssRuleset) {
            System.out.println("2" + resolve)
        }
        if (resolve is CssClass) {
            System.out.println("2" + resolve.text)
        }
        val cssFile = resolve?.containingFile

        val cssClassFromFile = getCssClassFromFile(cssFile, className)
        return "border: 1px solid black"
    }

    private fun getCssClassFromFile(cssFile: PsiFile?, className: String): PsiElement? {

        if (cssFile is CssFile) {
            val stylesheet = cssFile.stylesheet
            val rulesets = stylesheet.rulesets
            rulesets.forEach { rule ->
                rule.selectors.forEach { sel ->
                    System.out.println("2" + sel.presentableText)
                }
                System.out.println("2" + rule.block?.text)
            }
        }

        return null
    }

    private fun getClassNameReference(jsxElement: PsiElement): PsiElement? {
        val classNameTag = getClassNameTag(jsxElement!!)
        val firstChild = classNameTag?.firstChild
        val firstChild1 = firstChild?.nextSibling
        return firstChild1?.nextSibling
    }

}
