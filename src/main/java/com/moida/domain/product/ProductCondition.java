package com.moida.domain.product;

public enum ProductCondition {
    S("미사용/새상품"),
    A("거의 새것"),
    B("사용감 있음"),
    C("하자 있음");

    private final String description;

    ProductCondition(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
