CREATE TABLE R ( A int, B int );
CREATE TABLE S ( B int, C int );

SELECT o.A, k.C FROM R AS o, S AS k;
