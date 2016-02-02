CREATE TABLE sessions (
    id character(36) NOT NULL,
    user_id character varying(255),
    first_name character varying(36),
    last_name character varying(36),
    consumer_key character(36),
    ip_address character varying(39) NOT NULL,
    started bigint NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE passes (
    id character(36) NOT NULL,
    session_id character(36) references sessions(id),
    al character varying(16) NOT NULL,
    tl character varying(16) NOT NULL,
    skill character varying(32) NOT NULL,
    started bigint NOT NULL,
    UNIQUE(id,session_id),
    PRIMARY KEY(id)
);

CREATE TABLE baskets (
    pass_id character(36) references passes(id),
    basket_id integer NOT NULL,
    basket_number integer NOT NULL,
    PRIMARY KEY(pass_id, basket_id)
);

CREATE TABLE pass_booklet (
    pass_id character(36) references passes(id),
    booklet_id integer NOT NULL,
    length integer NOT NULL,
    PRIMARY KEY(pass_id)
);

CREATE TABLE item_responses (
    pass_id character(36) references passes(id),
    basket_id integer NOT NULL,
    item_id integer NOT NULL,
    answer_id integer,
    answer_text character varying(255),
    score integer NOT NULL,
    correct boolean NOT NULL,
    pass_order integer NOT NULL,
    PRIMARY KEY(pass_id, item_id)
);

CREATE TABLE sa_scores (
    pass_id character(36) references passes(id),
    ppe real NOT NULL,
    level character(2) NOT NULL,
    PRIMARY KEY(pass_id)
);

CREATE TABLE sa_responses (
    pass_id character(36) references passes(id),
    statement_id character varying(4) NOT NULL,
    response boolean NOT NULL,
    PRIMARY KEY(pass_id, statement_id)
);

CREATE TABLE test_results (
    pass_id character(36) references passes(id),
    grade smallint NOT NULL,
    level character(2) NOT NULL,
    PRIMARY KEY(pass_id)
);

CREATE TABLE vsp_test_responses (
    pass_id character(36) references passes(id),
    word_id character(6) NOT NULL,
    response boolean NOT NULL,
    PRIMARY KEY(pass_id, word_id)
);

CREATE TABLE vsp_test_scores (
    pass_id character(36) references passes(id),
    z_score real NOT NULL,
    meara_score smallint NOT NULL,
    level character(2) NOT NULL,
    PRIMARY KEY(pass_id)
);

CREATE TABLE test_durations (
    pass_id char(36) references passes(id),
    start bigint not null,
    finish bigint,
    PRIMARY KEY(pass_id)
);

CREATE TABLE tokens (
    token character(36) NOT NULL,
    pass_id character(36) references passes(id),
    current_basket integer,
    vspt_skipped boolean NOT NULL,
    sa_skipped boolean NOT NULL,
    PRIMARY KEY(pass_id)
);
