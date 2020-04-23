CREATE TABLE LINEITEM (
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
	
CREATE TABLE ORDERS (
        orderkey       INT,
        custkey        INT,
        orderstatus    CHAR(1),
        totalprice     DECIMAL,
        orderdate      DATE,
        orderpriority  CHAR(15),
        clerk          CHAR(15),
        shippriority   INT,
        comment        VARCHAR(79)
    );

SELECT DISTINCT l.shipmode     
FROM  ORDERS o, LINEITEM l
WHERE  o.orderkey = l.orderkey
  AND  (l.shipmode IN ('MAIL', 'SHIP'))
  AND  l.commitdate < l.receiptdate
  AND  l.shipdate < l.commitdate
  AND  l.receiptdate >= {d'1994-01-01'}
  AND  l.receiptdate < {d'1995-01-01'}
  AND  (o.orderpriority IN ('1-URGENT', '2-HIGH'))
GROUP BY l.shipmode;