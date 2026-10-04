package ru.example.helpdesk;

import ru.example.helpdesk.jdbc.JdbcTicketRepository;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketPriority;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.repository.TicketRepository;
import ru.example.helpdesk.service.TicketService;

public class Main {
    public static void main(String[] args) {
        TicketRepository repo = new JdbcTicketRepository();
        TicketService service = new TicketService();

        // 1. Создаём заявку
        Ticket t = new Ticket();
        t.setTitle("Проблема с VPN");
        t.setDescription("Не подключается к корпоративной сети");
        t.setStatus(TicketStatus.NEW);
        t.setPriority(TicketPriority.HIGH);
        t.setCustomerId(1L);
        t.setCategoryId(1L);

        repo.save(t);
        System.out.println("Создана: " + t);
        long id = t.getId();

        // 2. NEW -> IN_PROGRESS
        service.changeStatus(id, TicketStatus.IN_PROGRESS, 2L);

        // 3. IN_PROGRESS -> RESOLVED
        service.changeStatus(id, TicketStatus.RESOLVED, 2L);

        // 4. RESOLVED -> CLOSED
        service.changeStatus(id, TicketStatus.CLOSED, 2L);

        // 5. Финальный статус
        repo.findById(id).ifPresent(x -> System.out.println("Финальный статус: " + x));

        // 6. Проверка отката
        System.out.println("\n=== Проверка отката ===");
        try {
            service.changeStatus(id, TicketStatus.NEW, 2L);
        } catch (IllegalArgumentException e) {
            System.out.println("Ожидаемая ошибка: " + e.getMessage());
        }

        repo.findById(id).ifPresent(x -> System.out.println("После неудачной попытки: " + x));

        // 7. JOIN-запрос
        System.out.println("\n=== Заявки с именами (JOIN) ===");
        ((JdbcTicketRepository) repo).printTicketsWithNames();
    }
}