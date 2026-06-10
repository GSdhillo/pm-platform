INSERT INTO users (id, email, display_name, password_hash, created_at) VALUES
('11111111-1111-1111-1111-111111111111', 'alice@demo.com',  'Alice Admin',  '$2b$10$aBpReb6DudpZ9t6znYdrZ.F.atpbI0Hp0xF9dBRlKaJ83NFPakH1O', now()),
('22222222-2222-2222-2222-222222222222', 'bob@demo.com',    'Bob Lead',     '$2b$10$aBpReb6DudpZ9t6znYdrZ.F.atpbI0Hp0xF9dBRlKaJ83NFPakH1O', now()),
('33333333-3333-3333-3333-333333333333', 'john@demo.com',   'John Member',  '$2b$10$aBpReb6DudpZ9t6znYdrZ.F.atpbI0Hp0xF9dBRlKaJ83NFPakH1O', now()),
('44444444-4444-4444-4444-444444444444', 'vera@demo.com',   'Vera Viewer',  '$2b$10$aBpReb6DudpZ9t6znYdrZ.F.atpbI0Hp0xF9dBRlKaJ83NFPakH1O', now());

INSERT INTO projects (id, project_key, name, description, lead_id, issue_seq, event_seq, created_at) VALUES
('aaaaaaaa-0000-0000-0000-000000000001', 'DEMO', 'Demo Project',
 'Seeded project showcasing workflows, sprints, search and real-time updates',
 '22222222-2222-2222-2222-222222222222', 10, 0, now());

INSERT INTO project_members (project_id, user_id, role, added_at) VALUES
('aaaaaaaa-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'ADMIN', now()),
('aaaaaaaa-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'PROJECT_LEAD', now()),
('aaaaaaaa-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 'MEMBER', now()),
('aaaaaaaa-0000-0000-0000-000000000001', '44444444-4444-4444-4444-444444444444', 'VIEWER', now());

INSERT INTO workflow_statuses (id, project_id, name, category, position, wip_limit) VALUES
('bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'To Do',       'TODO',        0, NULL),
('bbbbbbbb-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'In Progress', 'IN_PROGRESS', 1, 5),
('bbbbbbbb-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', 'In Review',   'IN_PROGRESS', 2, NULL),
('bbbbbbbb-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000001', 'Done',        'DONE',        3, NULL);

INSERT INTO workflow_transitions (id, project_id, from_status_id, to_status_id, name) VALUES
('cccccccc-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000002', 'Start work'),
('cccccccc-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000003', 'Submit for review'),
('cccccccc-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000003', 'bbbbbbbb-0000-0000-0000-000000000004', 'Approve'),
('cccccccc-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000003', 'bbbbbbbb-0000-0000-0000-000000000002', 'Request changes');

INSERT INTO transition_hooks (id, transition_id, kind, hook_type, config) VALUES
('dddddddd-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000001', 'VALIDATOR', 'REQUIRE_ASSIGNEE', '{}'),
('dddddddd-0000-0000-0000-000000000002', 'cccccccc-0000-0000-0000-000000000002', 'ACTION', 'ASSIGN_REVIEWER', '{"userId": "22222222-2222-2222-2222-222222222222"}');

INSERT INTO sprints (id, project_id, name, goal, status, start_date, end_date, created_at) VALUES
('eeeeeeee-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Sprint 1', 'Ship the MVP board', 'ACTIVE', CURRENT_DATE - 7, CURRENT_DATE + 7, now()),
('eeeeeeee-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'Sprint 2', 'Polish + search', 'FUTURE', NULL, NULL, now());

INSERT INTO custom_field_definitions (id, project_id, name, field_type, options) VALUES
('ffffffff-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Platform', 'DROPDOWN', '["Web","Mobile","API"]'),
('ffffffff-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'Due Date', 'DATE', '[]');

INSERT INTO issues (id, project_id, issue_key, type, title, description, status_id, priority, version, assignee_id, reporter_id, sprint_id, parent_id, story_points, labels, created_at, updated_at) VALUES
('99999999-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-1', 'EPIC', 'User authentication & onboarding', 'Everything needed for signup, login and team invites', 'bbbbbbbb-0000-0000-0000-000000000002', 'HIGH', 0, '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', NULL, NULL, NULL, '["auth"]', now() - interval '6 days', now()),
('99999999-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-2', 'STORY', 'Implement JWT login endpoint', 'As a user I can log in with email and password and receive a JWT', 'bbbbbbbb-0000-0000-0000-000000000004', 'HIGH', 2, '33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'eeeeeeee-0000-0000-0000-000000000001', '99999999-0000-0000-0000-000000000001', 5, '["auth","backend"]', now() - interval '5 days', now()),
('99999999-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-3', 'STORY', 'Signup flow with email validation', 'Signup with duplicate-email handling', 'bbbbbbbb-0000-0000-0000-000000000002', 'MEDIUM', 1, '33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'eeeeeeee-0000-0000-0000-000000000001', '99999999-0000-0000-0000-000000000001', 3, '["auth"]', now() - interval '5 days', now()),
('99999999-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-4', 'SUBTASK', 'Hash passwords with bcrypt', 'Use a strong work factor', 'bbbbbbbb-0000-0000-0000-000000000004', 'MEDIUM', 1, '33333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'eeeeeeee-0000-0000-0000-000000000001', '99999999-0000-0000-0000-000000000002', 1, '[]', now() - interval '4 days', now()),
('99999999-0000-0000-0000-000000000005', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-5', 'BUG', 'Login returns 500 on empty password', 'NPE in password comparison when password field is empty', 'bbbbbbbb-0000-0000-0000-000000000003', 'HIGHEST', 3, '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'eeeeeeee-0000-0000-0000-000000000001', NULL, 2, '["bug","auth"]', now() - interval '3 days', now()),
('99999999-0000-0000-0000-000000000006', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-6', 'TASK', 'Set up CI pipeline', 'Build + test on every push', 'bbbbbbbb-0000-0000-0000-000000000001', 'MEDIUM', 0, NULL, '11111111-1111-1111-1111-111111111111', 'eeeeeeee-0000-0000-0000-000000000001', NULL, 3, '["infra"]', now() - interval '3 days', now()),
('99999999-0000-0000-0000-000000000007', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-7', 'STORY', 'Real-time board updates over WebSocket', 'Clients see card moves instantly', 'bbbbbbbb-0000-0000-0000-000000000001', 'HIGH', 0, '33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'eeeeeeee-0000-0000-0000-000000000001', NULL, 8, '["realtime"]', now() - interval '2 days', now()),
('99999999-0000-0000-0000-000000000008', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-8', 'STORY', 'Full-text search across issues and comments', 'Postgres FTS with GIN indexes', 'bbbbbbbb-0000-0000-0000-000000000001', 'MEDIUM', 0, NULL, '22222222-2222-2222-2222-222222222222', NULL, NULL, 5, '["search"]', now() - interval '2 days', now()),
('99999999-0000-0000-0000-000000000009', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-9', 'TASK', 'Write k6 load test for board endpoint', '100 concurrent viewers, p95 under 300ms', 'bbbbbbbb-0000-0000-0000-000000000001', 'LOW', 0, NULL, '11111111-1111-1111-1111-111111111111', NULL, NULL, 2, '["perf"]', now() - interval '1 days', now()),
('99999999-0000-0000-0000-000000000010', 'aaaaaaaa-0000-0000-0000-000000000001', 'DEMO-10', 'BUG', 'Board flickers when two users drag the same card', 'Optimistic locking demo issue', 'bbbbbbbb-0000-0000-0000-000000000001', 'HIGH', 0, '33333333-3333-3333-3333-333333333333', '44444444-4444-4444-4444-444444444444', NULL, NULL, 3, '["bug","board"]', now(), now());

INSERT INTO custom_field_values (issue_id, field_id, value) VALUES
('99999999-0000-0000-0000-000000000002', 'ffffffff-0000-0000-0000-000000000001', '"API"'),
('99999999-0000-0000-0000-000000000005', 'ffffffff-0000-0000-0000-000000000001', '"Web"');

INSERT INTO issue_watchers (issue_id, user_id) VALUES
('99999999-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222'),
('99999999-0000-0000-0000-000000000005', '33333333-3333-3333-3333-333333333333'),
('99999999-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111');

INSERT INTO comments (id, issue_id, parent_comment_id, author_id, body, mentions, created_at, updated_at) VALUES
('88888888-0000-0000-0000-000000000001', '99999999-0000-0000-0000-000000000005', NULL, '33333333-3333-3333-3333-333333333333', 'Reproduced on staging. Stack trace points to the password comparison in AuthService.', '[]', now() - interval '2 days', now() - interval '2 days'),
('88888888-0000-0000-0000-000000000002', '99999999-0000-0000-0000-000000000005', '88888888-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'Good catch @John Member - adding a guard clause and a regression test.', '["33333333-3333-3333-3333-333333333333"]', now() - interval '1 days', now() - interval '1 days');
