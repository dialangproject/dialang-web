ALTER TABLE passes ADD CONSTRAINT passesfk FOREIGN KEY (session_id) REFERENCES sessions (id);

ALTER TABLE baskets ADD CONSTRAINT basketsfk FOREIGN KEY (pass_id) REFERENCES passes (id);

ALTER TABLE pass_booklet ADD CONSTRAINT pass_bookletfk FOREIGN KEY (pass_id) REFERENCES passes (id);

ALTER TABLE item_responses ADD CONSTRAINT item_responsesfk FOREIGN KEY (pass_id) REFERENCES passes (id);

ALTER TABLE sa_scores ADD CONSTRAINT sa_scoresfk FOREIGN KEY (pass_id) REFERENCES passes (id);

ALTER TABLE sa_responses ADD CONSTRAINT sa_responsesfk FOREIGN KEY (pass_id) REFERENCES passes (id);

ALTER TABLE test_results ADD CONSTRAINT test_resultsfk FOREIGN KEY (pass_id) REFERENCES passes (id);

ALTER TABLE vsp_test_responses ADD CONSTRAINT vsp_test_responsesfk FOREIGN KEY (pass_id) REFERENCES passes (id);

ALTER TABLE vsp_test_scores ADD CONSTRAINT vsp_test_scoresfk FOREIGN KEY (pass_id) REFERENCES passes (id);

ALTER TABLE test_durations ADD CONSTRAINT test_durationsfk FOREIGN KEY (pass_id) REFERENCES passes (id);

ALTER TABLE tokens ADD CONSTRAINT tokensfk FOREIGN KEY (pass_id) REFERENCES passes (id);
