CREATE TABLE LINEITEM(
        orderkey       INT,
        partkey        INT,
        suppkey        INT,
        linenumber     INT,
        quantity       DECIMAL,
        extendedprice  DECIMAL,
        discount       DECIMAL,
        tax            DECIMAL,
        returnflag     CHAR(1),
        linestatus     CHAR(1),
        shipdate       DATE,
        commitdate     DATE,
        receiptdate    DATE,
        shipinstruct   CHAR(25),
        shipmode       CHAR(10),
        comment        VARCHAR(44)
    );

SELECT SUM(l.extendedprice*l.discount) AS revenue
FROM   LINEITEM l
WHERE  l.shipdate >= {d'1994-01-01'}
  AND  l.shipdate < {d'1995-01-01'}
  AND  l.discount >= 0.06 - 0.01 
  AND  l.discount <= 0.06 + 0.01 
  AND  l.quantity < 24;