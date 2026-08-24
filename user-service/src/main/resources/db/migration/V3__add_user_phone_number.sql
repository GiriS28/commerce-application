ALTER TABLE users
ADD COLUMN phone_number VARCHAR(15);

ALTER TABLE users
ADD CONSTRAINT users_phone_number_key UNIQUE (phone_number);