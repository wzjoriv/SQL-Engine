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
	
CREATE TABLE PART (
        partkey      INT,
        name         VARCHAR(55),
        mfgr         CHAR(25),
        brand        CHAR(10),
        type         VARCHAR(25),
        size         INT,
        container    CHAR(10),
        retailprice  DECIMAL,
        comment      VARCHAR(23)
    );

SELECT SUM(l.extendedprice * (1 - l.discount) ) AS revenue
FROM LINEITEM l, PART p
WHERE
    (
        p.partkey = l.partkey
        AND p.brand = 'Brand#12'
        AND ( p.container IN ( 'SM CASE', 'SM BOX', 'SM PACK', 'SM PKG') )
        AND l.quantity >= 1 AND l.quantity <= 1 + 10 
        AND p.size >= 1 AND p.size <= 5
        AND (l.shipmode IN ('AIR', 'AIR REG') )
        AND l.shipinstruct = 'DELIVER IN PERSON' 
    )
    OR 
    (
        p.partkey = l.partkey
        AND p.brand = 'Brand#23'
        AND ( p.container IN ('MED BAG', 'MED BOX', 'MED PKG', 'MED PACK') )
        AND l.quantity >= 10 AND l.quantity <= 10 + 10
		AND p.size >= 1 AND p.size <= 10
        AND ( l.shipmode IN ('AIR', 'AIR REG') )
        AND l.shipinstruct = 'DELIVER IN PERSON'
    )
    OR 
    (
        p.partkey = l.partkey
        AND p.brand = 'Brand#34'
        AND ( p.container IN ( 'LG CASE', 'LG BOX', 'LG PACK', 'LG PKG') )
        AND l.quantity >= 20 AND l.quantity <= 20 + 10
		AND p.size >= 1 AND p.size <= 15
        AND ( l.shipmode IN ('AIR', 'AIR REG') )
        AND l.shipinstruct = 'DELIVER IN PERSON'
    );