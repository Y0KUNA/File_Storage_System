CREATE USER identity WITH PASSWORD 'identity';
CREATE DATABASE identity_db OWNER identity;

CREATE USER filesystem WITH PASSWORD 'filesystem';
CREATE DATABASE filesystem_db OWNER filesystem;

CREATE USER upload WITH PASSWORD 'upload';
CREATE DATABASE upload_db OWNER upload;

CREATE USER download WITH PASSWORD 'download';
CREATE DATABASE download_db OWNER download;

CREATE USER notification WITH PASSWORD 'notification';
CREATE DATABASE notification_db OWNER notification;

CREATE USER audit WITH PASSWORD 'audit';
CREATE DATABASE audit_analytics_db OWNER audit;

