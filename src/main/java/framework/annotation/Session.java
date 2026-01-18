package framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Session {
    String action(); // "get", "set", "remove"
    String key();    // Nom de la variable de session
    String value() default ""; // Valeur pour "set" (optionnel)
}