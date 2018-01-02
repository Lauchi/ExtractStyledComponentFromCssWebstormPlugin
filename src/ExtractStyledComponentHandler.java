import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ExtractStyledComponentHandler extends AnAction {

    public ExtractStyledComponentHandler() {
        super("Create _Publish _Script _Handler");
    }

    public void actionPerformed(AnActionEvent event) {

    }

    @Override
    public void update(AnActionEvent event) {
       //
    }
}