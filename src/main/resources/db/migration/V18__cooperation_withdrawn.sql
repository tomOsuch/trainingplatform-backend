ALTER TABLE cooperation
DROP
CONSTRAINT cooperation_status_check;

ALTER TABLE cooperation
    ADD CONSTRAINT cooperation_status_check
        CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'ENDED', 'EXPIRED', 'WITHDRAWN'));