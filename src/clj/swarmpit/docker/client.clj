(ns swarmpit.docker.client
  (:refer-clojure :exclude [get])
  (:require [clojure.java.shell :as shell]
            [cheshire.core :refer [parse-string]]))

(def ^:private api-version "v1.24")
(def ^:private base-cmd ["curl" "-s" "--unix-socket" "/var/run/docker.sock"])

(defn- parse-headers
  "Parse headers map to curl vector cmd representation"
  [headers]
  (->> headers
       (map #(str (name (key %)) ": " (val %)))
       (map #(into ["-H" %]))
       (flatten)
       (into [])))

(defn- parse-request
  "Parse request to curl vector cmd representation"
  [method api]
  ["-X" method (str "http:/" api-version api)])

(defn- parse-payload
  "Parse request payload to curl vector cmd representation"
  [json]
  (if (nil? json)
    []
    ["-d" json]))

(defn- command
  "Build docker command"
  [method api headers payload]
  (let [pheaders (parse-headers headers)
        pcommand (parse-request method api)
        ppayload (parse-payload payload)]
    (-> base-cmd
        (into pheaders)
        (into ppayload)
        (into pcommand))))

(defn- execute
  "Execute docker command and parse result"
  [method api headers payload]
  (let [cmd (command method api headers payload)
        result (apply shell/sh cmd)]
    (if (= 0 (:exit result))
      (parse-string (:out result) true)
      (parse-string (:err result) true))))

(defn get
  ([api] (execute "GET" api nil nil))
  ([api headers] (execute "GET" api headers nil)))

(defn post
  ([api payload] (execute "POST" api nil payload))
  ([api headers payload] (execute "POST" api headers payload)))

(defn put
  ([api payload] (execute "PUT" api nil payload))
  ([api headers payload] (execute "PUT" api headers payload)))

(defn delete
  ([api] (execute "DELETE" api nil nil))
  ([api headers] (execute "DELETE" api headers nil)))