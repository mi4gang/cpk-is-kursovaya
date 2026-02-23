package ru.cpk.system.model;

public enum AccessStatus {
    NO_ACCESS("Нет доступа"),
    TRIAL_ACCESS("Пробный доступ"),
    FULL_ACCESS("Полный доступ");

    private final String label;

    AccessStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
