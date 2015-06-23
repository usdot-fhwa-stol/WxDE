--
-- PostgreSQL database dump
--

-- Dumped from database version 9.2.4
-- Dumped by pg_dump version 9.2.2
-- Started on 2013-04-12 18:59:12

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 2823 (class 1262 OID 17302)
-- Name: wxde_subs; Type: DATABASE; Schema: -; Owner: wxde
--

CREATE DATABASE wxde_subs WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8';


ALTER DATABASE wxde_subs OWNER TO wxde;

\connect wxde_subs

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 172 (class 3079 OID 12595)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2826 (class 0 OID 0)
-- Dependencies: 172
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 168 (class 1259 OID 17331)
-- Name: subcontrib; Type: TABLE; Schema: public; Owner: wxde; Tablespace: 
--

CREATE TABLE subcontrib (
    subid integer DEFAULT 0 NOT NULL,
    contribid integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.subcontrib OWNER TO wxde;

--
-- TOC entry 169 (class 1259 OID 17338)
-- Name: subradius; Type: TABLE; Schema: public; Owner: wxde; Tablespace: 
--

CREATE TABLE subradius (
    subid integer DEFAULT 0 NOT NULL,
    lat integer DEFAULT 0 NOT NULL,
    lng integer DEFAULT 0 NOT NULL,
    radius integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.subradius OWNER TO wxde;

--
-- TOC entry 170 (class 1259 OID 17347)
-- Name: subscription; Type: TABLE; Schema: public; Owner: wxde; Tablespace: 
--

CREATE TABLE subscription (
    id integer DEFAULT 0 NOT NULL,
    expires timestamp without time zone DEFAULT '2010-01-01 00:00:00'::timestamp without time zone NOT NULL,
    lat1 integer,
    lng1 integer,
    lat2 integer,
    lng2 integer,
    obstypeid integer,
    minvalue double precision,
    maxvalue double precision,
    qchrun integer,
    qchflags integer,
    password character varying(15) DEFAULT ''::character varying NOT NULL,
    format character varying(8) DEFAULT 'CSV'::character varying NOT NULL,
    cycle smallint DEFAULT 20::smallint NOT NULL,
    contactname character varying(64) DEFAULT NULL::character varying,
    contactemail character varying(128) DEFAULT NULL::character varying
);


ALTER TABLE public.subscription OWNER TO wxde;

--
-- TOC entry 171 (class 1259 OID 17359)
-- Name: substation; Type: TABLE; Schema: public; Owner: wxde; Tablespace: 
--

CREATE TABLE substation (
    subid integer DEFAULT 0 NOT NULL,
    stationid integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.substation OWNER TO wxde;

--
-- TOC entry 2815 (class 0 OID 17331)
-- Dependencies: 168
-- Data for Name: subcontrib; Type: TABLE DATA; Schema: public; Owner: wxde
--

COPY subcontrib (subid, contribid) FROM stdin;
\.


--
-- TOC entry 2816 (class 0 OID 17338)
-- Dependencies: 169
-- Data for Name: subradius; Type: TABLE DATA; Schema: public; Owner: wxde
--

COPY subradius (subid, lat, lng, radius) FROM stdin;
\.


--
-- TOC entry 2817 (class 0 OID 17347)
-- Dependencies: 170
-- Data for Name: subscription; Type: TABLE DATA; Schema: public; Owner: wxde
--

COPY subscription (id, expires, lat1, lng1, lat2, lng2, obstypeid, minvalue, maxvalue, qchrun, qchflags, password, format, cycle, contactname, contactemail) FROM stdin;
\.


--
-- TOC entry 2818 (class 0 OID 17359)
-- Dependencies: 171
-- Data for Name: substation; Type: TABLE DATA; Schema: public; Owner: wxde
--

COPY substation (subid, stationid) FROM stdin;
\.


--
-- TOC entry 2808 (class 2606 OID 17337)
-- Name: subcontrib_pkey; Type: CONSTRAINT; Schema: public; Owner: wxde; Tablespace: 
--

ALTER TABLE ONLY subcontrib
    ADD CONSTRAINT subcontrib_pkey PRIMARY KEY (subid, contribid);


--
-- TOC entry 2810 (class 2606 OID 17346)
-- Name: subradius_pkey; Type: CONSTRAINT; Schema: public; Owner: wxde; Tablespace: 
--

ALTER TABLE ONLY subradius
    ADD CONSTRAINT subradius_pkey PRIMARY KEY (subid);


--
-- TOC entry 2812 (class 2606 OID 17358)
-- Name: subscription_pkey; Type: CONSTRAINT; Schema: public; Owner: wxde; Tablespace: 
--

ALTER TABLE ONLY subscription
    ADD CONSTRAINT subscription_pkey PRIMARY KEY (id);


--
-- TOC entry 2814 (class 2606 OID 17365)
-- Name: substation_pkey; Type: CONSTRAINT; Schema: public; Owner: wxde; Tablespace: 
--

ALTER TABLE ONLY substation
    ADD CONSTRAINT substation_pkey PRIMARY KEY (subid, stationid);


--
-- TOC entry 2825 (class 0 OID 0)
-- Dependencies: 5
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2013-04-12 18:59:16

--
-- PostgreSQL database dump complete
--

