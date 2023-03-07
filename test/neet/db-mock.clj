(ns scheduling-program.test-db
  (:require [next.jdbc :as jdbc]
            [mount.core :refer [defstate]]))

(defstate db-spec
  :start (jdbc/get-datasource
            {:dbtype "postgresql"
             :host "localhost"
             :port 5432
             :dbname "scheduling_program_test"
             :user "testuser"
             :password "testpass"
             :ssl false})
  :stop (jdbc/close! @db-spec))

(defstate db
  :start (jdbc/with-connection
            {:datasource @db-spec}
            (jdbc/execute! "CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR(255), email VARCHAR(255))",
                           "CREATE TABLE meetings (id SERIAL PRIMARY KEY, title VARCHAR(255), start_time TIMESTAMP, end_time TIMESTAMP, invited_users INTEGER[], created_by INTEGER REFERENCES users(id))"))
  :stop (jdbc/with-connection
          {:datasource @db-spec}
          (jdbc/execute! "DROP TABLE IF EXISTS users, meetings")))

