import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SelectFromListDialog
import com.intellij.psi.*
import com.intellij.psi.css.CssClass
import com.intellij.psi.css.CssFile
import com.intellij.psi.css.CssRuleset
import com.intellij.psi.xml.XmlElement
import com.intellij.lang.javascript.psi.e4x.impl.JSXmlLiteralExpressionImpl
import com.intellij.psi.xml.XmlAttribute

internal class ConvertToStyledComponent : AnAction("Convert to a styled component") {

    private lateinit var project: Project
    private lateinit var dialogManager: DialogManager

    override fun actionPerformed(event: AnActionEvent) {
        project = event.getData(PlatformDataKeys.PROJECT)!!
        dialogManager = DialogManager(project)
        val caret = event.getData(PlatformDataKeys.CARET)!!
        val editor = event.getData(PlatformDataKeys.EDITOR)!!
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)!!

        val psiElement = psiFile.findElementAt(caret.offset)!!
        if (psiElement is XmlElement) {
            val jsxElement = psiElement.parent
            if (jsxElement is JSXmlLiteralExpressionImpl) {
                val styledComponentClassNames = getClassNames(jsxElement)
                addStyledDefinitionAtEnd(project, psiFile, jsxElement, styledComponentClassNames)
                replaceHtmlElementWithStyledTag(project, jsxElement, styledComponentClassNames)
            }
        }
    }

    private fun getClassNames(jsxElement: JSXmlLiteralExpressionImpl): List<String> {
        val classNameTag = getClassNameTag(jsxElement)
        val classNames = classNameTag?.value ?: return emptyList()
        return classNames.replace("\'", "").replace("\"", "").split(" ")
    }

    private fun replaceHtmlElementWithStyledTag(project: Project, jsXmlElement: JSXmlLiteralExpressionImpl, classNames: List<String>) {
        val newTag = classNames.map { name -> name.capitalize() }.last()

        val runnable = Runnable {
            val classNameTag = getClassNameTag(jsXmlElement)
            classNameTag?.delete()
            jsXmlElement.name = newTag
        }

        WriteCommandAction.runWriteCommandAction(project, runnable)
    }

    private fun getClassNameTag(jsxElement: JSXmlLiteralExpressionImpl): XmlAttribute? {
        val attributes = jsxElement.attributes
        val classNameAttributes = attributes.filter { attribute ->
            val name = attribute.name
            name == "className"
        }

        if (classNameAttributes.isEmpty()) return null
        return classNameAttributes[0]
    }

    private fun addStyledDefinitionAtEnd(project: Project, psiFile: PsiFile, jsxElement: JSXmlLiteralExpressionImpl, classNames: List<String>) {
        val htmlElement = jsxElement.name
        val extractedCssList = getCssRulesAsBlockStringFrom(jsxElement)

        var lastClassName = ""
        for (i in classNames.indices) {
            val className = classNames[i].capitalize()
            var extractedCss = ""
            if (i < extractedCssList.size) {
                extractedCss = extractedCssList[i]
            }

            val styledComponentDefinition = if (i == 0) {
                "\n\nconst $className = styled.$htmlElement`\n$extractedCss`;"
            } else {
                "\n\nconst $className = $lastClassName.extend`\n$extractedCss`;"
            }

            val styledComponentDefinitionPsi = PsiFileFactory.getInstance(project).createFileFromText(styledComponentDefinition, psiFile)

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

    private fun getCssRulesAsBlockStringFrom(jsxElement: JSXmlLiteralExpressionImpl): List<String> {
        val classNameReferences = getClassNameReferences(jsxElement)
        val classNames = getClassNames(jsxElement)

        val stringList: ArrayList<String> = arrayListOf()
            for (i in classNames.indices) {
                val psiReference = classNameReferences[i]
                if (psiReference is PsiPolyVariantReference) {
                    val multiResolve = psiReference.multiResolve(false)
                    val resolvedElements = multiResolve.map { res -> res.element }

                    val selected = if (resolvedElements.size > 1) {
                        dialogManager.getFileSelectionFromUser(resolvedElements as List<PsiElement>)
                    } else {
                        resolvedElements[0]
                    }

                    var declarationStrings = ""
                    if (selected is CssClass) {
                        val cssRuleSet = getCssRulesetFromFile(selected, classNames[i])
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

    private fun getCssRulesetFromFile(cssClassReference: CssClass, className: String): CssRuleset? {
        val cssFile = cssClassReference.containingFile
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

    private fun getClassNameReferences(jsxElement: JSXmlLiteralExpressionImpl): Array<out PsiReference> {
        val classNameTag = getClassNameTag(jsxElement)
        return classNameTag?.valueElement?.references ?: return emptyArray()
    }
}

class DialogManager(private val project: Project) {

    fun getFileSelectionFromUser(resolvedElements: List<PsiElement>): Any {
        val selectFromListDialog = SelectFromListDialog(project, resolvedElements.toTypedArray(),
                SelectFromListDialog.ToStringAspect { string -> if (string is CssClass) {
                    string.containingFile.virtualFile.name
                } else {
                    "no css File"
                } } ,
                "Source", 1)
        selectFromListDialog.showAndGet()
        return selectFromListDialog.selection[0]
    }

}
