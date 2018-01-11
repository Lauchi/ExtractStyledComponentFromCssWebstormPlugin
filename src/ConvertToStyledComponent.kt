import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.css.CssClass
import com.intellij.psi.css.CssFile
import com.intellij.psi.css.CssRuleset
import com.intellij.psi.xml.XmlElement
import com.intellij.lang.javascript.psi.e4x.impl.JSXmlLiteralExpressionImpl
import com.intellij.psi.xml.XmlAttribute
import org.jetbrains.plugins.scss.psi.SCSSFileImpl

internal class ConvertToStyledComponent : AnAction("Convert to a styled component") {

    private lateinit var project: Project
    private lateinit var dialogManager: DialogManager
    private lateinit var fileWriter: PsiFileWriter

    override fun actionPerformed(event: AnActionEvent) {
        project = event.getData(PlatformDataKeys.PROJECT)!!
        val caret = event.getData(PlatformDataKeys.CARET)!!
        val editor = event.getData(PlatformDataKeys.EDITOR)!!
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)!!

        dialogManager = DialogManager(project)
        fileWriter = PsiFileWriter(psiFile)

        val psiElement = psiFile.findElementAt(caret.offset)!!
        if (psiElement is XmlElement) {
            val jsxElement = psiElement.parent
            if (jsxElement is JSXmlLiteralExpressionImpl) {
                val styledComponentClassNames = getClassNames(jsxElement)
                addStyledDefinitionAtEnd(jsxElement, styledComponentClassNames)
                replaceHtmlElementWithStyledTag(jsxElement, styledComponentClassNames)
            }
        }
    }

    private fun getClassNames(jsxElement: JSXmlLiteralExpressionImpl): List<String> {
        val classNameTag = getClassNameTag(jsxElement)
        val classNames = classNameTag?.value ?: return emptyList()
        return classNames.split(" ")
    }

    private fun replaceHtmlElementWithStyledTag(jsxElement: JSXmlLiteralExpressionImpl, classNames: List<String>) {
        val classNames = classNames.map { name -> convertToValidString(name) }
        val newTagName = classNames.last()
        val classNameTagToDelete = getClassNameTag(jsxElement)

        fileWriter.renameHtmlTagAndDeleteClassTag(jsxElement, newTagName, classNameTagToDelete)
    }

    private fun getClassNameTag(jsxElement: JSXmlLiteralExpressionImpl): XmlAttribute? {
        val attributes = jsxElement.attributes
        val classNameAttributes = attributes.filter { attribute -> attribute.name == "className" }

        if (classNameAttributes.isEmpty()) return null
        return classNameAttributes[0]
    }

    private fun addStyledDefinitionAtEnd(jsxElement: JSXmlLiteralExpressionImpl, classNames: List<String>) {
        val htmlElement = jsxElement.name
        val extractedCssList = getCssRulesAsBlockStringFrom(jsxElement)

        var lastClassName = ""
        for (i in classNames.indices) {
            val className = convertToValidString(classNames[i])
            var extractedCss = ""
            if (i < extractedCssList.size) {
                extractedCss = extractedCssList[i]
            }

            val styledComponentDefinition = if (i == 0) {
                "\n\nconst $className = styled.$htmlElement`\n$extractedCss`;"
            } else {
                "\n\nconst $className = $lastClassName.extend`\n$extractedCss`;"
            }

            fileWriter.addStyledCompDefinition(styledComponentDefinition)

            lastClassName = className
        }
    }

    private fun convertToValidString(rawString: String): String {
        val split = rawString.split("-", "_")
        val map = split.map { s -> s.capitalize() }
        val fold = map.fold("") { concat, s -> concat + s }
        return fold
    }

    private fun getCssRulesAsBlockStringFrom(jsxElement: JSXmlLiteralExpressionImpl): List<String> {
        val classNameReferences = getClassNameReferences(jsxElement)

        val stringList: ArrayList<String> = arrayListOf()
        for(psiReference in classNameReferences) {
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
                    val cssRuleSet = getCssRulesetFromFile(selected)
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

    private fun getCssRulesetFromFile(cssClassReference: CssClass): CssRuleset? {
        val cssFile = cssClassReference.containingFile
        var ruleSet: CssRuleset? = null
        if (cssFile is CssFile) {
            val stylesheet = cssFile.stylesheet
            val rulesets = stylesheet.rulesets
            rulesets.forEach { rule ->
                rule.selectors.forEach { sel ->
                    if (sel.text == "." + cssClassReference.name) ruleSet = rule
                }
            }
        } else if (cssFile is SCSSFileImpl) {
            val stylesheet = cssFile.stylesheet
            val rulesets = stylesheet.rulesets
            rulesets.forEach { rule ->
                rule.selectors.forEach { sel ->
                    if (sel.text == "." + cssClassReference.name) ruleSet = rule
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