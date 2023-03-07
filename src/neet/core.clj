(ns neet.core
  (:gen-class)
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.types :as sql-types]
            [clojure.string :as str]
            ))

(defn parse-env
  [file]
  (into {} (map #(str/split % #" *= *") (str/split file #"\n"))))

(defn get-db-variables
  [env]
  {:user (get env "USER" "")
    :password (get env "PASSWORD" "")
    :dbtype (get env "DBTYPE" "")
    :dbname (get env "DBNAME" "")
    :host (get env "HOST" "")})

(def db (get-db-variables (parse-env (slurp ".env"))))

(def ds (jdbc/get-datasource db))

(def create-users-table
  "CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL,
  password TEXT NOT NULL
)")

(def create-meetings-table
  "CREATE TABLE meetings (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  created_by INTEGER NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  invited_users INTEGER[] NOT NULL,
  FOREIGN KEY (created_by) REFERENCES users(id)
)")

(def create-availability-table
  "CREATE TABLE availability (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id),
  meeting_id INTEGER NOT NULL REFERENCES meetings(id),
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  is_available BOOLEAN NOT NULL,
  CONSTRAINT availability_timeslot UNIQUE (user_id, start_time, end_time)
)")


(defrecord User [id email name password])
(defrecord Meeting [id name created_by start_time end_time invited_users])
(defrecord Availability [id user_id meeting_id start_time end_time is_available])

(def sample-user-0 {:email "hello@world.ca" :name "Sam Michaels" :password "password123"})
(def sample-user-1 {:email "abc@gmail.com" :name "Amy Whilliams" :password "123321"})
(def sample-meeting-0 {:name "General Meeting" :created-by 1 :start-time "2023-02-14T12:00:00Z" :end-time "2023-02-24T12:00:00Z"})
(def sample-availability-0 {:user-id 1 :meeting-id 0 :start-time "2023-02-15T12:00:00Z" :end-time "2023-02-16T12:00:00Z" :id-available true})
(def sample-availability-0 {:user-id 1 :meeting-id 0 :start-time "2023-02-20T12:00:00Z" :end-time "2023-02-21T12:00:00Z" :id-available true})

(defn create-tables
  []
  (jdbc/execute! ds [create-users-table])
  (jdbc/execute! ds [create-meetings-table])
  (jdbc/execute! ds [create-availability-table]))

(defn create-user
  [^User user]
  (sql/insert! ds "users" user))
;(create-user sample-user-0)
;(create-user sample-user-1)

(defn query-users
  []
  (jdbc/execute! ds ["SELECT * FROM users;"]))
(def users-query (query-users))
(first users-query)

(defn create-meeting
  [^Meeting meeting]
  (sql/insert! ds "meetings" meeting))
;(create-meeting {:name "Meeting number 1" :created_by (:users/id (first (query-users))) :start_time (java.sql.Timestamp/valueOf "2023-02-15 12:00:00") :end_time (java.sql.Timestamp/valueOf "2023-03-15 12:00:00") :invited_users (int-array [0 1])})
;(create-meeting {:name "Meeting number 2" :created_by (:users/id (first (query-users))) :start_time (java.sql.Timestamp/valueOf "2023-02-20 12:00:00") :end_time (java.sql.Timestamp/valueOf "2023-03-10 12:00:00") :invited_users (int-array [1 2])})
;(create-meeting {:name "Meeting number 3" :created_by 4 :start_time (java.sql.Timestamp/valueOf "2023-02-20 12:00:00") :end_time (java.sql.Timestamp/valueOf "2023-03-10 12:00:00") :invited_users (int-array [1 2])})


(defn six-bit-to-char
  [six-bit]
  (condp #(< %2 %1) (int six-bit)
        26 (char (+ (int \a) (int six-bit)))
        52 (char (+ (int \A) (- (int six-bit) 26)))
        62 (- (int six-bit) 52)
        63 \-
        64 \+
        :error))
(six-bit-to-char 2r011010)
(six-bit-to-char 26)
(int 2r0101)

(defn get-meetings-for-user
  [user-id]
  (jdbc/execute! ds [(format "SELECT * FROM meetings WHERE %s = ANY(invited_users) OR %s = created_by;" (str user-id) (str user-id))]))
(get-meetings-for-user 4)

(defn get-meetings
  []
  (jdbc/execute! ds ["SELECT * FROM meetings;"]))
(get-meetings)

(defn get-availability-for-meeting
  [meeting-id]
  (jdbc/execute! ds [(format "SELECT * FROM availability WHERE %s = meeting_id" (str meeting-id))]))
(get-availability-for-meeting 0)

(defn build-schedule-from-times
  [start-time end-time]
  ())
(build-schedule-from-times (java.sql.Timestamp/valueOf "2023-02-20 12:00:00") (java.sql.Timestamp/valueOf "2023-03-10 12:00:00"))
(.getMinutes (java.sql.Timestamp/valueOf "2023-02-20 12:00:00"))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn create-meeting
  "Creates a new meeting with the given parameters.
  The `start-time` and `end-time` parameters should be Java Instant objects.
  The `invited-users` parameter should be a vector of user ids (ints)."
  [start-time end-time title description invited-users]
  (jdbc/with-db-transaction [conn datasource]
    (let [meeting-id (jdbc/insert-multi! conn
                        :meetings
                        {:title title
                         :description description
                         :start_time (to-pg-timestamp start-time)
                         :end_time (to-pg-timestamp end-time)})
          _ (jdbc/insert-multi! conn
               :meetings_users
               (map #(hash-map :meeting_id meeting-id
                               :user_id %)
                    invited-users))]
      meeting-id)))

(defn mark-availability
  "Marks the availability of a user for a given meeting.
  The `user-id` parameter should be the id of the user whose availability is being updated.
  The `meeting-id` parameter should be the id of the meeting for which the availability is being updated.
  The `available` parameter should be a boolean indicating whether the user is available or not.
  The `times` parameter should be a vector of Java Instant objects representing the times for which the user is (or is not) available."
  [user-id meeting-id available times]
  (jdbc/with-db-transaction [conn datasource]
    (jdbc/execute! conn
      ["DELETE FROM availability
        WHERE user_id = ? AND meeting_id = ?" user-id meeting-id])
    (jdbc/insert-multi! conn
      :availability
      (map #(hash-map :user_id user-id
                      :meeting_id meeting-id
                      :available available
                      :time (to-pg-timestamp %))
           times))))

