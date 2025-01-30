/*
 * Copyright (c) 2025. Leon<leondevlifelog@gmail.com>. All rights reserved.
 * SPDX-License-Identifier: MIT
 */

package com.github.leondevlifelog.gitea.tasks.com.github.leondevlifelog.gitea.actions;

import com.github.leondevlifelog.gitea.authentication.GiteaAccountsUtil;
import com.intellij.dvcs.repo.Repository;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.remote.GitRepositoryHostingService;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import io.ktor.http.ContentType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;


public class OpenRepositoryLineAction  extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        //project file or something else is not valid
        if (project == null || editor == null || file == null || !editor.getCaretModel().getCurrentCaret().isValid()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        //file is not tracked or no repository exists
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        if (changeListManager.isUnversioned(file)) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        //current branch is not tracked by remote
        GitRepository repository = GitRepositoryManager.getInstance(project).getRepositoryForFile(file);
        GitLocalBranch localBranch = null != repository? repository.getCurrentBranch() : null;
        if(null == localBranch || null == localBranch.findTrackedBranch(repository)){
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setEnabledAndVisible(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            GitRepository repository = GitRepositoryManager.getInstance(project).getRepositoryForFile(file);
            GitRemoteBranch remoteBranch = repository.getCurrentBranch().findTrackedBranch(repository);
            GitRemote remote = remoteBranch.getRemote();
            int currentLine = editor.getCaretModel().getPrimaryCaret().getLogicalPosition().getLine() + 1;
            String url = StringUtils.removeEnd(remote.getFirstUrl(),".git");
            String filepath = file.getPath().substring(repository.getRoot().getPath().length() + 1);
            String fullUrl = String.format("%s/src/branch/%s/%s#L%s",url,repository.getCurrentBranch().getName(),filepath,currentLine);
            ApplicationManager.getApplication().invokeLater(() -> {
                int result = Messages.showYesNoDialog(String.format("Do you want to open \"%s\"",fullUrl), "Open URL in Your Browser?",Messages.getQuestionIcon());
                if(result == Messages.YES){
                    BrowserUtil.browse(fullUrl);
                }
            });
        });
    }
}
