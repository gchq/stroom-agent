package stroom.agent.util.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StroomExpectedException {
	abstract Class<? extends Throwable>[] exception() default {};
}
