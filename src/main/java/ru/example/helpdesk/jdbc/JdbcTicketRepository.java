package ru.example.helpdesk.jdbc;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketPriority;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.repository.TicketRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTicketRepository implements TicketRepository {

    @Override
    public Ticket save(Ticket ticket) {
        String sql = """
            INSERT INTO tickets(title, description, status, priority,
                                customer_id, assignee_id, category_id)
            VALUES (?, ?, ?::ticket_status, ?::ticket_priority, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getStatus().name());
            ps.setString(4, ticket.getPriority().name());
            ps.setLong(5, ticket.getCustomerId());

            if (ticket.getAssigneeId() == null) ps.setNull(6, Types.BIGINT);
            else ps.setLong(6, ticket.getAssigneeId());

            if (ticket.getCategoryId() == null) ps.setNull(7, Types.BIGINT);
            else ps.setLong(7, ticket.getCategoryId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ticket.setId(rs.getLong("id"));
                    ticket.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    ticket.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
            return ticket;
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось сохранить заявку", e);
        }
    }

    @Override
    public Optional<Ticket> findById(long id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapTicket(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска заявки", e);
        }
    }

    @Override
    public List<Ticket> findAll() {
        String sql = "SELECT * FROM tickets ORDER BY id";
        List<Ticket> result = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapTicket(rs));
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения заявок", e);
        }
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        String sql = "SELECT * FROM tickets WHERE status = ?::ticket_status ORDER BY created_at DESC";
        List<Ticket> result = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapTicket(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска по статусу", e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM tickets WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления", e);
        }
    }

    private Ticket mapTicket(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setId(rs.getLong("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setStatus(TicketStatus.valueOf(rs.getString("status")));
        t.setPriority(TicketPriority.valueOf(rs.getString("priority")));
        t.setCustomerId(rs.getLong("customer_id"));

        long assigneeId = rs.getLong("assignee_id");
        t.setAssigneeId(rs.wasNull() ? null : assigneeId);

        long categoryId = rs.getLong("category_id");
        t.setCategoryId(rs.wasNull() ? null : categoryId);

        t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        t.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        Timestamp closed = rs.getTimestamp("closed_at");
        t.setClosedAt(closed == null ? null : closed.toLocalDateTime());

        return t;
    }
}