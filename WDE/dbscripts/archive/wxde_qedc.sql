--
-- PostgreSQL database dump
--

-- Dumped from database version 9.2.4
-- Dumped by pg_dump version 9.2.2
-- Started on 2013-04-12 19:00:28

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 2787 (class 1262 OID 17296)
-- Name: wxde_qedc; Type: DATABASE; Schema: -; Owner: wxde
--

CREATE DATABASE wxde_qedc WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8';


ALTER DATABASE wxde_qedc OWNER TO wxde;

\connect wxde_qedc

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 169 (class 3079 OID 12595)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2790 (class 0 OID 0)
-- Dependencies: 169
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 168 (class 1259 OID 17297)
-- Name: obs; Type: TABLE; Schema: public; Owner: wxde; Tablespace: 
--

CREATE TABLE obs (
    obstype integer NOT NULL,
    sensorid integer NOT NULL,
    "timestamp" timestamp without time zone NOT NULL,
    latitude integer NOT NULL,
    longitude integer NOT NULL,
    elevation integer NOT NULL,
    value double precision NOT NULL,
    confidence real NOT NULL,
    runflags integer NOT NULL,
    passedflags integer NOT NULL,
    created bigint NOT NULL,
    updated bigint NOT NULL
);


ALTER TABLE public.obs OWNER TO wxde;

--
-- TOC entry 2782 (class 0 OID 17297)
-- Dependencies: 168
-- Data for Name: obs; Type: TABLE DATA; Schema: public; Owner: wxde
--

COPY obs (obstype, sensorid, "timestamp", latitude, longitude, elevation, value, confidence, runflags, passedflags, created, updated) FROM stdin;
\.


--
-- TOC entry 2781 (class 2606 OID 17301)
-- Name: obs_pkey; Type: CONSTRAINT; Schema: public; Owner: wxde; Tablespace: 
--

ALTER TABLE ONLY obs
    ADD CONSTRAINT obs_pkey PRIMARY KEY ("timestamp", obstype, sensorid);


--
-- TOC entry 2789 (class 0 OID 0)
-- Dependencies: 5
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2013-04-12 19:00:31

--
-- PostgreSQL database dump complete
--

