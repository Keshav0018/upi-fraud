CREATE TABLE payments (
    id                UUID PRIMARY KEY,
    payer_vpa         VARCHAR(120) NOT NULL,
    payee_vpa         VARCHAR(120) NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL,
    note              VARCHAR(280),
    device_id         VARCHAR(120) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    check_latency_ms  BIGINT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_payer_vpa ON payments (payer_vpa);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at);

CREATE TABLE fraud_alerts (
    id                 UUID PRIMARY KEY,
    payment_id         UUID NOT NULL REFERENCES payments (id),
    payer_vpa          VARCHAR(120) NOT NULL,
    payee_vpa          VARCHAR(120) NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    reasons            JSONB NOT NULL DEFAULT '[]',
    profile_snapshot   JSONB,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ai_status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ai_risk_level      VARCHAR(20),
    ai_summary         TEXT,
    ai_recommendation  VARCHAR(20),
    ai_signals         JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at        TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_fraud_alerts_payment_id ON fraud_alerts (payment_id);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts (status);

CREATE TABLE processed_events (
    event_id      UUID PRIMARY KEY,
    topic         VARCHAR(100) NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
