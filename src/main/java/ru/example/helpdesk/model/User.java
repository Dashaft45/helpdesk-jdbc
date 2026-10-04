package ru.example.helpdesk.model;

public class User {
    private Long id;
    private String name;
    private String email;
    private String role;
    private Long departmentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    @Override
    public String toString() {
        return "User#" + id + " " + name + " (" + email + ", " + role + ")";
    }
}