import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;

public class ExtractStyledComponentHandler extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Object data = e.getData(LangDataKeys.PSI_FILE);
    }
}
