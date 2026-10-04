package ru.example.helpdesk;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.jdbc.JdbcTicketRepository;
import ru.example.helpdesk.jdbc.JdbcUserRepository;
import ru.example.helpdesk.model.*;
import ru.example.helpdesk.repository.TicketRepository;
import ru.example.helpdesk.repository.UserRepository;
import ru.example.helpdesk.service.TicketService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {
    public static void main(String[] args) throws Exception {
        TicketRepository tickets = new JdbcTicketRepository();
        UserRepository users = new JdbcUserRepository();
        TicketService service = new TicketService();
        JdbcTicketRepository jdbcTickets = (JdbcTicketRepository) tickets;

        // 1. Подключение к PostgreSQL — уже проверено
        System.out.println("=== HELP DESK (JDBC + PostgreSQL) ===");

        // 2. Найти клиента по email
        User anna = users.findByEmail("anna@example.org").orElseThrow();
        System.out.println("Клиент: " + anna);

        // 3. Список категорий
        System.out.println("\nКатегории:");
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM categories ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("  #%d %s%n", rs.getLong("id"), rs.getString("name"));
            }
        }

        // 4. Создать заявку от Анны
        Ticket t = new Ticket();
        t.setTitle("Проблема с VPN");
        t.setDescription("Не подключается к корпоративной сети");
        t.setStatus(TicketStatus.NEW);
        t.setPriority(TicketPriority.HIGH);
        t.setCustomerId(anna.getId());
        t.setCategoryId(1L);
        tickets.save(t);
        long id = t.getId();
        System.out.println("\n5. Создана заявка: " + t);

        // 6. Найти по id
        tickets.findById(id).ifPresent(x -> System.out.println("6. Найдена: " + x));

        // 7. Назначить исполнителя (Сергей, id=3)
        jdbcTickets.assignTicket(id, 3L);
        System.out.println("7. Назначен исполнитель Сергей Волков (id=3)");

        // 8. Комментарий клиента
        jdbcTickets.addComment(id, anna.getId(), "Проблема появилась после обновления роутера", false);

        // 9. IN_PROGRESS -> RESOLVED
        service.changeStatus(id, TicketStatus.RESOLVED, 3L);

        // 10. Внутренний комментарий сотрудника
        jdbcTickets.addComment(id, 3L, "Заменил настройки VPN-клиента, проблема решена", true);

        // 11. RESOLVED -> CLOSED
        service.changeStatus(id, TicketStatus.CLOSED, 3L);

        // 12. Комментарии и история
        System.out.println("\n12. Комментарии:");
        jdbcTickets.printComments(id);

        System.out.println("\nИстория статусов:");
        jdbcTickets.printStatusHistory(id);

        // 13. JOIN-запрос
        System.out.println("\n13. Все заявки с именами:");
        jdbcTickets.printTicketsWithNames();

        // 14. Перезапустите программу — заявка останется в БД
        System.out.println("\n14. Перезапустите программу и убедитесь: заявка сохранилась в PostgreSQL.");
    }
}