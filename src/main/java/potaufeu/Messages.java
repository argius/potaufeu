package potaufeu;

import java.text.*;
import java.util.*;

public final class Messages {

    private static final ResourceBundle resourceBundle = PackagePrivate.getResourceBundle(Messages.class);

    private Messages() {
    }

    public static String message(String key, Object... args) {
        try {
            String s = resourceBundle.getString(key);
            if (args.length > 0)
                return MessageFormat.format(s, args);
            return s;
        } catch (MissingResourceException e) {
            String s = (args.length == 0) ? "" : " + " + Arrays.toString(args);
            return '!' + key + '!' + s;
        }
    }

}
