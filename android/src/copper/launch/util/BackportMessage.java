package copper.launch.util;

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
