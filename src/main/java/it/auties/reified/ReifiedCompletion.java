package it.auties.reified;

import com.intellij.codeInsight.completion.CompletionLocation;
import com.intellij.codeInsight.completion.CompletionPreselectSkipper;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightVariableBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReifiedCompletion extends CompletionPreselectSkipper {
    @Override
    public boolean skipElement(LookupElement lookup, CompletionLocation location) {
        var element = lookup.getPsiElement();
        if(!(element instanceof LightVariableBuilder<?>)){
            return false;
        }

        var type = ((LightVariableBuilder<?>) element).getType();
        if(!(type instanceof PsiClassType)){
            return false;
        }

        var classType = (PsiClassType) type;
        if(!classType.hasParameters()){
            return false;
        }

        if(!ReifiedUtils.hasGeneratedAnnotation(classType)){
            return false;
        }

        var parentMethod = findMethodParent(location.getCompletionParameters().getOriginalPosition());
        if(parentMethod == null || !(parentMethod.getParent() instanceof PsiTypeParameterListOwner)){
            return false;
        }

        var parentClass = (PsiTypeParameterListOwner) parentMethod.getParent();
        var annotatedMembers = findReifiedTypeParameters(parentMethod);
        var annotatedClassMembers = findReifiedTypeParameters(parentClass);
        return noMatchingParameter(annotatedMembers, classType) && noMatchingParameter(annotatedClassMembers, classType);
    }

    @Nullable
    private PsiMethod findMethodParent(PsiElement element){
        if(element == null){
            return null;
        }

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

    private boolean noMatchingParameter(List<PsiTypeParameter> annotatedMembers, PsiClassType classType) {
        return Arrays.stream(classType.getParameters())
                .noneMatch(parameterType -> anyMatchingParameter(annotatedMembers, parameterType));
    }

    private boolean anyMatchingParameter(List<PsiTypeParameter> annotatedMembers, PsiType parameterType) {
        return annotatedMembers.stream()
                .anyMatch(annotatedType -> Objects.equals(annotatedType.getName(), parameterType.getPresentableText()));
    }
}
