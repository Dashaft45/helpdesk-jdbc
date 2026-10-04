package ru.example.helpdesk;

import ru.example.helpdesk.jdbc.JdbcTicketRepository;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketPriority;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.repository.TicketRepository;

public class Main {
    public static void main(String[] args) {
        TicketRepository repo = new JdbcTicketRepository();

        System.out.println("=== Создаём заявку из Java ===");
        Ticket t = new Ticket();
        t.setTitle("Тестовая заявка из Java");
        t.setDescription("Проверка JDBC INSERT");
        t.setStatus(TicketStatus.NEW);
        t.setPriority(TicketPriority.HIGH);
        t.setCustomerId(1L);   // Анна Петрова
        t.setCategoryId(1L);   // Сеть

        Ticket saved = repo.save(t);
        System.out.println("Создана: " + saved);

        System.out.println("\n=== Поиск по id ===");
        repo.findById(saved.getId()).ifPresent(
            found -> System.out.println("Найдена: " + found)
        );

        System.out.println("\n=== Все заявки ===");
        repo.findAll().forEach(System.out::println);

        System.out.println("\n=== Заявки со статусом NEW ===");
        repo.findByStatus(TicketStatus.NEW).forEach(System.out::println);
    }
}