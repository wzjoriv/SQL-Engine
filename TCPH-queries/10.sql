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
	
CREATE TABLE CUSTOMER (
        custkey      INT,
        name         VARCHAR(25),
        address      VARCHAR(40),
        nationkey    INT,
        phone        CHAR(15),
        acctbal      DECIMAL,
        mktsegment   CHAR(10),
        comment      VARCHAR(117)
    );

CREATE TABLE NATION (
        nationkey    INT,
        name         CHAR(25),
        regionkey    INT,
        comment      VARCHAR(152)
    );	

SELECT  c.custkey, c.name, 
        c.acctbal,
        n.name,
        c.address,
        c.phone,
        c.comment,
        SUM(l.extendedprice * (1 - l.discount)) AS revenue
FROM    CUSTOMER c, ORDERS o, LINEITEM l, NATION n
WHERE   c.custkey = o.custkey
  AND   l.orderkey = o.orderkey
  AND   o.orderdate >= {d'1993-10-01'}
  AND   o.orderdate < {d'1994-01-01'}
  AND   l.returnflag = 'R'
  AND   c.nationkey = n.nationkey
GROUP BY c.custkey, c.name;