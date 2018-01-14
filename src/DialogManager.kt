import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SelectFromListDialog
import com.intellij.psi.PsiElement
import com.intellij.psi.css.CssClass

class DialogManager(private val project: Project) {

    fun getFileSelectionFromUser(resolvedElements: List<PsiElement>): Any? {
        val selectFromListDialog = SelectFromListDialog(project, resolvedElements.toTypedArray(),
                SelectFromListDialog.ToStringAspect { cssClass ->
                    if (cssClass is CssClass) {
                        val projectFilePath = project.basePath
                        cssClass.containingFile.virtualFile.path.replaceFirst(projectFilePath.toString(), "")
                    } else {
                        "no css File"
                    }
                },
                "Source", 0)
        val showAndGet = selectFromListDialog.showAndGet()
        return if (showAndGet) selectFromListDialog.selection[0]
        else null
    }

}