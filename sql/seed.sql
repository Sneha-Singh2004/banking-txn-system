INSERT INTO customer (name, email, phone)
SELECT
    'Customer ' || g,
    'customer' || g || '@bank.example.com',
    '98000' || LPAD(g::text, 5, '0')
FROM generate_series(1, 3000) AS g;

INSERT INTO account (account_id, customer_id, balance, status)
SELECT
    'ACC' || (1000 + g),
    g,
    CASE
        WHEN g % 25 = 0 THEN 0.00
        ELSE 50000.00
    END,
    CASE
        WHEN g % 50 = 0 THEN 'INACTIVE'
        WHEN g % 75 = 0 THEN 'BLOCKED'
        ELSE 'ACTIVE'
    END
FROM generate_series(1, 3000) AS g;