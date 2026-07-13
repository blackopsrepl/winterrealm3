-- ---------------------------
-- Native player-bot control metadata
-- ---------------------------
CREATE TABLE IF NOT EXISTS character_bots (
  char_obj_id decimal(11,0) NOT NULL,
  enabled decimal(1,0) NOT NULL DEFAULT 1,
  mode varchar(16) NOT NULL DEFAULT 'WORLD',
  owner_obj_id decimal(11,0) DEFAULT NULL,
  profile_name varchar(64) NOT NULL,
  activity_policy varchar(32) NOT NULL DEFAULT 'default',
  home_x decimal(11,0) DEFAULT NULL,
  home_y decimal(11,0) DEFAULT NULL,
  home_z decimal(11,0) DEFAULT NULL,
  personality_seed bigint NOT NULL,
  metadata_version int NOT NULL DEFAULT 1,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (char_obj_id),
  KEY owner_obj_id (owner_obj_id),
  KEY enabled_mode (enabled,mode)
) ;
