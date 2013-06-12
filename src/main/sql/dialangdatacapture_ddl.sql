--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: DIALANGDATACAPTURE; Type: DATABASE; Schema: -; Owner: dialangadmin
--

CREATE DATABASE "DIALANGDATACAPTURE" WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_GB.UTF-8' LC_CTYPE = 'en_GB.UTF-8';


ALTER DATABASE "DIALANGDATACAPTURE" OWNER TO dialangadmin;

\connect "DIALANGDATACAPTURE"

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: item_responses; Type: TABLE; Schema: public; Owner: dialangadmin; Tablespace: 
--

CREATE TABLE item_responses (
    pass_id character(36) NOT NULL,
    basket_id integer NOT NULL,
    item_id integer NOT NULL,
    answer_id integer,
    answer_text character varying(255)
);


ALTER TABLE public.item_responses OWNER TO dialangadmin;

--
-- Name: sa_ppe; Type: TABLE; Schema: public; Owner: dialangadmin; Tablespace: 
--

CREATE TABLE sa_ppe (
    pass_id character(36) NOT NULL,
    ppe real NOT NULL
);


ALTER TABLE public.sa_ppe OWNER TO dialangadmin;

--
-- Name: sa_responses; Type: TABLE; Schema: public; Owner: dialangadmin; Tablespace: 
--

CREATE TABLE sa_responses (
    pass_id character(36) NOT NULL,
    statement_id character varying(4) NOT NULL,
    response boolean NOT NULL
);


ALTER TABLE public.sa_responses OWNER TO dialangadmin;

--
-- Name: sessions; Type: TABLE; Schema: public; Owner: dialangadmin; Tablespace: 
--

CREATE TABLE sessions (
    session_id character(36) NOT NULL,
    pass_id character(36) NOT NULL,
    user_id character varying(255),
    consumer_key character(36),
    al character varying(16) NOT NULL,
    tl character varying(16) NOT NULL,
    skill character varying(32) NOT NULL,
    ip_address character varying(39) NOT NULL,
    started bigint NOT NULL
);


ALTER TABLE public.sessions OWNER TO dialangadmin;

--
-- Name: test_results; Type: TABLE; Schema: public; Owner: dialangadmin; Tablespace: 
--

CREATE TABLE test_results (
    pass_id character(36) NOT NULL,
    grade smallint NOT NULL,
    level character(2) NOT NULL
);


ALTER TABLE public.test_results OWNER TO dialangadmin;

--
-- Name: vsp_test_responses; Type: TABLE; Schema: public; Owner: dialangadmin; Tablespace: 
--

CREATE TABLE vsp_test_responses (
    pass_id character(36) NOT NULL,
    word_id character(6) NOT NULL,
    response boolean NOT NULL
);


ALTER TABLE public.vsp_test_responses OWNER TO dialangadmin;

--
-- Name: vsp_test_scores; Type: TABLE; Schema: public; Owner: dialangadmin; Tablespace: 
--

CREATE TABLE vsp_test_scores (
    pass_id character(36) NOT NULL,
    z_score real NOT NULL,
    meara_score smallint NOT NULL,
    level character(2) NOT NULL
);


ALTER TABLE public.vsp_test_scores OWNER TO dialangadmin;

--
-- Name: sa_ppe_pkey; Type: CONSTRAINT; Schema: public; Owner: dialangadmin; Tablespace: 
--

ALTER TABLE ONLY sa_ppe
    ADD CONSTRAINT sa_ppe_pkey PRIMARY KEY (pass_id);


--
-- Name: sa_responses_pkey; Type: CONSTRAINT; Schema: public; Owner: dialangadmin; Tablespace: 
--

ALTER TABLE ONLY sa_responses
    ADD CONSTRAINT sa_responses_pkey PRIMARY KEY (pass_id, statement_id);


--
-- Name: sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: dialangadmin; Tablespace: 
--

ALTER TABLE ONLY sessions
    ADD CONSTRAINT sessions_pkey PRIMARY KEY (pass_id);

ALTER TABLE ONLY sessions
    ADD CONSTRAINT sessions_ukey UNIQUE (session_id,pass_id);


--
-- Name: vsp_test_responses_pkey; Type: CONSTRAINT; Schema: public; Owner: dialangadmin; Tablespace: 
--

ALTER TABLE ONLY vsp_test_responses
    ADD CONSTRAINT vsp_test_responses_pkey PRIMARY KEY (pass_id, word_id);


--
-- Name: vsp_test_scores_pkey; Type: CONSTRAINT; Schema: public; Owner: dialangadmin; Tablespace: 
--

ALTER TABLE ONLY vsp_test_scores
    ADD CONSTRAINT vsp_test_scores_pkey PRIMARY KEY (pass_id);


--
-- Name: sa_ppe_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: dialangadmin
--

ALTER TABLE ONLY sa_ppe
    ADD CONSTRAINT sa_ppe_session_id_fkey FOREIGN KEY (pass_id) REFERENCES sessions(pass_id);


--
-- Name: vsp_test_scores_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: dialangadmin
--

ALTER TABLE ONLY vsp_test_scores
    ADD CONSTRAINT vsp_test_scores_session_id_fkey FOREIGN KEY (pass_id) REFERENCES sessions(pass_id);


--
-- Name: public; Type: ACL; Schema: -; Owner: fisha
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM fisha;
GRANT ALL ON SCHEMA public TO fisha;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

