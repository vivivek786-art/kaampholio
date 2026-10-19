CREATE TABLE professionals (
 id VARCHAR(80) PRIMARY KEY, name VARCHAR(120) NOT NULL, trade VARCHAR(30) NOT NULL,
 city VARCHAR(100) NOT NULL, area VARCHAR(150) NOT NULL, experience INT NOT NULL,
 rating DOUBLE NOT NULL, reviews INT NOT NULL, rate INT NOT NULL, available BOOLEAN NOT NULL,
 skills TEXT NOT NULL, tools TEXT NOT NULL, bio TEXT NOT NULL
);
CREATE INDEX idx_professionals_city_trade ON professionals(city,trade);
CREATE TABLE portfolio_work (
 id VARCHAR(80) PRIMARY KEY, professional_id VARCHAR(80) NOT NULL,
 title VARCHAR(180) NOT NULL, description TEXT NOT NULL, kind VARCHAR(10) NOT NULL,
 src VARCHAR(300) NOT NULL, tools TEXT NOT NULL, sort_order INT NOT NULL,
 FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
);
CREATE TABLE saved_professionals (
 owner_id VARCHAR(80) NOT NULL, professional_id VARCHAR(80) NOT NULL,
 PRIMARY KEY (owner_id,professional_id),
 FOREIGN KEY (owner_id) REFERENCES professionals(id) ON DELETE CASCADE,
 FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
);
CREATE TABLE state_revision (owner_id VARCHAR(80) PRIMARY KEY, revision BIGINT NOT NULL);
INSERT INTO state_revision VALUES ('me',0);
