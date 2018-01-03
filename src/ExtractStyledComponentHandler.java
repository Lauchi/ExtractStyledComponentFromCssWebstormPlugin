import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

public class ExtractStyledComponentHandler extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        final PsiFile openedFilePsi = e.getData(LangDataKeys.PSI_FILE);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        int selectionStart1 = editor.getSelectionModel().getSelectionStart() + 1;
        PsiElement selectionStart = openedFilePsi.findElementAt(selectionStart1);
        int selectionEnd1 = editor.getSelectionModel().getSelectionEnd() - 1;
        PsiElement selectionEnd = openedFilePsi.findElementAt(selectionEnd1);

        final PsiElement commonParent = PsiTreeUtil.findCommonParent(selectionStart, selectionEnd);

        new WriteCommandAction.Simple(e.getProject(), openedFilePsi.getContainingFile()) {
            public void run() {
                PsiElement lastElement = openedFilePsi.getNode().getLastChildNode().getPsi();
                openedFilePsi.addAfter(commonParent, lastElement);
            }
        }.execute();
    }
}
