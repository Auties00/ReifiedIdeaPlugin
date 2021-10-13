package it.auties.reified;

import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import it.auties.reified.annotation.Reified;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@UtilityClass
public class ReifiedUtils {
    private final String REIFIED_PATH = Reified.class.getName();
    private final String REIFIED_GENERATED_PATH = String.format("%sGenerated", REIFIED_PATH);
    private static final String REIFIED_JAVADOC = "/**\n* Generated parameter from type parameter\n*/";

    public boolean hasAnnotation(PsiTypeParameter parameter) {
        return Arrays.stream(parameter.getAnnotations())
                .map(PsiAnnotation::getQualifiedName)
                .anyMatch(REIFIED_PATH::equals);
    }

    public boolean hasGeneratedAnnotation(PsiClassType classType){
        return Arrays.stream(classType.getAnnotations())
                .map(PsiAnnotation::getQualifiedName)
                .anyMatch(REIFIED_GENERATED_PATH::equals);
    }

    public PsiAnnotation[] createGeneratedAnnotation(PsiElementFactory elementFactory, PsiClass clazz){
        return new PsiAnnotation[]{
                elementFactory.createAnnotationFromText(String.format("@%s", REIFIED_GENERATED_PATH), clazz)
        };
    }

    public PsiDocComment createJavadoc(PsiElementFactory elementFactory){
        return elementFactory.createDocCommentFromText(REIFIED_JAVADOC);
    }
}
