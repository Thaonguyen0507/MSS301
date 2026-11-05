-- Run in SSMS or sqlcmd using an account allowed to create databases.
-- Enable SQL Server Authentication and the sa login in SSMS if needed.
IF DB_ID(N'DBAccount') IS NULL CREATE DATABASE DBAccount;
GO
IF DB_ID(N'DBBrand') IS NULL CREATE DATABASE DBBrand;
GO
IF DB_ID(N'DBWatch') IS NULL CREATE DATABASE DBWatch;
GO
