package framework.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME) 
@Target(ElementType.TYPE)
public @interface Controller {
    String valeur() default "default";
}