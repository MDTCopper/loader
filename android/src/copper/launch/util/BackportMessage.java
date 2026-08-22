package copper.launch.util;

/**
 * Minimal stand-ins for the {@code javax.annotation.processing}/{@code javax.tools}
 * types that are missing on Android.
 *
 * <p>These are NOT real implementations; they exist only so the classes of ecj/jdt
 * (and the mixin framework) that reference those types can be remapped to something
 * that exists on the device. See the {@code patchAndroidJar} task in the android
 * build script, which rewrites the class references to point here.</p>
 */
public class BackportMessage {
    public interface Message {

        void printMessage(Kind kind, CharSequence msg);


        void printMessage(Kind kind, CharSequence msg, Element e);


        void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a);

        void printMessage(Kind kind,
                          CharSequence msg,
                          Element e,
                          AnnotationMirror a,
                          AnnotationValue v);
    }

    public enum Kind {
        ERROR,
        WARNING,
        MANDATORY_WARNING,
        NOTE,
        OTHER
    }

    public interface Element {}
    public interface AnnotationMirror {}
    public interface AnnotationValue {}
}