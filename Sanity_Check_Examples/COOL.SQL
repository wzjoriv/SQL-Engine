CREATE TABLE R (A int, B int);
CREATE TABLE S (B int, C int);

SELECT K.A FROM (SELECT A, B FROM R) AS K;

