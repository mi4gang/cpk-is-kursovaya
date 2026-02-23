package ru.cpk.system.model;

public enum ProgramType {
    PROFESSIONAL_RETRAINING("Профессиональная переподготовка"),
    ADVANCED_TRAINING("Повышение квалификации");

    private final String label;

    ProgramType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
