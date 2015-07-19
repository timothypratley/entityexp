(ns entityexp.queries
  (:require [clojure.edn :as edn]
            [datomic.api :as d]))

(def conn
  (d/connect "datomic:free://localhost:4334/mbrainz-1968-1973"))

(def db (d/db conn))

#_(d/q [:find ?e
      :where [?e ]] conn)


(defn artist [artist-name]
  (d/q '[:find ?id ?type ?gender
         :in $ ?name
         :where
         [$ ?e :artist/name ?name]
         [$ ?e :artist/gid ?id]
         [$ ?e :artist/type ?type]
         [$ ?e :artist/gender ?gender]]
       db
       artist-name))

(defn titles [artist-name]
  (d/q '[:find ?title
         :in $ ?artist-name
         :where
         [?a :artist/name ?artist-name]
         [?t :track/artists ?a]
         [?t :track/name ?title]]
       db
       artist-name))

(defn pull-artist [artist-name]
  (d/pull
   db
   '[*]
   (ffirst (d/q '[:find ?e
                  :in $ ?artist-name
                  :where [$ ?e :artist/name ?artist-name]]
                db
                artist-name))))

(defn pull [q]
  (d/pull
   db
   '[*]
   (ffirst (d/q q db))))

(defn pull-id [id]
  (d/pull db '[* {:track/artists [:db/id :artist/name]
                  :artist/type [:db/id :db/ident]
                  :artist/country [:db/id :country/name]
                  :artist/gender [:db/id :db/ident]}] id))

(defn q [q]
  (d/q q db))
