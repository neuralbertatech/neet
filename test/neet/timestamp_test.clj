(ns neet.timestamp-test
  (:require [clojure.test :refer :all]
            [neet.core :refer :all]))

(deftest to-pg-timestamp-test
  (testing "Converts a Clojure instant to a Postgres timestamp"
    (let [instant (java.time.Instant/parse "2023-02-14T12:00:00Z")
          expected "2023-02-14 12:00:00"]
      (is (= expected (to-pg-timestamp instant))))))

(deftest to-pg-timestamp-test-with-timezone
  (testing "Converts a Clojure instant with a time zone to a Postgres timestamp"
    (let [zone (java.time.ZoneId/of "America/New_York")
          zoned-instant (java.time.ZonedDateTime/of 2023 2 14 12 0 0 0 zone)
          expected "2023-02-14 17:00:00"]
      (is (= expected (to-pg-timestamp zoned-instant))))))

(deftest to-pg-timestamp-test-with-nanoseconds
  (testing "Converts a Clojure instant with nanoseconds to a Postgres timestamp"
    (let [instant (java.time.Instant/parse "2023-02-14T12:00:00.123456789Z")
          expected "2023-02-14 12:00:00.123456"]
      (is (= expected (to-pg-timestamp instant))))))

(deftest to-pg-timestamp-test-without-nanoseconds
  (testing "Converts a Clojure instant without nanoseconds to a Postgres timestamp"
    (let [instant (java.time.Instant/parse "2023-02-14T12:00:00Z")
          expected "2023-02-14 12:00:00"]
      (is (= expected (to-pg-timestamp instant))))))

(deftest to-pg-timestamp-test-with-null
  (testing "Returns nil when given a nil value"
    (is (nil? (to-pg-timestamp nil)))))

(deftest to-pg-timestamp-test-with-wrong-input
  (testing "Throws an exception when given a non-instant value"
    (is (thrown? Exception (to-pg-timestamp "not an instant")))))

(deftest from-pg-timestamp-test
  (testing "Converts a Postgres timestamp to a Clojure instant"
    (let [timestamp "2023-02-14 12:00:00"
          expected (java.time.Instant/parse "2023-02-14T12:00:00Z")]
      (is (= expected (from-pg-timestamp timestamp))))))

(deftest from-pg-timestamp-test-with-timezone
  (testing "Converts a Postgres timestamp with a time zone to a Clojure instant"
    (let [timestamp "2023-02-14 12:00:00-05"
          expected (java.time.Instant/parse "2023-02-14T17:00:00Z")]
      (is (= expected (from-pg-timestamp timestamp))))))

(deftest from-pg-timestamp-test-with-milliseconds
  (testing "Converts a Postgres timestamp with milliseconds to a Clojure instant"
    (let [timestamp "2023-02-14 12:00:00.123"
          expected (java.time.Instant/parse "2023-02-14T12:00:00.123Z")]
      (is (= expected (from-pg-timestamp timestamp))))))

(deftest from-pg-timestamp-test-without-milliseconds
  (testing "Converts a Postgres timestamp without milliseconds to a Clojure instant"
    (let [timestamp "2023-02-14 12:00:00"
          expected (java.time.Instant/parse "2023-02-14T12:00:00Z")]
      (is (= expected (from-pg-timestamp timestamp))))))

(deftest from-pg-timestamp-test-with-null
  (testing "Returns nil when given a nil value"
    (is (nil? (from-pg-timestamp nil)))))

(deftest from-pg-timestamp-test-with-wrong-input
  (testing "Throws an exception when given a non-timestamp string"
    (is (thrown? Exception (from-pg-timestamp "not a timestamp")))))

