package org.rgo;

import com.intellij.codeInsight.highlighting.BraceMatchingUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;

public class PluginAction extends AnAction {

    @Override public void actionPerformed(AnActionEvent e) {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiFile file = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
        if (file == null) {
            return;
        }

        final Caret caret = editor.getCaretModel().getCurrentCaret();
        final EditorHighlighter highlighter = ((EditorEx) editor).getHighlighter();
        final CharSequence text = editor.getDocument().getCharsSequence();

        int offset = caret.getOffset();
        FileType fileType = getFileType(file, offset);
        HighlighterIterator iterator = highlighter.createIterator(offset);

        if (iterator.atEnd()) {
            offset--;
        } else if (!BraceMatchingUtil.isLBraceToken(iterator, text, fileType)) {
            offset--;

            if (offset >= 0) {
                final HighlighterIterator i = highlighter.createIterator(offset);
                if (!BraceMatchingUtil.isRBraceToken(i, text, getFileType(file, i.getStart())))
                    offset++;
            }
        }

        if (offset < 0)
            return;

        iterator = highlighter.createIterator(offset);
        fileType = getFileType(file, iterator.getStart());

        while (!BraceMatchingUtil.isLBraceToken(iterator, text, fileType) &&
                !BraceMatchingUtil.isRBraceToken(iterator, text, fileType)) {
            if (iterator.getStart() == 0)
                return;
            iterator.retreat();
        }

        if (BraceMatchingUtil.matchBrace(text, fileType, iterator, true)) {
            moveCaret(editor, caret, iterator.getEnd());
            return;
        }
    }

    @Override
    public void update(final AnActionEvent e) {
        //Get required data keys
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        //Set visibility only in case of existing project and editor
        e.getPresentation().setVisible((project != null && editor != null));

    }

    @NotNull
    private static FileType getFileType(PsiFile file, int offset) {
        return PsiUtilBase.getPsiFileAtOffset(file, offset).getFileType();
    }

    private static void moveCaret(Editor editor, Caret caret, int offset) {
        caret.removeSelection();
        caret.moveToOffset(offset);
        EditorModificationUtil.scrollToCaret(editor);
    }
}
