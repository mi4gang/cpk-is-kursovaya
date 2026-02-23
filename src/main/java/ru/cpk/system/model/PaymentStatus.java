package ru.cpk.system.model;

public enum PaymentStatus {
    PENDING("Ожидает оплаты"),
    PAID("Оплачено"),
    FAILED("Ошибка");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
