SELECT name FROM sys.databases WHERE name = N'cinema_customer';
USE cinema_customer;
SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank;
SELECT customer_id,customer_name,email,customer_status FROM customer ORDER BY customer_id;
SELECT COLUMN_NAME,DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'customer';
GO
