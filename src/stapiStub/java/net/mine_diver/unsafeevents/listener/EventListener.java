package net.mine_diver.unsafeevents.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stub - see src/stapiStub/java/README.md. Marks the method StationAPI's event bus should call.
 *
 * <p>The real annotation carries optional attributes (priority and the like); none are used here, and
 * an annotation is matched at runtime by descriptor, so a bare stub produces identical bytecode at
 * the use site.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventListener {
}
