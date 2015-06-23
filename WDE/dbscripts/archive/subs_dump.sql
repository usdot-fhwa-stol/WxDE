--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: subs; Type: SCHEMA; Schema: -; Owner: wxde
--

CREATE SCHEMA subs;


ALTER SCHEMA subs OWNER TO wxde;

SET search_path = subs, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: subcontrib; Type: TABLE; Schema: subs; Owner: wxde; Tablespace: 
--

CREATE TABLE subcontrib (
    subid integer DEFAULT 0 NOT NULL,
    contribid integer DEFAULT 0 NOT NULL
);


ALTER TABLE subs.subcontrib OWNER TO wxde;

--
-- Name: subradius; Type: TABLE; Schema: subs; Owner: wxde; Tablespace: 
--

CREATE TABLE subradius (
    subid integer DEFAULT 0 NOT NULL,
    lat integer DEFAULT 0 NOT NULL,
    lng integer DEFAULT 0 NOT NULL,
    radius integer DEFAULT 0 NOT NULL
);


ALTER TABLE subs.subradius OWNER TO wxde;

--
-- Name: subscription; Type: TABLE; Schema: subs; Owner: wxde; Tablespace: 
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
    cycle smallint DEFAULT (20)::smallint NOT NULL,
    contactname character varying(64) DEFAULT NULL::character varying,
    contactemail character varying(128) DEFAULT NULL::character varying
);


ALTER TABLE subs.subscription OWNER TO wxde;

--
-- Name: substation; Type: TABLE; Schema: subs; Owner: wxde; Tablespace: 
--

CREATE TABLE substation (
    subid integer DEFAULT 0 NOT NULL,
    stationid integer DEFAULT 0 NOT NULL
);


ALTER TABLE subs.substation OWNER TO wxde;

--
-- Data for Name: subcontrib; Type: TABLE DATA; Schema: subs; Owner: wxde
--

COPY subcontrib (subid, contribid) FROM stdin;
\.


--
-- Data for Name: subradius; Type: TABLE DATA; Schema: subs; Owner: wxde
--

COPY subradius (subid, lat, lng, radius) FROM stdin;
\.


--
-- Data for Name: subscription; Type: TABLE DATA; Schema: subs; Owner: wxde
--

COPY subscription (id, expires, lat1, lng1, lat2, lng2, obstypeid, minvalue, maxvalue, qchrun, qchflags, password, format, cycle, contactname, contactemail) FROM stdin;
\.


--
-- Data for Name: substation; Type: TABLE DATA; Schema: subs; Owner: wxde
--

COPY substation (subid, stationid) FROM stdin;
\.


--
-- Name: subcontrib_pkey; Type: CONSTRAINT; Schema: subs; Owner: wxde; Tablespace: 
--

ALTER TABLE ONLY subcontrib
    ADD CONSTRAINT subcontrib_pkey PRIMARY KEY (subid, contribid);


--
-- Name: subradius_pkey; Type: CONSTRAINT; Schema: subs; Owner: wxde; Tablespace: 
--

ALTER TABLE ONLY subradius
    ADD CONSTRAINT subradius_pkey PRIMARY KEY (subid);


--
-- Name: subscription_pkey; Type: CONSTRAINT; Schema: subs; Owner: wxde; Tablespace: 
--

ALTER TABLE ONLY subscription
    ADD CONSTRAINT subscription_pkey PRIMARY KEY (id);


--
-- Name: substation_pkey; Type: CONSTRAINT; Schema: subs; Owner: wxde; Tablespace: 
--

ALTER TABLE ONLY substation
    ADD CONSTRAINT substation_pkey PRIMARY KEY (subid, stationid);


--
-- Name: subs; Type: ACL; Schema: -; Owner: wxde
--

REVOKE ALL ON SCHEMA subs FROM PUBLIC;
REVOKE ALL ON SCHEMA subs FROM wxde;
GRANT ALL ON SCHEMA subs TO wxde;


--
-- Name: subcontrib; Type: ACL; Schema: subs; Owner: wxde
--

REVOKE ALL ON TABLE subcontrib FROM PUBLIC;
REVOKE ALL ON TABLE subcontrib FROM wxde;
GRANT ALL ON TABLE subcontrib TO wxde;
GRANT SELECT ON TABLE subcontrib TO wxdero;


--
-- Name: subradius; Type: ACL; Schema: subs; Owner: wxde
--

REVOKE ALL ON TABLE subradius FROM PUBLIC;
REVOKE ALL ON TABLE subradius FROM wxde;
GRANT ALL ON TABLE subradius TO wxde;
GRANT SELECT ON TABLE subradius TO wxdero;


--
-- Name: subscription; Type: ACL; Schema: subs; Owner: wxde
--

REVOKE ALL ON TABLE subscription FROM PUBLIC;
REVOKE ALL ON TABLE subscription FROM wxde;
GRANT ALL ON TABLE subscription TO wxde;
GRANT SELECT ON TABLE subscription TO wxdero;


--
-- Name: substation; Type: ACL; Schema: subs; Owner: wxde
--

REVOKE ALL ON TABLE substation FROM PUBLIC;
REVOKE ALL ON TABLE substation FROM wxde;
GRANT ALL ON TABLE substation TO wxde;
GRANT SELECT ON TABLE substation TO wxdero;


--
-- PostgreSQL database dump complete
--

