import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.TokenSet;

public class ExtractStyledComponentHandler extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        final PsiFile openedFilePsi = e.getData(LangDataKeys.PSI_FILE);
        FileASTNode node = openedFilePsi.getNode();
        TokenSet tokenSet = TokenSet.ANY;
        ASTNode[] children = node.getChildren(tokenSet);
        for (final ASTNode child : children) {
            new WriteCommandAction.Simple(e.getProject(), openedFilePsi.getContainingFile()) {
                public void run() {
                    openedFilePsi.add(child.getPsi());
                }
            }.execute();
        }
    }
}
