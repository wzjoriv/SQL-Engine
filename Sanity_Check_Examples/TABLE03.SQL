CREATE TABLE R ( A int, B int );
CREATE TABLE S ( B int, C int );

SELECT AVG(DISTINCT B) FROM (SELECT A, B AS K FROM R), S WHERE (K = S.B);