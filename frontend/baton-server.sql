CREATE TABLE `users` (
	`id`	BIGINT	NOT NULL,
	`email`	VARCHAR(255)	NOT NULL,
	`name`	VARCHAR(100)	NOT NULL,
	`timezone`	VARCHAR(50)	NULL,
	`language`	VARCHAR(20)	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `platform_connections` (
	`id`	BIGINT	NOT NULL,
	`user_id`	BIGINT	NOT NULL,
	`platform_type`	VARCHAR(30)	NOT NULL,
	`workspace_id`	VARCHAR(255)	NOT NULL,
	`workspace_name`	VARCHAR(255)	NULL,
	`access_token_encrypted`	TEXT	NOT NULL,
	`refresh_token_encrypted`	TEXT	NULL,
	`token_expires_at`	DATETIME	NULL,
	`connection_status`	VARCHAR(30)	NOT NULL,
	`last_synced_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `conversations` (
	`id`	BIGINT	NOT NULL,
	`platform_connection_id`	BIGINT	NOT NULL,
	`external_conversation_id`	VARCHAR(255)	NOT NULL,
	`external_thread_id`	VARCHAR(255)	NULL,
	`conversation_type`	VARCHAR(30)	NOT NULL,
	`title`	VARCHAR(255)	NULL,
	`counterpart_external_id`	VARCHAR(255)	NULL,
	`counterpart_name`	VARCHAR(100)	NULL,
	`counterpart_timezone`	VARCHAR(50)	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `messages` (
	`id`	BIGINT	NOT NULL,
	`conversation_id`	BIGINT	NOT NULL,
	`external_message_id`	VARCHAR(255)	NOT NULL,
	`external_event_id`	VARCHAR(255)	NULL,
	`sender_external_id`	VARCHAR(255)	NOT NULL,
	`sender_type`	VARCHAR(30)	NOT NULL,
	`content`	TEXT	NOT NULL,
	`original_language`	VARCHAR(20)	NULL,
	`is_baton_generated`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`sent_at`	DATETIME	NOT NULL,
	`created_at`	DATETIME	NOT NULL
);

CREATE TABLE `batons` (
	`id`	BIGINT	NOT NULL,
	`user_id`	BIGINT	NOT NULL,
	`conversation_id`	BIGINT	NOT NULL,
	`trigger_message_id`	BIGINT	NOT NULL,
	`reply_message_id`	BIGINT	NULL,
	`status`	VARCHAR(30)	NOT NULL,
	`auto_send_enabled`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`expires_at`	DATETIME	NULL,
	`activated_at`	DATETIME	NULL,
	`completed_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `branches` (
	`id`	BIGINT	NOT NULL,
	`baton_id`	BIGINT	NOT NULL,
	`name`	VARCHAR(100)	NOT NULL,
	`description`	VARCHAR(500)	NULL,
	`condition_text`	TEXT	NOT NULL,
	`condition_rule_json`	JSON	NULL,
	`decision_text`	TEXT	NOT NULL,
	`response_text`	TEXT	NULL,
	`action_type`	VARCHAR(30)	NOT NULL,
	`action_config_json`	JSON	NULL,
	`execution_mode`	VARCHAR(30)	NOT NULL,
	`sort_order`	INT	NOT NULL	DEFAULT 0,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `classifications` (
	`id`	BIGINT	NOT NULL,
	`baton_id`	BIGINT	NOT NULL,
	`reply_message_id`	BIGINT	NOT NULL,
	`selected_branch_id`	BIGINT	NULL,
	`confidence`	DECIMAL(5,4)	NULL,
	`is_ambiguous`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`contains_new_question`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`extracted_data_json`	JSON	NULL,
	`reasoning_summary`	TEXT	NULL,
	`result_status`	VARCHAR(30)	NOT NULL,
	`model_name`	VARCHAR(100)	NULL,
	`created_at`	DATETIME	NOT NULL
);

CREATE TABLE `executions` (
	`id`	BIGINT	NOT NULL,
	`baton_id`	BIGINT	NOT NULL,
	`branch_id`	BIGINT	NULL,
	`classification_id`	BIGINT	NULL,
	`action_type`	VARCHAR(30)	NOT NULL,
	`execution_status`	VARCHAR(30)	NOT NULL,
	`result_message_id`	BIGINT	NULL,
	`executed_at`	DATETIME	NULL,
	`failure_reason`	TEXT	NULL,
	`created_at`	DATETIME	NOT NULL
);

ALTER TABLE `users` ADD CONSTRAINT `PK_USERS` PRIMARY KEY (
	`id`
);

ALTER TABLE `platform_connections` ADD CONSTRAINT `PK_PLATFORM_CONNECTIONS` PRIMARY KEY (
	`id`
);

ALTER TABLE `conversations` ADD CONSTRAINT `PK_CONVERSATIONS` PRIMARY KEY (
	`id`
);

ALTER TABLE `messages` ADD CONSTRAINT `PK_MESSAGES` PRIMARY KEY (
	`id`
);

ALTER TABLE `batons` ADD CONSTRAINT `PK_BATONS` PRIMARY KEY (
	`id`
);

ALTER TABLE `branches` ADD CONSTRAINT `PK_BRANCHES` PRIMARY KEY (
	`id`
);

ALTER TABLE `classifications` ADD CONSTRAINT `PK_CLASSIFICATIONS` PRIMARY KEY (
	`id`
);

ALTER TABLE `executions` ADD CONSTRAINT `PK_EXECUTIONS` PRIMARY KEY (
	`id`
);

