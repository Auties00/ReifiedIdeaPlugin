package it.auties.reified;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReifiedAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiReferenceExpression)) {
            return;
        }

        var parentMethod = findMethodParent(element);
        if(parentMethod == null){
            return;
        }

        var qualified = ((PsiReferenceExpression) element).getQualifierExpression();
        if(qualified == null){
            return;
        }

        var type = qualified.getType();
        if(!(type instanceof PsiClassType)){
            return;
        }

        var classType = (PsiClassType) type;
        if(!classType.hasParameters()){
            return;
        }

        if(!ReifiedUtils.hasGeneratedAnnotation(classType)){
            return;
        }

        if(!(parentMethod.getParent() instanceof PsiTypeParameterListOwner)){
            return;
        }

        var annotatedMembers = findReifiedTypeParameters(parentMethod);
        var parentClass = (PsiTypeParameterListOwner) parentMethod.getParent();
        var annotatedClassMembers = findReifiedTypeParameters(parentClass);
        if(hasMatchingParameter(annotatedMembers, classType) || hasMatchingParameter(annotatedClassMembers, classType)){
            return;
        }

        holder.newAnnotation(HighlightSeverity.ERROR, "Reified type parameter is not in this method's scope")
                .create();
    }

    @Nullable
    private PsiMethod findMethodParent(PsiElement element){
        var parent = element.getParent();
        if(parent == null){
            return null;
        }

        if(parent instanceof PsiMethod){
            return (PsiMethod) parent;
        }

        return findMethodParent(parent);
    }

    @NotNull
    private List<PsiTypeParameter> findReifiedTypeParameters(PsiTypeParameterListOwner parentMethod) {
        return Arrays.stream(parentMethod.getTypeParameters())
                .filter(ReifiedUtils::hasAnnotation)
                .collect(Collectors.toUnmodifiableList());
    }

    private boolean hasMatchingParameter(List<PsiTypeParameter> annotatedMembers, PsiClassType classType) {
        return Arrays.stream(classType.getParameters())
                .anyMatch(parameterType -> annotatedMembers.stream()
                        .anyMatch(annotatedType -> Objects.equals(annotatedType.getName(), parameterType.getPresentableText())));
    }
}
