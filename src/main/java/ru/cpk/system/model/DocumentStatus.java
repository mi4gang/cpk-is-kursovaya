package ru.cpk.system.model;

public enum DocumentStatus {
    PENDING("На проверке"),
    APPROVED("Подтверждены"),
    REJECTED("Отклонены");

    private final String label;

    DocumentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
