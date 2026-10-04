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

        System.out.println("=== HELP DESK (JDBC + PostgreSQL) ===");

        // === Задание 1: findByEmail (существующий и несуществующий) ===
        System.out.println("\n--- Задание 1: поиск пользователя по email ---");
        users.findByEmail("anna@example.org").ifPresentOrElse(
            u -> System.out.println("Найден: " + u),
            () -> System.out.println("Не найден")
        );
        users.findByEmail("nobody@example.org").ifPresentOrElse(
            u -> System.out.println("Найден: " + u),
            () -> System.out.println("Не найден (ожидаемо)")
        );

        User anna = users.findByEmail("anna@example.org").orElseThrow();
        System.out.println("\nКлиент для сценария: " + anna);

        // Категории
        System.out.println("\nКатегории:");
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM categories ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("  #%d %s%n", rs.getLong("id"), rs.getString("name"));
            }
        }

        // Создать заявку
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

        // === Задание 3: вывод updated_at ===
        System.out.println("\n--- Задание 3: updated_at при изменениях ---");
        System.out.println("Создана:  " + t.getCreatedAt());
        System.out.println("Обновлена: " + t.getUpdatedAt());

        // 6. Найти по id
        tickets.findById(id).ifPresent(x -> System.out.println("6. Найдена: " + x));

        // 7. Назначить исполнителя
        jdbcTickets.assignTicket(id, 3L, 2L);
        System.out.println("7. Назначен исполнитель Сергей Волков (id=3)");

        // 8. Комментарий клиента
        jdbcTickets.addComment(id, anna.getId(), "Проблема появилась после обновления роутера", false);

        // 9. IN_PROGRESS -> RESOLVED
        service.changeStatus(id, TicketStatus.RESOLVED, 3L);

        // 10. Внутренний комментарий
        jdbcTickets.addComment(id, 3L, "Заменил настройки VPN-клиента, проблема решена", true);

        // 11. RESOLVED -> CLOSED
        service.changeStatus(id, TicketStatus.CLOSED, 3L);

        // Вывод обновлённой заявки
        tickets.findById(id).ifPresent(x -> {
            System.out.println("\nОбновлена: " + x.getUpdatedAt());
            System.out.println("Закрыта:   " + x.getClosedAt());
        });

        // 12. Все комментарии
        System.out.println("\n12. Все комментарии:");
        jdbcTickets.printComments(id);

        // === Задание 6: только внешние комментарии ===
        System.out.println("\n--- Задание 6: только внешние комментарии ---");
        jdbcTickets.printExternalComments(id);

        // 12.2 История
        System.out.println("\nИстория статусов:");
        jdbcTickets.printStatusHistory(id);

        // 13. JOIN-запрос
        System.out.println("\n13. Все заявки с именами:");
        jdbcTickets.printTicketsWithNames();

        // === Задание 8: карточка заявки ===
        System.out.println("\n--- Задание 8: карточка заявки ---");
        jdbcTickets.printTicketCard(id);

        System.out.println("\n14. Заявка сохранена в PostgreSQL. Перезапустите программу — данные останутся.");
    }
}