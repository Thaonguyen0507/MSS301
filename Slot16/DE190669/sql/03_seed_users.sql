USE MSS301_2026_PE_MEDIA;
GO
INSERT INTO dbo.users (username, email, role, rate_per_post, max_daily_posts, level, status)
SELECT username, email, role, rate_per_post, max_daily_posts, level, status
FROM (VALUES
    (N'creator_a', N'a@example.com', N'CREATOR', CAST(500000 AS DECIMAL(18,2)), 3, 1, N'ACTIVE'),
    (N'creator_b', N'b@example.com', N'CREATOR', CAST(300000 AS DECIMAL(18,2)), 1, 1, N'ACTIVE'),
    (N'creator_c', N'c@example.com', N'CREATOR', CAST(800000 AS DECIMAL(18,2)), 4, 2, N'SUSPENDED')
) AS seed(username, email, role, rate_per_post, max_daily_posts, level, status)
WHERE NOT EXISTS (SELECT 1 FROM dbo.users u WHERE u.username = seed.username);
GO
