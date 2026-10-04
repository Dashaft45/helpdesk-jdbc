package ru.example.helpdesk.service;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.TicketStatus;

import java.sql.*;

public class TicketService {

    public void changeStatus(long ticketId, TicketStatus newStatus, long changedByUserId) {
        String selectSql = "SELECT status FROM tickets WHERE id = ? FOR UPDATE";
        String updateSql = """
            UPDATE tickets
            SET status = ?::ticket_status,
                updated_at = CURRENT_TIMESTAMP,
                closed_at = CASE WHEN ?::ticket_status = 'CLOSED'
                                 THEN CURRENT_TIMESTAMP
                                 ELSE closed_at END
            WHERE id = ?
            """;
        String historySql = """
            INSERT INTO ticket_status_history(ticket_id, old_status, new_status, changed_by_id)
            VALUES (?, ?::ticket_status, ?::ticket_status, ?)
            """;

        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                TicketStatus oldStatus;

                try (PreparedStatement ps = c.prepareStatement(selectSql)) {
                    ps.setLong(1, ticketId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Заявка #" + ticketId + " не найдена");
                        }
                        oldStatus = TicketStatus.valueOf(rs.getString("status"));
                    }
                }

                validateTransition(oldStatus, newStatus);

                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus.name());
                    ps.setString(2, newStatus.name());
                    ps.setLong(3, ticketId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(historySql)) {
                    ps.setLong(1, ticketId);
                    ps.setString(2, oldStatus.name());
                    ps.setString(3, newStatus.name());
                    ps.setLong(4, changedByUserId);
                    ps.executeUpdate();
                }

                c.commit();
                System.out.println("Статус изменён: " + oldStatus + " -> " + newStatus);

            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка изменения статуса", e);
        }
    }

    private void validateTransition(TicketStatus oldStatus, TicketStatus newStatus) {
        boolean allowed = switch (oldStatus) {
            case NEW        -> newStatus == TicketStatus.IN_PROGRESS || newStatus == TicketStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == TicketStatus.RESOLVED  || newStatus == TicketStatus.CANCELLED;
            case RESOLVED   -> newStatus == TicketStatus.CLOSED     || newStatus == TicketStatus.IN_PROGRESS;
            case CLOSED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new IllegalArgumentException(
                "Недопустимый переход: " + oldStatus + " -> " + newStatus);
        }
    }
}