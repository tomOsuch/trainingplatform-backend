-- Termin ważności zaproszenia do współpracy.
-- Kolumna jest nullowalna, bo dotyczy wyłącznie stanu PENDING - współpraca aktywna,
-- odrzucona czy zakończona żadnego terminu nie ma.
ALTER TABLE cooperation
    ADD COLUMN expires_at timestamp(6);

-- Piąty stan. Zaproszenie, które straciło ważność, NIE jest ani odrzucone (adresat
-- niczego nie odmówił), ani zakończone (współpraca nigdy nie ruszyła). Bez osobnego
-- stanu przeterminowany wiersz zostawałby w PENDING i blokował parę kont na zawsze
-- przez uq_cooperation_open_pair.
ALTER TABLE cooperation
DROP CONSTRAINT cooperation_status_check;

ALTER TABLE cooperation
    ADD CONSTRAINT cooperation_status_check
        CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'ENDED', 'EXPIRED'));

-- Zaproszenie oczekujące bez terminu byłoby zaproszeniem wiecznym. Baza tego pilnuje,
-- więc pominięcie terminu w serwisie nie przejdzie po cichu.
ALTER TABLE cooperation
    ADD CONSTRAINT cooperation_pending_expiry_check
        CHECK (status <> 'PENDING' OR expires_at IS NOT NULL);