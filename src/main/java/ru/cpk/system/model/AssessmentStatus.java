package ru.cpk.system.model;

public enum AssessmentStatus {
    NOT_PASSED("Не пройдена"),
    PASSED("Пройдена");

    private final String label;

    AssessmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
