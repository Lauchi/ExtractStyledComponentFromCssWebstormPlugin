import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class ExtractStyledComponentHandler extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        final PsiFile openedFilePsi = e.getData(LangDataKeys.PSI_FILE);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        final PsiElement elementAt = openedFilePsi.findElementAt(editor.getCaretModel().getOffset());

        new WriteCommandAction.Simple(e.getProject(), openedFilePsi.getContainingFile()) {
            public void run() {
                openedFilePsi.add(elementAt);
            }
        }.execute();
    }
}
