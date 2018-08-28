CREATE OR REPLACE
FUNCTION create_tables() 
RETURN NUMBER AS
BEGIN
	CREATE TABLE dictionary
		(term VARCHAR2(100),
		document_frequency NUMBER(20),
		offset NUMBER(20));	
  RETURN 1;
END create_tables;
