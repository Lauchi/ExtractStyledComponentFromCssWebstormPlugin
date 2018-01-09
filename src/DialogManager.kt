import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SelectFromListDialog
import com.intellij.psi.PsiElement
import com.intellij.psi.css.CssClass

class DialogManager(private val project: Project) {

    fun getFileSelectionFromUser(resolvedElements: List<PsiElement>): Any {
        val selectFromListDialog = SelectFromListDialog(project, resolvedElements.toTypedArray(),
                SelectFromListDialog.ToStringAspect { string ->
                    if (string is CssClass) {
                        string.containingFile.virtualFile.name
                    } else {
                        "no css File"
                    }
                },
                "Source", 1)
        selectFromListDialog.showAndGet()
        return selectFromListDialog.selection[0]
    }

}