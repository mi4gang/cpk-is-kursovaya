package ru.cpk.system.model;

public enum CertificateStatus {
    NOT_ISSUED("Не выдано"),
    ISSUED("Выдано");

    private final String label;

    CertificateStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
