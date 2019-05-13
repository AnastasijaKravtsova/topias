package processing;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import db.dao.StatisticsViewDAO;
import db.entities.StatisticsViewEntity;
import git4idea.GitReference;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import gr.uom.java.xmi.UMLOperation;
import navigation.wrappers.Reference;
import settings.TopiasSettingsState;
import settings.enums.DiscrType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public final class Utils {

    public static String calculateSignature(PsiMethod method) {
        final PsiClass containingClass = method.getContainingClass();
        final String className;
        if (containingClass != null) {
            className = containingClass.getQualifiedName();
        } else {
            className = "";
        }
        final String methodName = method.getName();
        final StringBuilder out = new StringBuilder(50);
        out.append(className);
        out.append('.');
        out.append(methodName);
        out.append('(');
        final PsiParameterList parameterList = method.getParameterList();
        final PsiParameter[] parameters = parameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i != 0) {
                out.append(',');
            }
            final PsiType parameterType = parameters[i].getType();
            final String parameterTypeText = parameterType.getPresentableText();
            out.append(parameterTypeText);
        }
        out.append(')');
        return out.toString();
    }

    public static String calculateSignatureForEcl(UMLOperation operation) {
        StringBuilder builder = new StringBuilder();

        builder.append(operation.getClassName())
                .append(".")
                .append(operation.getName())
                .append("(");

        operation.getParameterTypeList().forEach(x -> builder.append(x).append(","));

        if (operation.getParameterTypeList().size() > 0)
            builder.deleteCharAt(builder.length() - 1);

        builder.append(")");
        return builder.toString();
    }

    public static String getCurrentBranchName(Project project) throws VcsException {
        final ProjectLevelVcsManagerImpl instance = (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(project);
        final VcsRoot gitRootPath = Arrays.stream(instance.getAllVcsRoots()).filter(x -> x.getVcs() != null)
                .filter(x -> x.getVcs().getName().equalsIgnoreCase("git"))
                .findAny().orElse(null);

        if (gitRootPath == null)
            throw new VcsException("No git repository found");

        return GitRepositoryManager.getInstance(project).getRepositories().stream().filter(x -> x.getRoot().equals(gitRootPath.getPath()))
                .map(GitRepository::getCurrentBranch)
                .filter(Objects::nonNull)
                .map(GitReference::getName)
                .findFirst().orElse("master");
    }

    public static Optional<Connection> connect(String url) {
        try {
            return Optional.of(DriverManager.getConnection(url));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return Optional.empty();
    }

    public static String getFileName(Change change) {
        return change.toString().substring(change.toString().indexOf(':') + 2);
    }

    public static String getOldFileName(Change change) {
        return change.toString().substring(change.toString().indexOf(':') + 2).split(" -> ")[0];
    }

    public static String getNewFileName(Change change) {
        return change.toString().substring(change.toString().indexOf(':') + 2).split(" -> ")[1];
    }

    public static String buildPathForSystem(Project project) {
        final StringBuilder pathBuilder = new StringBuilder().append(project.getBasePath());
        final String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if (os.contains("mac") || os.contains("darwin") || os.contains("nux")) {
            pathBuilder.append("/.idea/state.db");
        } else if (os.contains("win")) {
            pathBuilder.append("\\.idea\\state.db");
        }
        return pathBuilder.toString();
    }

    public static String trimClassName(String fullMethodSignature) {
        return fullMethodSignature.substring(0, fullMethodSignature.lastIndexOf('.'));
    }

    public static String trimMethodName(String fullMethodName) {
        return fullMethodName.substring(fullMethodName.lastIndexOf('.') + 1, fullMethodName.lastIndexOf('('));
    }
}
