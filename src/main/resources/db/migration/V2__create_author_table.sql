CREATE TABLE author (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);