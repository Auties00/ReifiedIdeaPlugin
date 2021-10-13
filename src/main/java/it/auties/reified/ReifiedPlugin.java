package it.auties.reified;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.light.LightFieldBuilder;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import org.apache.commons.collections.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ReifiedPlugin extends PsiAugmentProvider {
    @NotNull
    @SuppressWarnings("deprecation")
    @Override
    protected <Psi extends PsiElement> List<Psi> getAugments(@NotNull PsiElement element, @NotNull Class<Psi> type) {
        return getAugments(element, type, null);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected <Psi extends PsiElement> List<Psi> getAugments(@NotNull PsiElement element, @NotNull Class<Psi> type, @Nullable String nameHint) {
        if (!(element instanceof PsiExtensibleClass)) {
            return Collections.emptyList();
        }

        var clazz = (PsiExtensibleClass) element;
        var elementFactory = PsiElementFactory.getInstance(clazz.getProject());

        if (type != PsiField.class) {
            return Collections.emptyList();
        }

        var classFields = createFieldsForConstructors(clazz, elementFactory);
        var methodFields = createFieldsForMethods(clazz, elementFactory);
        return ListUtils.union(classFields, methodFields);
    }

    private List<LightFieldBuilder> createFieldsForConstructors(PsiExtensibleClass clazz, PsiElementFactory elementFactory) {
        return Arrays.stream(clazz.getTypeParameters())
                .filter(ReifiedUtils::hasAnnotation)
                .map(parameter -> createField(clazz, elementFactory, parameter, false))
                .collect(Collectors.toList());
    }

    private List<LightFieldBuilder> createFieldsForMethods(PsiExtensibleClass clazz, PsiElementFactory elementFactory) {
        return clazz.getOwnMethods()
                .stream()
                .map(psiMethod -> createFieldsForMethod(clazz, elementFactory, psiMethod))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<LightFieldBuilder> createFieldsForMethod(PsiExtensibleClass clazz, PsiElementFactory elementFactory, PsiMethod psiMethod) {
        return Arrays.stream(psiMethod.getTypeParameters())
                .filter(ReifiedUtils::hasAnnotation)
                .map(parameter -> createField(clazz, elementFactory, parameter, psiMethod.hasModifier(JvmModifier.STATIC)))
                .collect(Collectors.toList());
    }

    private LightFieldBuilder createField(PsiExtensibleClass clazz, PsiElementFactory elementFactory, PsiTypeParameter parameter, boolean staticModifier) {
        var classType = PsiType.getJavaLangClass(clazz.getManager(), clazz.getResolveScope()).resolve();
        var genericType = PsiType.getTypeByName(Objects.requireNonNull(parameter.getName()), clazz.getManager().getProject(), clazz.getResolveScope());
        var field = createField(clazz, elementFactory, parameter, classType, genericType);
        var modifiers = new LightModifierList(field);
        modifiers.addModifier(PsiModifier.PRIVATE);
        modifiers.addModifier(PsiModifier.FINAL);
        if(staticModifier) modifiers.addModifier(PsiModifier.STATIC);
        field.setContainingClass(clazz);
        field.setDocComment(ReifiedUtils.createJavadoc(elementFactory));
        field.setModifierList(modifiers);
        field.setNavigationElement(parameter);
        return field;
    }

    private LightFieldBuilder createField(PsiExtensibleClass clazz, PsiElementFactory elementFactory, PsiTypeParameter parameter, PsiClass classType, PsiClassType genericType) {
        return new LightFieldBuilder(
                clazz.getManager(),
                Objects.requireNonNull(parameter.getName()),
                constructGenericType(elementFactory, clazz, classType, genericType)
        );
    }

    private PsiClassType constructGenericType(PsiElementFactory elementFactory, PsiExtensibleClass parent, PsiClass classType, PsiClassType genericType) {
        return elementFactory.createType(Objects.requireNonNull(classType), genericType)
                .annotate(() -> ReifiedUtils.createGeneratedAnnotation(elementFactory, parent));
    }
}