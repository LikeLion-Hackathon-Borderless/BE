package com.likelion.asyncalign.user.domain;

public enum WorkRole {
    DEVELOPER("개발자"),
    PROJECT_MANAGER("프로젝트 매니저(PM)"),
    PRODUCT_MANAGER("프로덕트 매니저"),
    DESIGNER("디자이너"),
    MARKETER("마케팅"),
    DATA_ANALYST("데이터 분석가"),
    QA_ENGINEER("QA 엔지니어"),
    SALES("영업"),
    CUSTOMER_SUCCESS("고객 성공/고객 지원"),
    HR("인사"),
    OPERATIONS("운영"),
    OTHER("기타");

    private final String label;

    WorkRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
