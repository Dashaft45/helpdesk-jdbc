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

        public void printTicketsWithNames() {
        String sql = """
            SELECT t.id, t.title, t.status, t.priority,
                   c.name AS category_name,
                   customer.name AS customer_name,
                   assignee.name AS assignee_name,
                   t.created_at
            FROM tickets t
            JOIN users customer ON customer.id = t.customer_id
            LEFT JOIN users assignee ON assignee.id = t.assignee_id
            LEFT JOIN categories c ON c.id = t.category_id
            ORDER BY t.created_at DESC
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("#%d %s | %s | %s | Клиент: %s | Исполнитель: %s | Категория: %s%n",
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("status"),
                    rs.getString("priority"),
                    rs.getString("customer_name"),
                    rs.getString("assignee_name") != null ? rs.getString("assignee_name") : "—",
                    rs.getString("category_name") != null ? rs.getString("category_name") : "—"
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка JOIN-запроса", e);
        }
    }

        public void addComment(long ticketId, long authorId, String text, boolean internal) {
        String sql = """
            INSERT INTO ticket_comments(ticket_id, author_id, text, internal)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            ps.setLong(2, authorId);
            ps.setString(3, text);
            ps.setBoolean(4, internal);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка добавления комментария", e);
        }
    }

    public void printComments(long ticketId) {
        String sql = """
            SELECT c.id, c.text, c.internal, c.created_at,
                   u.name AS author_name, u.role AS author_role
            FROM ticket_comments c
            JOIN users u ON u.id = c.author_id
            WHERE c.ticket_id = ?
            ORDER BY c.created_at
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  [%s] %s (%s): %s%n",
                        rs.getBoolean("internal") ? "внутр." : "клиент",
                        rs.getString("author_name"),
                        rs.getString("author_role"),
                        rs.getString("text")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения комментариев", e);
        }
    }

    public void printStatusHistory(long ticketId) {
        String sql = """
            SELECT h.old_status, h.new_status, h.changed_at,
                   u.name AS changed_by_name
            FROM ticket_status_history h
            LEFT JOIN users u ON u.id = h.changed_by_id
            WHERE h.ticket_id = ?
            ORDER BY h.changed_at
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  %s -> %s (%s) — %s%n",
                        rs.getString("old_status") != null ? rs.getString("old_status") : "—",
                        rs.getString("new_status"),
                        rs.getString("changed_by_name") != null ? rs.getString("changed_by_name") : "—",
                        rs.getTimestamp("changed_at").toLocalDateTime()
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения истории", e);
        }
    }

        public void assignTicket(long ticketId, long assigneeId, long changedByUserId) {
        String selectSql = "SELECT status FROM tickets WHERE id = ? FOR UPDATE";
        String updateSql = """
            UPDATE tickets
            SET assignee_id = ?,
                status = 'IN_PROGRESS',
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        String historySql = """
            INSERT INTO ticket_status_history(ticket_id, old_status, new_status, changed_by_id)
            VALUES (?, ?::ticket_status, 'IN_PROGRESS'::ticket_status, ?)
            """;

        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                TicketStatus oldStatus;
                try (PreparedStatement ps = c.prepareStatement(selectSql)) {
                    ps.setLong(1, ticketId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Заявка не найдена");
                        oldStatus = TicketStatus.valueOf(rs.getString("status"));
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setLong(1, assigneeId);
                    ps.setLong(2, ticketId);
                    ps.executeUpdate();
                }

                if (oldStatus != TicketStatus.IN_PROGRESS) {
                    try (PreparedStatement ps = c.prepareStatement(historySql)) {
                        ps.setLong(1, ticketId);
                        ps.setString(2, oldStatus.name());
                        ps.setLong(3, changedByUserId);
                        ps.executeUpdate();
                    }
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка назначения", e);
        }
    }

        public void printExternalComments(long ticketId) {
        String sql = """
            SELECT c.id, c.text, c.created_at,
                   u.name AS author_name
            FROM ticket_comments c
            JOIN users u ON u.id = c.author_id
            WHERE c.ticket_id = ? AND c.internal = FALSE
            ORDER BY c.created_at
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("  [%s] %s: %s%n",
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("author_name"),
                        rs.getString("text"));
                }
                if (!any) System.out.println("  (нет внешних комментариев)");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения внешних комментариев", e);
        }
    }

        public void printTicketCard(long ticketId) {
        String sql = """
            SELECT t.id, t.title, t.description, t.status, t.priority,
                   t.created_at, t.updated_at, t.closed_at,
                   customer.name AS customer_name,
                   assignee.name AS assignee_name,
                   c.name AS category_name
            FROM tickets t
            JOIN users customer ON customer.id = t.customer_id
            LEFT JOIN users assignee ON assignee.id = t.assignee_id
            LEFT JOIN categories c ON c.id = t.category_id
            WHERE t.id = ?
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Заявка не найдена");
                    return;
                }
                System.out.println("╔══════════════════════════════════════════════════════╗");
                System.out.printf( "║ Заявка #%-44d ║%n", rs.getLong("id"));
                System.out.println("╠══════════════════════════════════════════════════════╣");
                System.out.printf( "║ Тема:        %-38s ║%n", rs.getString("title"));
                System.out.printf( "║ Статус:      %-38s ║%n", rs.getString("status"));
                System.out.printf( "║ Приоритет:   %-38s ║%n", rs.getString("priority"));
                System.out.printf( "║ Клиент:      %-38s ║%n", rs.getString("customer_name"));
                System.out.printf( "║ Исполнитель: %-38s ║%n",
                    rs.getString("assignee_name") != null ? rs.getString("assignee_name") : "—");
                System.out.printf( "║ Категория:   %-38s ║%n",
                    rs.getString("category_name") != null ? rs.getString("category_name") : "—");
                System.out.println("╠══════════════════════════════════════════════════════╣");
                System.out.printf( "║ Создана:     %-38s ║%n", rs.getTimestamp("created_at").toLocalDateTime());
                System.out.printf( "║ Обновлена:   %-38s ║%n", rs.getTimestamp("updated_at").toLocalDateTime());
                java.sql.Timestamp closed = rs.getTimestamp("closed_at");
                System.out.printf( "║ Закрыта:     %-38s ║%n", closed != null ? closed.toLocalDateTime() : "—");
                System.out.println("╠══════════════════════════════════════════════════════╣");
                System.out.println("║ Описание:                                            ║");
                System.out.printf( "║   %-50s ║%n", rs.getString("description"));
                System.out.println("╚══════════════════════════════════════════════════════╝");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка вывода карточки", e);
        }
    }
}