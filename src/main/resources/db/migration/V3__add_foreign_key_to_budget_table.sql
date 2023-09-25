ALTER TABLE budget
    ADD COLUMN author_id INT;

ALTER TABLE budget
    ADD CONSTRAINT fk_author
        FOREIGN KEY (author_id)
            REFERENCES author(id)
            ON DELETE SET NULL;