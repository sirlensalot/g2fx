package org.g2fx.g2lib.util;

import java.util.logging.*;

public class DualConsoleHandler extends StreamHandler {

    public static final SimpleFormatter FORMATTER = new SimpleFormatter() {
        @Override
        public String format(LogRecord record) {
            String n = record.getLoggerName();
            record.setLoggerName(n.substring(n.lastIndexOf('.')+1));
            return super.format(record);
        }
    };
    private final ConsoleHandler stderrHandler = new ConsoleHandler();

    public DualConsoleHandler() {
        super(System.out, FORMATTER);
        stderrHandler.setFormatter(FORMATTER);
    }

    @Override
    public void publish(LogRecord record) {
        if (record.getLevel().intValue() <= Level.INFO.intValue()) {
            super.publish(record);
            super.flush();
        } else {
            stderrHandler.publish(record);
            stderrHandler.flush();
        }
    }
}
