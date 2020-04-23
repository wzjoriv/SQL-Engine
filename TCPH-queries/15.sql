CREATE TABLE SUPPLIER (
        suppkey      INT,
        name         CHAR(25),
        address      VARCHAR(40),
        nationkey    INT,
        phone        CHAR(15),
        acctbal      DECIMAL,
        comment      VARCHAR(101)
    );

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
	
SELECT s.suppkey, s.name, s.address, s.phone, r1.total_revenue as total_revenue
FROM SUPPLIER s, 
     (SELECT l.suppkey AS SUPPLIER_no, 
             SUM(l.extendedprice * (1 - l.discount)) AS total_revenue, shipdate
      FROM LINEITEM l
      WHERE l.shipdate >= {d'1996-01-01'}
        AND l.shipdate <  {d'1996-04-01'}
      GROUP BY l.suppkey) AS r1
WHERE 
    s.suppkey = r1.SUPPLIER_no;