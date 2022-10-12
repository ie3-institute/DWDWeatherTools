CREATE SCHEMA icon;

CREATE TABLE icon.icon_coordinates (
    id integer NOT NULL, latitude double precision NOT NULL, longitude double precision NOT NULL,
    coordinate_type character varying(255), CONSTRAINT pk_icon_coordinates PRIMARY KEY (id));

CREATE TABLE icon.weather (
    time timestamp without time zone NOT NULL, alb_rad double precision, asob_s double precision,
    aswdifd_s double precision, aswdifu_s double precision, aswdir_s double precision, sobs_rad double precision,
    p_20m double precision, p_65m double precision, p_131m double precision,
    t_131m double precision, t_2m double precision, t_g double precision, u_10m double precision,
    u_131m double precision, u_20m double precision, u_216m double precision, u_65m double precision,
    v_10m double precision, v_131m double precision, v_20m double precision, v_216m double precision,
    v_65m double precision, w_131m double precision, w_20m double precision, w_216m double precision,
    w_65m double precision, z0 double precision,
    coordinate_id integer NOT NULL,
    CONSTRAINT pk_weather PRIMARY KEY (coordinate_id, time),
    CONSTRAINT fk_weather_coordinates FOREIGN KEY (coordinate_id) REFERENCES icon.icon_coordinates (id)
        MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION);

CREATE TABLE icon.files(
    name character varying(255) NOT NULL, archivefile_deleted boolean, decompressed boolean,
    download_date timestamp without time zone, download_fails integer, gribfile_deleted boolean,
    missing_coordinates integer, modelrun timestamp without time zone NOT NULL, parameter character varying(
    255), persisted boolean, sufficient_size boolean, timestep integer NOT NULL, valid_file boolean,
    CONSTRAINT pk_files PRIMARY KEY (name));

-- Insert the five test files, plus one non-existent file
INSERT INTO icon.files(name, archivefile_deleted, decompressed, download_date, download_fails, gribfile_deleted,
    missing_coordinates, modelrun, parameter, persisted, sufficient_size, timestep, valid_file) VALUES
    ('icon-eu_europe_regular-lat-lon_model-level_2019082300_001_60_U', false, false, '2019-08-22 19:24', 0, false, 0,
        '2019-08-23 00:00', 'U_20M', false, true, 1, null),
    ('icon-eu_europe_regular-lat-lon_single-level_2019082300_001_U_10M', false, false, '2019-08-22 19:24', 0, false, 0,
        '2019-08-23 00:00', 'U_10M', false, true, 1, null),
    ('icon-eu_europe_regular-lat-lon_single-level_2019082300_003_Z0', false, false, '2019-08-22 19:24', 0, false, 0,
        '2019-08-23 00:00', 'Z0', false, true, 3, null),
    ('icon-eu_europe_regular-lat-lon_single-level_2019082303_000_Z0', false, false, '2019-08-22 19:24', 0, false, 0,
        '2019-08-23 03:00', 'Z0', false, true, 0, null),
    ('icon-eu_europe_regular-lat-lon_single-level_2019082303_001_Z0', false, false, '2019-08-22 19:24', 0, false, 0,
        '2019-08-23 03:00', 'Z0', false, true, 1, null),
    ('icon-eu_europe_regular-lat-lon_single-level_2019082300_001_ASOB_S', false, false, '2019-08-22 19:24', 0, false, 0,
        '2019-08-23 00:00', 'ASOB_S', false, true, 1, null)
        ;
