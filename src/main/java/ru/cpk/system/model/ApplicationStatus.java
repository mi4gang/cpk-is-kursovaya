package ru.cpk.system.model;

public enum ApplicationStatus {
    SUBMITTED("Заявка подана"),
    DOCS_APPROVED("Документы проверены"),
    TRIAL_ACCESS("Пробный доступ"),
    PAID("Оплачено"),
    IN_PROGRESS("Обучение"),
    COMPLETED("Завершено"),
    REJECTED("Отклонено");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
