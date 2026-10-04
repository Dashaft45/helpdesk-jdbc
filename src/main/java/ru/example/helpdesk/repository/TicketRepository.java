package ru.example.helpdesk.repository;

import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketStatus;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);
    Optional<Ticket> findById(long id);
    List<Ticket> findAll();
    List<Ticket> findByStatus(TicketStatus status);
    boolean deleteById(long id);
}