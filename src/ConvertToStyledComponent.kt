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
            val styledComponentClassNames = getClassNames(jsxElement)
            addStyledDefinitionAtEnd(project!!, psiFile!!, jsxElement, styledComponentClassNames)
            replaceHtmlElementWithStyledTag(project, psiFile, jsxElement, styledComponentClassNames)
        }
    }

    private fun getClassNames(jsxElement: PsiElement): List<String> {
        val className = getClassNameAttribute(jsxElement)
        var resultRaw = className?.text
        if (resultRaw == null) resultRaw = getHtmlTag(jsxElement)
        return resultRaw.replace("\'", "").replace("\"", "").split(" ")
    }

    private fun getClassNameAttribute(classNameTag: PsiElement?): PsiElement? {
        val classNameTag = getClassNameTag(classNameTag!!)
        val firstChild = classNameTag?.firstChild
        val firstChild1 = firstChild?.nextSibling
        val nextSibling2 = firstChild1?.nextSibling
        val nextSibling3 = nextSibling2?.firstChild
        return nextSibling3?.nextSibling
    }

    private fun replaceHtmlElementWithStyledTag(project: Project?, psiFile: PsiFile?, psiElement: PsiElement, classNames: List<String>) {
        val newTag = classNames.map { name -> name.capitalize() }.last()
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

    private fun addStyledDefinitionAtEnd(project: Project, psiFile: PsiFile, jsxElement: PsiElement, classNames: List<String>) {
        val htmlElement = getHtmlTag(jsxElement)
        val extractedCssList = getCssRulesAsBlockStringFrom(jsxElement)

        var lastClassName = ""
        for (i in classNames.indices) {
            val className = classNames[i].capitalize()
            var extractedCss = ""
            if (extractedCssList.size < i) {
                extractedCss = extractedCssList[i]
            }

            val styledComponentDefinition = if (i == 0) {
                "\n\nconst $className = styled.$htmlElement`\n$extractedCss`;"
            } else {
                "\n\nconst $className = $lastClassName.extend`\n$extractedCss`;"
            }

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
            lastClassName = className
        }
    }

    private fun getHtmlTag(jsxElement: PsiElement): String {
        return jsxElement.firstChild.nextSibling.text
    }

    private fun getCssRulesAsBlockStringFrom(jsxElement: PsiElement): List<String> {
        val classNameReference = getClassNameReferences(jsxElement)
        val classNames = getClassNameAttribute(jsxElement)?.text?.split(" ")

        var stringList: ArrayList<String> = arrayListOf()
        if (classNames != null) {
            for (i in classNames.indices) {
                val psiReference = classNameReference?.references!![i]
                val cssClass = psiReference?.resolve()

                var declarationStrings = ""
                if (cssClass is CssClass) {
                    val cssRuleSet = getCssClassFromFile(cssClass, classNames[i])
                    cssRuleSet?.block?.declarations?.forEach { dec ->
                        val rule = dec.text
                        declarationStrings += "\t$rule;\n"
                    }
                }
                stringList.add(declarationStrings)
            }
        }

        return stringList
    }

    private fun getCssClassFromFile(cssClassReference: CssClass?, className: String): CssRuleset? {
        val cssFile = cssClassReference?.containingFile
        var ruleSet: CssRuleset? = null
        if (cssFile is CssFile) {
            val stylesheet = cssFile.stylesheet
            val rulesets = stylesheet.rulesets
            rulesets.forEach { rule ->
                rule.selectors.forEach { sel ->
                    if (sel.text == "." + className) ruleSet = rule
                }
            }
        }
        return ruleSet
    }

    private fun getClassNameReferences(jsxElement: PsiElement): PsiElement? {
        val classNameTag = getClassNameTag(jsxElement!!)
        val firstChild = classNameTag?.firstChild
        val firstChild1 = firstChild?.nextSibling
        return firstChild1?.nextSibling
    }
}