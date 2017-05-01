package potaufeu;

import java.io.*;

public final class FileLine implements Serializable {

    private static final long serialVersionUID = 334941113541918223L;

    public final int number;
    public final String text;

    public FileLine(int number, String text) {
        this.number = number;
        this.text = text;
    }

}
