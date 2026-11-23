DROP TABLE IF EXISTS failed_transaction CASCADE;
DROP TABLE IF EXISTS transaction_audit CASCADE;
DROP TABLE IF EXISTS "transaction" CASCADE;
DROP TABLE IF EXISTS account CASCADE;
DROP TABLE IF EXISTS customer CASCADE;

CREATE TABLE customer (
    customer_id   BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) UNIQUE,
    phone         VARCHAR(20),
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE account (
    account_id    VARCHAR(20) PRIMARY KEY,
    customer_id   BIGINT NOT NULL REFERENCES customer(customer_id),
    balance       NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status        VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE','BLOCKED','CLOSED')),
    version       BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE "transaction" (
    transaction_id  VARCHAR(40) PRIMARY KEY,
    type            VARCHAR(12) NOT NULL CHECK (type IN ('DEPOSIT','WITHDRAWAL','TRANSFER')),
    amount          NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    source_account  VARCHAR(20) REFERENCES account(account_id),
    dest_account    VARCHAR(20) REFERENCES account(account_id),
    channel         VARCHAR(12),
    status          VARCHAR(10) NOT NULL CHECK (status IN ('PENDING','SUCCESS','FAILED','REJECTED')),
    reason          VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (
        (type='DEPOSIT'    AND dest_account   IS NOT NULL) OR
        (type='WITHDRAWAL' AND source_account IS NOT NULL) OR
        (type='TRANSFER'   AND source_account IS NOT NULL AND dest_account IS NOT NULL
                           AND source_account <> dest_account)
    )
);

CREATE TABLE transaction_audit (
    audit_id        BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(40) NOT NULL REFERENCES "transaction"(transaction_id),
    event_time      TIMESTAMP NOT NULL DEFAULT now(),
    thread_name     VARCHAR(60),
    operation       VARCHAR(12),
    result          VARCHAR(10),
    duration_ms     INT,
    details         TEXT
);

CREATE TABLE failed_transaction (
    failure_id      BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(40) NOT NULL REFERENCES "transaction"(transaction_id),
    error_category  VARCHAR(40) NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    failed_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_customer_id ON account(customer_id);
CREATE INDEX idx_txn_source_account ON "transaction"(source_account);
CREATE INDEX idx_txn_dest_account ON "transaction"(dest_account);
CREATE INDEX idx_txn_created_type ON "transaction"(created_at, type);
CREATE INDEX idx_txn_status_reason ON "transaction"(status, reason);
CREATE INDEX idx_audit_txn_id ON transaction_audit(transaction_id);
CREATE INDEX idx_failed_category ON failed_transaction(error_category);
