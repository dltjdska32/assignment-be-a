package com.assginment.be_a.infra.config;

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.SQLException;

@Component
public class P6SpyFormatter extends JdbcEventListener implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId,
                                String now,
                                long elapsed,
                                String category,
                                String prepared,
                                String sql,
                                String url) {

        StringBuilder sb = new StringBuilder();

        sb.append(category)
                .append(" ")
                .append(elapsed)
                .append("ms");

        if (StringUtils.hasText(sql)) {
            sb.append(highlight(format(sql)));
        }

        return sb.toString();
    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation,
                                     SQLException e) {

        P6SpyOptions
                .getActiveInstance()
                .setLogMessageFormat(getClass().getName());
    }

    private String highlight(String format) {

        return FormatStyle
                .HIGHLIGHT
                .getFormatter()
                .format(format);
    }

    private String format(String sql) {

        if(isDDL(sql)){
            return FormatStyle
                    .DDL
                    .getFormatter()
                    .format(sql);
        } else if (isDML(sql)) {
            return FormatStyle
                    .BASIC
                    .getFormatter()
                    .format(sql);
        }

        return sql;
    }

    private boolean isDML(String sql) {
        return sql.startsWith("create")
                || sql.startsWith("alter")
                || sql.startsWith("drop")
                || sql.startsWith("comment");
    }

    private boolean isDDL(String sql) {
        return sql.startsWith("select")
                || sql.startsWith("insert")
                || sql.startsWith("update")
                || sql.startsWith("delete");
    }
}