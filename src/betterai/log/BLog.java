package betterai.log;

import arc.util.Log;

// Extends arc.util.Log prepending [BetterAI] to all log messages
public class BLog
{
    private static final String PREFIX = "[BetterAI] ";

    public static void info(Object message)
    {
        Log.info(PREFIX + message);
    }

    public static void info(Object message, Object... message2)
    {
        Log.info(PREFIX + message + " " + Join(message2));
    }

    public static void err(Object message)
    {
        Log.err(PREFIX + message);
    }

    public static void err(Object message, Object... message2)
    {
        Log.err(PREFIX + message + " " + Join(message2));
    }

    public static void warn(Object message)
    {
        Log.warn(PREFIX + message);
    }

    public static void warn(Object message, Object... message2)
    {
        Log.warn(PREFIX + message + " " + Join(message2));
    }

    public static void debug(Object message)
    {
        Log.debug(PREFIX + message);
    }

    public static void debug(Object message, Object... message2)
    {
        Log.debug(PREFIX + message + " " + Join(message2));
    }

    private static String Join(Object[] array)
    {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < array.length; i++)
        {
            builder.append(array[i].toString());
            if (i < array.length - 1) builder.append(" ");
        }

        return builder.toString();
    }
}
