-- cash_card
CREATE TABLE cash_card (
    ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    AMOUNT Number NOT NULL DEFAULT 0,
    OWNER VARCHAR(256) NOT NULL
);